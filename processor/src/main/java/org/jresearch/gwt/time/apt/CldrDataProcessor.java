package org.jresearch.gwt.time.apt;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.jresearch.gwt.time.apt.annotation.CldrLocale;
import org.jresearch.gwt.time.apt.annotation.CldrTime;
import org.jresearch.gwt.time.apt.cldr.ldml.Ldml;
import org.jresearch.gwt.time.apt.cldr.ldmlSupplemental.CodeMappings;
import org.jresearch.gwt.time.apt.cldr.ldmlSupplemental.FirstDay;
import org.jresearch.gwt.time.apt.cldr.ldmlSupplemental.MinDays;
import org.jresearch.gwt.time.apt.cldr.ldmlSupplemental.SupplementalData;
import org.jresearch.gwt.time.apt.cldr.ldmlSupplemental.TerritoryCodes;
import org.jresearch.gwt.time.apt.cldr.ldmlSupplemental.WeekData;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.JavaFile.Builder;
import com.squareup.javapoet.TypeSpec;

import one.util.streamex.StreamEx;

@AutoService(Processor.class)
public class CldrDataProcessor extends AbstractProcessor {

	private static final String OTHER_TERRITORIES = "001";

	private static final Integer FALBACK_MIN_DAYS = Integer.valueOf(1);

	private static final String CLDR_XML = "/cldr/common/supplemental/supplementalData.xml";
	private static final String LDML_XML_LIST = "/main-list.txt";
	private static final String WEEK_INFO_CLASS_NAME = "WeekInfo";
	private static final String LOCALE_INFO_CLASS_NAME = "LocaleInfo";
	private static final String PATTERN_INFO_CLASS_NAME = "PatternInfo";

	static final String REGION_ENUM_NAME = "Region";

