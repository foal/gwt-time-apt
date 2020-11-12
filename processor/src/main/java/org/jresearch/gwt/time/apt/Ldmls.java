package org.jresearch.gwt.time.apt;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.jresearch.gwt.time.apt.cldr.ldml.Calendar;
import org.jresearch.gwt.time.apt.cldr.ldml.Calendars;
import org.jresearch.gwt.time.apt.cldr.ldml.DateFormat;
import org.jresearch.gwt.time.apt.cldr.ldml.DateFormatLength;
import org.jresearch.gwt.time.apt.cldr.ldml.DateFormats;
import org.jresearch.gwt.time.apt.cldr.ldml.DateTimeFormat;
import org.jresearch.gwt.time.apt.cldr.ldml.DateTimeFormatLength;
import org.jresearch.gwt.time.apt.cldr.ldml.DateTimeFormats;
import org.jresearch.gwt.time.apt.cldr.ldml.Dates;
import org.jresearch.gwt.time.apt.cldr.ldml.Identity;
import org.jresearch.gwt.time.apt.cldr.ldml.Language;
import org.jresearch.gwt.time.apt.cldr.ldml.Ldml;
import org.jresearch.gwt.time.apt.cldr.ldml.Script;
import org.jresearch.gwt.time.apt.cldr.ldml.Territory;
import org.jresearch.gwt.time.apt.cldr.ldml.TimeFormat;
import org.jresearch.gwt.time.apt.cldr.ldml.TimeFormatLength;
import org.jresearch.gwt.time.apt.cldr.ldml.TimeFormats;
import org.jresearch.gwt.time.apt.cldr.ldml.Variant;

import one.util.streamex.StreamEx;

@SuppressWarnings("nls")
public class Ldmls {

	private static final String ROOT = "root";

	private Ldmls() {
		// prevent instantiation
	}

	public static Optional<IdentityInfo> getIdentityInfo(final @Nonnull Ldml ldml) {
		return Optional.of(ldml).map(Ldml::getIdentity).flatMap(Ldmls::getIdentityInfo);
	}

	public static Optional<IdentityInfo> getIdentityInfo(final @Nonnull Identity identity) {
		Optional<String> language = get(identity, Language.class).map(Language::getType);
		if (language.isPresent()) {
			String territory = get(identity, Territory.class).map(Territory::getType).orElse("");
			String script = get(identity, Script.class).map(Script::getType).orElse("");
			String variant = get(identity, Variant.class).map(Variant::getType).orElse("");
			return Optional.of(ImmutableIdentityInfo.builder()
					.language(language.get())
					.territory(territory)
					.script(script)
					.variant(variant)
					.build());
		}
		return Optional.empty();
	}

	public static <T> Optional<T> get(DateTimeFormat formats, Class<T> propertyType) {
		return get(propertyType, formats::getAliasOrPatternOrDisplayNameOrSpecial);
	}

	public static <T> Optional<T> get(TimeFormat formats, Class<T> propertyType) {
		return get(propertyType, formats::getAliasOrPatternOrDisplayNameOrSpecial);
	}

	public static <T> Optional<T> get(DateFormat formats, Class<T> propertyType) {
		return get(propertyType, formats::getAliasOrPatternOrDisplayNameOrSpecial);
	}

	public static <T> Optional<T> get(DateTimeFormatLength formats, Class<T> propertyType) {
		return get(propertyType, formats::getAliasOrDefaultOrDateTimeFormatOrSpecial);
	}

	public static <T> Optional<T> get(TimeFormatLength formats, Class<T> propertyType) {
		return get(propertyType, formats::getAliasOrDefaultOrTimeFormatOrSpecial);
	}

	public static <T> Optional<T> get(DateFormatLength formats, Class<T> propertyType) {
		return get(propertyType, formats::getAliasOrDefaultOrDateFormatOrSpecial);
	}

	public static <T> List<T> getAll(DateTimeFormats formats, Class<T> propertyType) {
		return getAll(propertyType, formats::getAliasOrDefaultOrDateTimeFormatLengthOrAvailableFormatsOrAppendItemsOrIntervalFormatsOrSpecial);
	}

