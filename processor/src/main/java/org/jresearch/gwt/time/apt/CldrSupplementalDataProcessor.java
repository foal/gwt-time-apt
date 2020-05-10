package org.jresearch.gwt.time.apt;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.time.DayOfWeek;
import java.util.Collection;
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
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.jresearch.gwt.time.apt.annotation.Cldr;
import org.jresearch.gwt.time.apt.cldr.CodeMappings;
import org.jresearch.gwt.time.apt.cldr.FirstDay;
import org.jresearch.gwt.time.apt.cldr.LanguagePopulation;
import org.jresearch.gwt.time.apt.cldr.MinDays;
import org.jresearch.gwt.time.apt.cldr.SupplementalData;
import org.jresearch.gwt.time.apt.cldr.Territory;
import org.jresearch.gwt.time.apt.cldr.TerritoryCodes;
import org.jresearch.gwt.time.apt.cldr.TerritoryInfo;
import org.jresearch.gwt.time.apt.cldr.WeekData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.JavaFile.Builder;
import com.squareup.javapoet.TypeSpec;

import one.util.streamex.StreamEx;

@AutoService(Processor.class)
@SuppressWarnings("nls")
public class CldrSupplementalDataProcessor extends AbstractProcessor {

	private static final String OTHER_TERRITORIES = "001";

	private static final Integer FALBACK_MIN_DAYS = Integer.valueOf(1);

	private static final Logger LOGGER = LoggerFactory.getLogger(CldrSupplementalDataProcessor.class);

	private static final String CLDR_XML = "supplementalData.xml";
	public static final String REGION_ENUM_NAME = "Region";
	private static final String WEEK_INFO_CLASS_NAME = "WeekInfo";
	private static final String LOCALE_INFO_CLASS_NAME = "LocaleInfo";

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return ImmutableSet.of(Cldr.class.getName());
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

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
		supplementalData
				.map(SupplementalData::getTerritoryInfo)
				.ifPresent(d -> generateLocaleInfoClass(d, packageName));
	}

	private Void generateTerritoryEnumClass(final List<String> territories, final Name packageName) {
		final TerritoryEnumBuilder enumBuilder = TerritoryEnumBuilder.create(packageName, REGION_ENUM_NAME);
		// Default value - any territory (all world)
		enumBuilder.addEnumConstant(OTHER_TERRITORIES);
		territories.forEach(enumBuilder::addEnumConstant);
		TypeSpec enumSpec = enumBuilder.build();

		final JavaFile javaFile = JavaFile.builder(packageName.toString(), enumSpec).indent("\t").build();
		try {
			final JavaFileObject jfo = processingEnv.getFiler().createSourceFile(packageName.toString() + "." + REGION_ENUM_NAME);
			try (Writer wr = jfo.openWriter()) {
				javaFile.writeTo(wr);
			}
		} catch (final IOException e) {
			LOGGER.error("Can't generate territory enumeration: {}", e.getMessage(), e);
		}
		return null;
	}

	private Optional<SupplementalData> loadSupplementalData() {
		try {
			System.setProperty("javax.xml.accessExternalDTD", "all");
			final URL data = processingEnv
					.getFiler()
					.getResource(StandardLocation.CLASS_OUTPUT, "cldr.common.supplemental", CLDR_XML)
					.toUri()
					.toURL();
			JAXBContext context = JAXBContext.newInstance(SupplementalData.class);
			return Optional.ofNullable((SupplementalData) context.createUnmarshaller().unmarshal(data));
		} catch (final IOException | JAXBException e) {
			LOGGER.error("Can't load CLDR SupplementalData: {}", e.getMessage(), e);
			return Optional.empty();
		} finally {
			System.clearProperty("javax.xml.accessExternalDTD");
		}
	}

	private Void generateWeekInfoClass(final WeekData weekData, final Name packageName) {
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

		final Builder javaFileBuilder = JavaFile.builder(packageName.toString(), spec).indent("\t");
		builder.getStaticImports().forEach(c -> javaFileBuilder.addStaticImport(c, "*"));
		JavaFile javaFile = javaFileBuilder.build();

		try {
			final JavaFileObject jfo = processingEnv.getFiler().createSourceFile(packageName.toString() + "." + WEEK_INFO_CLASS_NAME);
			try (Writer wr = jfo.openWriter()) {
				javaFile.writeTo(wr);
			}
		} catch (final IOException e) {
			LOGGER.error("Can't generate week info class: {}", e.getMessage(), e);
		}
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

	private Void generateLocaleInfoClass(final TerritoryInfo territoryInfo, final Name packageName) {
		final LocaleInfoClassBuilder builder = LocaleInfoClassBuilder.create(packageName, LOCALE_INFO_CLASS_NAME);

		StreamEx.of(territoryInfo.getTerritory())
				.flatCollection(CldrSupplementalDataProcessor::toTerritoryLangInfo)
				.forEach(builder::addLocale);

		TypeSpec spec = builder.build();

		final Builder javaFileBuilder = JavaFile.builder(packageName.toString(), spec).indent("\t");
		JavaFile javaFile = javaFileBuilder.build();

		try {
			final JavaFileObject jfo = processingEnv.getFiler().createSourceFile(packageName.toString() + "." + LOCALE_INFO_CLASS_NAME);
			try (Writer wr = jfo.openWriter()) {
				javaFile.writeTo(wr);
			}
		} catch (final IOException e) {
			LOGGER.error("Can't generate week info class: {}", e.getMessage(), e);
		}
		return null;
	}

	private static Collection<TerritoryLangInfo> toTerritoryLangInfo(Territory territory) {
		return StreamEx.of(territory.getLanguagePopulation())
				.map(LanguagePopulation::getType)
				.map(lang -> ImmutableTerritoryLangInfo
						.builder()
						.territory(territory.getType())
						.language(lang)
						.build())
				.map(TerritoryLangInfo.class::cast)
				.toList();
	}

}