	private Unmarshaller ldmUnmarshaller;
	private Unmarshaller supUnmarshaller;
	private List<Ldml> mainData = List.of();

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Set.of(CldrLocale.class.getName(), CldrTime.class.getName());
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@SuppressWarnings("resource")
	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
		try {
			System.setProperty("javax.xml.accessExternalDTD", "all");
			initUnmarshaller();
			// get first available annotated package and ignore others
			StreamEx.of(roundEnv.getElementsAnnotatedWith(CldrTime.class))
				.filterBy(Element::getKind, ElementKind.PACKAGE)
				.findAny()
				.map(PackageElement.class::cast)
				.ifPresent(this::generateCldrTimeData);
			// get first available annotated package and ignore others
			StreamEx.of(roundEnv.getElementsAnnotatedWith(CldrLocale.class))
				.filterBy(Element::getKind, ElementKind.PACKAGE)
				.findAny()
				.map(PackageElement.class::cast)
				.ifPresent(this::generateCldrLocaleData);
			return true;
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		} finally {
			System.clearProperty("javax.xml.accessExternalDTD");
		}
	}

	private void initUnmarshaller() throws JAXBException {
		ClassLoader classLoader = JAXBContext.class.getClassLoader();
		if (ldmUnmarshaller == null) {
			ldmUnmarshaller = JAXBContext.newInstance(Ldml.class.getPackageName(), classLoader).createUnmarshaller();
		}
		if (supUnmarshaller == null) {
			supUnmarshaller = JAXBContext.newInstance(SupplementalData.class.getPackageName(), classLoader).createUnmarshaller();
		}
	}

	@SuppressWarnings("resource")
	private void generateCldrTimeData(final PackageElement annotatedPackage) {
		Name packageName = annotatedPackage.getQualifiedName();
		Optional<SupplementalData> supplementalData = loadSupplementalData();
		StreamEx.of(supplementalData)
			.map(SupplementalData::getCodeMappings)
			.flatCollection(CodeMappings::getTerritoryCodes)
			.map(TerritoryCodes::getType)
			.toListAndThen(l -> generateTerritoryEnumClass(l, packageName));
		supplementalData
			.map(SupplementalData::getWeekData)
			.ifPresent(d -> generateWeekInfoClass(d, packageName));
		generatePatternInfoClass(loadMainData(), packageName);
	}

	private void generateCldrLocaleData(final PackageElement annotatedPackage) {
		Name packageName = annotatedPackage.getQualifiedName();
		generateLocaleInfoClass(loadMainData(), packageName);
	}

	private Void generateTerritoryEnumClass(final List<String> territories, final Name packageName) {
		processingEnv.getMessager().printMessage(Kind.NOTE, "Generate territory enum");
		final TerritoryEnumBuilder enumBuilder = TerritoryEnumBuilder.create(REGION_ENUM_NAME);
		// Default value - any territory (all world)
		enumBuilder.addEnumConstant(OTHER_TERRITORIES);
		territories.forEach(enumBuilder::addEnumConstant);
		TypeSpec enumSpec = enumBuilder.build();

		writeJavaFile(packageName, enumSpec);
		return null;
	}

	private Optional<SupplementalData> loadSupplementalData() {
		processingEnv.getMessager().printMessage(Kind.NOTE, "Load SupplementalData");
		try {
			URL data = CldrDataProcessor.class.getResource(CLDR_XML);
			return loadSupplementalData(data);
		} catch (final Exception e) {
			processingEnv.getMessager().printMessage(Kind.ERROR, String.format("Can't load CLDR SupplementalData: %s", e.getMessage()));
			return Optional.empty();
		}
	}

	@SuppressWarnings("resource")
	private List<Ldml> loadMainData() {
		if (mainData.isEmpty()) {
			processingEnv.getMessager().printMessage(Kind.NOTE, "Load Main data");
			mainData = getMainUrls()
				.map(this::loadLdml)
				.flatMap(StreamEx::of)
				.toImmutableList();
		}
		return mainData;
	}

	@SuppressWarnings("resource")
	private StreamEx<URL> getMainUrls() {
		try (InputStream content = CldrDataProcessor.class.getResourceAsStream(LDML_XML_LIST)) {
			String list = new String(content.readAllBytes(), StandardCharsets.UTF_8);
			return StreamEx.split(list, ';')
				.map(CldrDataProcessor.class::getResource)
				.nonNull();
		} catch (IOException e) {
			processingEnv.getMessager().printMessage(Kind.ERROR, String.format("Can't get LDML data list. Error: %s", e.getMessage()));
			return StreamEx.of();
		}
	}

	private Optional<Ldml> loadLdml(final URL data) {
		try {
			return Optional.of(Ldml.class.cast(ldmUnmarshaller.unmarshal(data)));
		} catch (JAXBException e) {
			processingEnv.getMessager().printMessage(Kind.ERROR, String.format("Can't load data from %s: %s", data, e.getMessage()));
			return Optional.empty();
		}
	}

	private Optional<SupplementalData> loadSupplementalData(final URL data) {
		try {
			return Optional.of((SupplementalData) supUnmarshaller.unmarshal(data));
		} catch (JAXBException e) {
			processingEnv.getMessager().printMessage(Kind.ERROR, String.format("Can't load data from %s: %s", data, e.getMessage()));
			return Optional.empty();
		}
	}

	@SuppressWarnings("resource")
	private Void generateWeekInfoClass(final WeekData weekData, final Name packageName) {
		processingEnv.getMessager().printMessage(Kind.NOTE, "Generate week info");
		final WeekInfoClassBuilder builder = WeekInfoClassBuilder.create(packageName, WEEK_INFO_CLASS_NAME);
		DayOfWeek firstDay = StreamEx.of(weekData.getFirstDay())
			.filter(CldrDataProcessor::isDefault)
			.findAny()
			.map(FirstDay::getDay)
			.map(CldrDataProcessor::toDayOfWeek)
			.orElse(DayOfWeek.MONDAY);

		builder.addDefaultFirstDay(firstDay);

		int minDays = StreamEx.of(weekData.getMinDays())
			.filter(CldrDataProcessor::isDefault)
			.findAny()
			.map(MinDays::getCount)
			.map(Integer::valueOf)
			.orElse(FALBACK_MIN_DAYS)
			.intValue();

		builder.addDefaultMinDays(minDays);

		StreamEx.of(weekData.getFirstDay())
			.remove(CldrDataProcessor::isDefault)
			.remove(CldrDataProcessor::isDraft)
			.remove(CldrDataProcessor::isAlt)
			.mapToEntry(FirstDay::getDay, FirstDay::getTerritories)
			.mapKeys(CldrDataProcessor::toDayOfWeek)
			.forKeyValue(builder::addFirstDayEntry);

		StreamEx.of(weekData.getMinDays())
			.remove(CldrDataProcessor::isDefault)
			.remove(CldrDataProcessor::isDraft)
			.remove(CldrDataProcessor::isAlt)
			.mapToEntry(MinDays::getCount, MinDays::getTerritories)
			.mapKeys(Integer::valueOf)
			.forKeyValue(builder::addMinDaysEntry);

		TypeSpec spec = builder.build();

		writeJavaFile(packageName, spec, builder.getStaticImports());

		return null;
	}

	private static boolean isDefault(FirstDay firstDay) {
		return isDefault(firstDay.getTerritories());
	}

	private static boolean isDraft(FirstDay firstDay) {
		return firstDay.getDraft() != null && !firstDay.getDraft().isEmpty();
	}

	private static boolean isAlt(FirstDay firstDay) {
		return firstDay.getAlt() != null && !firstDay.getAlt().isEmpty();
	}

	private static boolean isDefault(MinDays minDays) {
		return isDefault(minDays.getTerritories());
	}

	private static boolean isDraft(MinDays minDays) {
		return minDays.getDraft() != null && !minDays.getDraft().isEmpty();
	}

	private static boolean isAlt(MinDays minDays) {
		return minDays.getAlt() != null && !minDays.getAlt().isEmpty();
	}

	private static boolean isDefault(List<String> territories) {
		return territories.contains(OTHER_TERRITORIES);
	}

	private static DayOfWeek toDayOfWeek(String day) {
		switch (day) {
		case "mon":
			return DayOfWeek.MONDAY;
		case "tue":
			return DayOfWeek.TUESDAY;
		case "wed":
			return DayOfWeek.WEDNESDAY;
		case "thu":
			return DayOfWeek.THURSDAY;
		case "fri":
			return DayOfWeek.FRIDAY;
		case "sat":
			return DayOfWeek.SATURDAY;
		case "sun":
			return DayOfWeek.SUNDAY;
		default:
			return null;
		}
	}

	@SuppressWarnings("resource")
	private Void generateLocaleInfoClass(final List<Ldml> ldmls, final Name packageName) {

		processingEnv.getMessager().printMessage(Kind.NOTE, "Generate locale info");

		final LocaleInfoClassBuilder builder = LocaleInfoClassBuilder.create(packageName, LOCALE_INFO_CLASS_NAME);

		StreamEx.of(ldmls)
			.map(Ldml::getIdentity)
			.forEach(builder::addLocale);

		TypeSpec spec = builder.build();

		writeJavaFile(packageName, spec);

		return null;
	}

	private Void generatePatternInfoClass(final List<Ldml> ldmls, final Name packageName) {

		processingEnv.getMessager().printMessage(Kind.NOTE, "Generate pattern info");

		final PatternInfoClassBuilder builder = PatternInfoClassBuilder.create(processingEnv.getMessager(), packageName, PATTERN_INFO_CLASS_NAME);

		ldmls.forEach(builder::updatePatternInfoClass);

		for (FormatStyle formatStyle : FormatStyle.values()) {
			TypeSpec spec = builder.build(formatStyle);
			writeJavaFile(packageName, spec, builder.getStaticImports());
		}

		return null;
	}

	private void writeJavaFile(final Name packageName, TypeSpec spec) {
		writeJavaFile(packageName, spec, List.of());
	}

	private void writeJavaFile(final Name packageName, TypeSpec spec, List<ClassName> staticImports) {

		final Builder javaFileBuilder = JavaFile.builder(packageName.toString(), spec).indent("\t");
		staticImports.forEach(i -> javaFileBuilder.addStaticImport(i, "*"));
		JavaFile javaFile = javaFileBuilder.build();

		try {
			final JavaFileObject jfo = processingEnv.getFiler().createSourceFile(packageName.toString() + "." + spec.name);
			try (Writer wr = jfo.openWriter()) {
				javaFile.writeTo(wr);
			}
		} catch (final IOException e) {
			processingEnv.getMessager().printMessage(Kind.ERROR, String.format("Can't write class %s to package %s: %s", spec, packageName, e.getMessage()));
		}
	}

}
