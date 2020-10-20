package org.jresearch.gwt.time.apt;

import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
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
import javax.tools.StandardLocation;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.jresearch.gwt.time.apt.annotation.Cldr;
import org.jresearch.gwt.time.apt.cldr.ldml.Ldml;
import org.jresearch.gwt.time.apt.cldr.ldmlSupplemental.CodeMappings;
import org.jresearch.gwt.time.apt.cldr.ldmlSupplemental.FirstDay;
import org.jresearch.gwt.time.apt.cldr.ldmlSupplemental.MinDays;
import org.jresearch.gwt.time.apt.cldr.ldmlSupplemental.SupplementalData;
import org.jresearch.gwt.time.apt.cldr.ldmlSupplemental.TerritoryCodes;
import org.jresearch.gwt.time.apt.cldr.ldmlSupplemental.WeekData;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.JavaFile.Builder;
import com.squareup.javapoet.TypeSpec;

import one.util.streamex.StreamEx;

@AutoService(Processor.class)
@SuppressWarnings("nls")
public class CldrSupplementalDataProcessor extends AbstractProcessor {

//	private static final Logger LOGGER = LoggerFactory.getLogger(CldrSupplementalDataProcessor.class);

	private static final String OTHER_TERRITORIES = "001";

	private static final Integer FALBACK_MIN_DAYS = Integer.valueOf(1);

	private static final String CLDR_XML = "supplementalData.xml";
	private static final String LDML_XML = "root.xml";
	private static final String WEEK_INFO_CLASS_NAME = "WeekInfo";
	static final String LOCALE_INFO_CLASS_NAME = "LocaleInfo";
	private static final String PATTERN_INFO_CLASS_NAME = "PatternInfo";

	static final String REGION_ENUM_NAME = "Region";

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return ImmutableSet.of(Cldr.class.getName());
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@SuppressWarnings("resource")
	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
		// get first available annotated package and ignore others
		StreamEx.of(roundEnv.getElementsAnnotatedWith(Cldr.class))
				.filterBy(Element::getKind, ElementKind.PACKAGE)
				.findAny()
				.map(PackageElement.class::cast)
				.ifPresent(this::generateCldrData);
		return true;
	}

	@SuppressWarnings("resource")
	private void generateCldrData(final PackageElement annotatedPackage) {
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
		List<Ldml> mainData = loadMainData();
		generateLocaleInfoClass(mainData, packageName);
		generatePatternInfoClass(mainData, packageName);

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
			System.setProperty("javax.xml.accessExternalDTD", "all");
			final URL data = processingEnv
					.getFiler()
					.getResource(StandardLocation.CLASS_OUTPUT, "cldr.common.supplemental", CLDR_XML)
					.toUri()
					.toURL();
			return loadSupplementalData(data);
		} catch (final IOException e) {
			processingEnv.getMessager().printMessage(Kind.ERROR, String.format("Can't load CLDR SupplementalData: %s", e.getMessage()));
			return Optional.empty();
		} finally {
			System.clearProperty("javax.xml.accessExternalDTD");
		}
	}

	@SuppressWarnings("resource")
	private List<Ldml> loadMainData() {
		processingEnv.getMessager().printMessage(Kind.NOTE, "Load Main data");
		try {
			System.setProperty("javax.xml.accessExternalDTD", "all");
			final URI uri = processingEnv.getFiler()
					.getResource(StandardLocation.CLASS_OUTPUT, "cldr.common.main", LDML_XML)
					.toUri();
			com.google.common.collect.ImmutableList.Builder<Ldml> builder = ImmutableList.builder();
			Path mainFolder = Paths.get(uri).getParent();
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(mainFolder, "*.xml")) {
				JAXBContext context = JAXBContext.newInstance(Ldml.class);
				StreamEx.of(stream.iterator())
						.parallel()
						.map(Path::toUri)
						.map(u -> loadLdml(context, u))
						.flatMap(StreamEx::of)
						.forEach(builder::add);
			} catch (JAXBException e) {
				processingEnv.getMessager().printMessage(Kind.ERROR, String.format("Can't load Ldml data: %s", e.getMessage()));
			}
			return builder.build();
		} catch (IOException e) {
			processingEnv.getMessager().printMessage(Kind.ERROR, String.format("Can't load LDML data. Error: {}", e.getMessage()));
			return ImmutableList.of();
		} finally {
			System.clearProperty("javax.xml.accessExternalDTD");
		}
	}

	private Optional<Ldml> loadLdml(JAXBContext context, final URI data) {
		try {
			return loadLdml(context, data.toURL());
		} catch (MalformedURLException e) {
			processingEnv.getMessager().printMessage(Kind.ERROR, String.format("Can't load data from path %s, skipped. Error: %s", data, e.getMessage()));
			return Optional.empty();
		}
	}

	private Optional<Ldml> loadLdml(JAXBContext context, final URL data) {
		processingEnv.getMessager().printMessage(Kind.NOTE, String.format("Process %s", data));
		try {
			return Optional.of(Ldml.class.cast(context.createUnmarshaller().unmarshal(data)));
		} catch (JAXBException e) {
			processingEnv.getMessager().printMessage(Kind.ERROR, String.format("Can't load data from %s: %s", data, e.getMessage()));
			return Optional.empty();
		}
	}

	private Optional<SupplementalData> loadSupplementalData(final URL data) {
		processingEnv.getMessager().printMessage(Kind.NOTE, String.format("Process %s", data));
		try {
			JAXBContext context = JAXBContext.newInstance(SupplementalData.class);
			return Optional.of((SupplementalData) context.createUnmarshaller().unmarshal(data));
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
				.filter(CldrSupplementalDataProcessor::isDefault)
				.findAny()
				.map(FirstDay::getDay)
				.map(CldrSupplementalDataProcessor::toDayOfWeek)
				.orElse(DayOfWeek.MONDAY);

		builder.addDefaultFirstDay(firstDay);

		int minDays = StreamEx.of(weekData.getMinDays())
				.filter(CldrSupplementalDataProcessor::isDefault)
				.findAny()
				.map(MinDays::getCount)
				.map(Integer::valueOf)
				.orElse(FALBACK_MIN_DAYS)
				.intValue();

		builder.addDefaultMinDays(minDays);

		StreamEx.of(weekData.getFirstDay())
				.remove(CldrSupplementalDataProcessor::isDefault)
				.remove(CldrSupplementalDataProcessor::isDraft)
				.remove(CldrSupplementalDataProcessor::isAlt)
				.mapToEntry(FirstDay::getDay, FirstDay::getTerritories)
				.mapKeys(CldrSupplementalDataProcessor::toDayOfWeek)
				.forKeyValue(builder::addFirstDayEntry);

		StreamEx.of(weekData.getMinDays())
				.remove(CldrSupplementalDataProcessor::isDefault)
				.remove(CldrSupplementalDataProcessor::isDraft)
				.remove(CldrSupplementalDataProcessor::isAlt)
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

		TypeSpec spec = builder.build();

		writeJavaFile(packageName, spec, builder.getStaticImports());

		return null;
	}

	private void writeJavaFile(final Name packageName, TypeSpec spec) {
		writeJavaFile(packageName, spec, ImmutableList.of());
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