	public static <T> List<T> getAll(TimeFormats formats, Class<T> propertyType) {
		return getAll(propertyType, formats::getAliasOrDefaultOrTimeFormatLengthOrSpecial);
	}

	public static <T> List<T> getAll(DateFormats formats, Class<T> propertyType) {
		return getAll(propertyType, formats::getAliasOrDefaultOrDateFormatLengthOrSpecial);
	}

	public static <T> Optional<T> get(DateTimeFormats formats, Class<T> propertyType) {
		return get(propertyType, formats::getAliasOrDefaultOrDateTimeFormatLengthOrAvailableFormatsOrAppendItemsOrIntervalFormatsOrSpecial);
	}

	public static <T> Optional<T> get(TimeFormats formats, Class<T> propertyType) {
		return get(propertyType, formats::getAliasOrDefaultOrTimeFormatLengthOrSpecial);
	}

	public static <T> Optional<T> get(DateFormats formats, Class<T> propertyType) {
		return get(propertyType, formats::getAliasOrDefaultOrDateFormatLengthOrSpecial);
	}

	public static <T> Optional<T> get(Calendar calendar, Class<T> propertyType) {
		return get(propertyType, calendar::getAliasOrMonthsOrMonthNamesOrMonthAbbrOrMonthPatternsOrDaysOrDayNamesOrDayAbbrOrQuartersOrWeekOrAmOrPmOrDayPeriodsOrErasOrCyclicNameSetsOrDateFormatsOrTimeFormatsOrDateTimeFormatsOrFieldsOrSpecial);
	}

	public static <T> List<T> getAll(Calendars calendars, Class<T> propertyType) {
		return getAll(propertyType, calendars::getAliasOrDefaultOrCalendarOrSpecial);
	}

	public static <T> Optional<T> get(Calendars calendars, Class<T> propertyType) {
		return get(propertyType, calendars::getAliasOrDefaultOrCalendarOrSpecial);
	}

	public static <T> Optional<T> get(Dates dates, Class<T> propertyType) {
		return get(propertyType, dates::getAliasOrLocalizedPatternCharsOrDateRangePatternOrCalendarsOrFieldsOrTimeZoneNamesOrSpecial);
	}

	public static <T> Optional<T> get(Identity identity, Class<T> propertyType) {
		return get(propertyType, identity::getAliasOrVersionOrGenerationOrLanguageOrScriptOrTerritoryOrVariantOrSpecial);
	}

	public static <T> Optional<T> get(Ldml ldml, Class<T> propertyType) {
		return get(propertyType, ldml::getAliasOrFallbackOrLocaleDisplayNamesOrLayoutOrContextTransformsOrCharactersOrDelimitersOrMeasurementOrDatesOrNumbersOrUnitsOrListPatternsOrCollationsOrPosixOrCharacterLabelsOrSegmentationsOrRbnfOrTypographicNamesOrAnnotationsOrMetadataOrReferencesOrSpecial);
	}

	@SuppressWarnings("resource")
	public static <T> List<T> getAll(Class<T> propertyType, Supplier<List<Object>> supplier) {
		return StreamEx.of(supplier.get())
				.filter(o -> propertyType.isAssignableFrom(o.getClass()))
				.map(propertyType::cast)
				.toList();
	}

	@SuppressWarnings("resource")
	public static <T> Optional<T> get(Class<T> propertyType, Supplier<List<Object>> supplier) {
		return StreamEx.of(supplier.get())
				.findAny(o -> propertyType.isAssignableFrom(o.getClass()))
				.map(propertyType::cast);
	}

	@SuppressWarnings("resource")
	public static String createFieldName(final IdentityInfo info) {
		return StreamEx.of(info.language(), info.script(), info.territory(), info.variant())
				.remove(String::isEmpty)
				.map(String::toUpperCase)
				.joining("_");
	}

	@SuppressWarnings("resource")
	public static String createLanguageTag(final IdentityInfo info) {
		// Special case for ROOT
		if (info.language().equalsIgnoreCase(ROOT)) {
			return "";
		}
		return StreamEx.of(info.language(), info.script(), info.territory(), info.variant())
				.remove(String::isEmpty)
				.joining("-");
	}
}
