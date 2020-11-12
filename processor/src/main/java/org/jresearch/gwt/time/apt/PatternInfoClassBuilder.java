package org.jresearch.gwt.time.apt;

import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic.Kind;

import org.jresearch.gwt.time.apt.base.Bases;
import org.jresearch.gwt.time.apt.base.Chrono;
import org.jresearch.gwt.time.apt.base.PatternType;
import org.jresearch.gwt.time.apt.cldr.ldml.Alias;
import org.jresearch.gwt.time.apt.cldr.ldml.Calendar;
import org.jresearch.gwt.time.apt.cldr.ldml.Calendars;
import org.jresearch.gwt.time.apt.cldr.ldml.DateFormat;
import org.jresearch.gwt.time.apt.cldr.ldml.DateFormatLength;
import org.jresearch.gwt.time.apt.cldr.ldml.DateFormats;
import org.jresearch.gwt.time.apt.cldr.ldml.DateTimeFormat;
import org.jresearch.gwt.time.apt.cldr.ldml.DateTimeFormatLength;
import org.jresearch.gwt.time.apt.cldr.ldml.DateTimeFormats;
import org.jresearch.gwt.time.apt.cldr.ldml.Dates;
import org.jresearch.gwt.time.apt.cldr.ldml.Ldml;
import org.jresearch.gwt.time.apt.cldr.ldml.Pattern;
import org.jresearch.gwt.time.apt.cldr.ldml.TimeFormat;
import org.jresearch.gwt.time.apt.cldr.ldml.TimeFormatLength;
import org.jresearch.gwt.time.apt.cldr.ldml.TimeFormats;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

import one.util.streamex.StreamEx;

/**
 * <pre>
 * public class PatternInfo {
 * 	public final static Map<String, PatternCoordinates[] DATE_FULL_PATTERNS = new HashMap<>();
 * 	public final static Map<String, PatternCoordinates[] DATE_LONG_PATTERNS = new HashMap<>();
 * 	public final static Map<String, PatternCoordinates[] DATE_MEDIUM_PATTERNS = new HashMap<>();
 * 	public final static Map<String, PatternCoordinates[] DATE_SHORT_DATE_PATTERNS = new HashMap<>();
 *
 * 	public final static Map<String, PatternCoordinates[] DATE_TIME_FULL_PATTERNS = new HashMap<>();
 * 	public final static Map<String, PatternCoordinates[] DATE_TIME_LONG_PATTERNS = new HashMap<>();
 * 	public final static Map<String, PatternCoordinates[] DATE_TIME_MEDIUM_PATTERNS = new HashMap<>();
 * 	public final static Map<String, PatternCoordinates[] DATE_TIME_SHORT_DATE_PATTERNS = new HashMap<>();
 *
 * 	public final static Map<String, PatternCoordinates[] TIME_FULL_PATTERNS = new HashMap<>();
 * 	public final static Map<String, PatternCoordinates[] TIME_LONG_PATTERNS = new HashMap<>();
 * 	public final static Map<String, PatternCoordinates[] TIME_MEDIUM_PATTERNS = new HashMap<>();
 * 	public final static Map<String, PatternCoordinates[] TIME_SHORT_DATE_PATTERNS = new HashMap<>();
 *
 * 	static {
 * 		DATE_FULL_PATTERNS.put("ttt", new PatternCoordinates>[] {
 * 				of(HIJRAH_UMALQURA, AF_NA),
 * 				});
 * 	}
 * }
 * </pre>
 */
@SuppressWarnings("nls")
public class PatternInfoClassBuilder {

	private final static List<String> IGNORED_PATTERNS = ImmutableList.of("↑↑↑");
	private final static java.util.regex.Pattern ALIAS_PATTERN = java.util.regex.Pattern.compile("\\.\\.\\/\\.\\.\\/calendar\\[@type='(.*)'");

	private final Builder poetBuilder;

	private final com.squareup.javapoet.CodeBlock.Builder staticInitBlock;
	private final List<ClassName> staticImports;

	public final ListMultimap<String, AptPatternCoordinates> dateFullPatterns = ArrayListMultimap.create();
	public final ListMultimap<String, AptPatternCoordinates> dateLongPatterns = ArrayListMultimap.create();
	public final ListMultimap<String, AptPatternCoordinates> dateMediumPatterns = ArrayListMultimap.create();
	public final ListMultimap<String, AptPatternCoordinates> dateShortPatterns = ArrayListMultimap.create();

	public final ListMultimap<String, AptPatternCoordinates> dateTimeFullPatterns = ArrayListMultimap.create();
	public final ListMultimap<String, AptPatternCoordinates> dateTimeLongPatterns = ArrayListMultimap.create();
	public final ListMultimap<String, AptPatternCoordinates> dateTimeMediumPatterns = ArrayListMultimap.create();
	public final ListMultimap<String, AptPatternCoordinates> dateTimeShortPatterns = ArrayListMultimap.create();

	public final ListMultimap<String, AptPatternCoordinates> timeFullPatterns = ArrayListMultimap.create();
	public final ListMultimap<String, AptPatternCoordinates> timeLongPatterns = ArrayListMultimap.create();
	public final ListMultimap<String, AptPatternCoordinates> timeMediumPatterns = ArrayListMultimap.create();
	public final ListMultimap<String, AptPatternCoordinates> timeShortPatterns = ArrayListMultimap.create();

	private final Messager messager;

	private PatternInfoClassBuilder(Messager messager, final CharSequence packageName, final CharSequence className) {
		this.messager = messager;
		ArrayTypeName coordinates = ArrayTypeName.of(ClassName.get(packageName.toString(), "PatternCoordinates"));
		ParameterizedTypeName map = ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class), coordinates);

		staticImports = ImmutableList.of(ClassName.get(Chrono.class), ClassName.get(packageName.toString(), "ImmutablePatternCoordinates"));
		staticInitBlock = CodeBlock.builder();
		poetBuilder = TypeSpec
				.classBuilder(className.toString())
				.addModifiers(Modifier.PUBLIC)
				.addField(map(map, "DATE_FULL_PATTERNS"))
				.addField(map(map, "DATE_LONG_PATTERNS"))
				.addField(map(map, "DATE_MEDIUM_PATTERNS"))
				.addField(map(map, "DATE_SHORT_PATTERNS"))
				.addField(map(map, "DATE_TIME_FULL_PATTERNS"))
				.addField(map(map, "DATE_TIME_LONG_PATTERNS"))
				.addField(map(map, "DATE_TIME_MEDIUM_PATTERNS"))
				.addField(map(map, "DATE_TIME_SHORT_PATTERNS"))
				.addField(map(map, "TIME_FULL_PATTERNS"))
				.addField(map(map, "TIME_LONG_PATTERNS"))
				.addField(map(map, "TIME_MEDIUM_PATTERNS"))
				.addField(map(map, "TIME_SHORT_PATTERNS"));
	}

	private static FieldSpec map(ParameterizedTypeName map, String name) {
		return FieldSpec.builder(map, name, Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
				.initializer("new $T<>()", HashMap.class)
				.build();
	}

	public static PatternInfoClassBuilder create(final Messager messager, final CharSequence packageName, final CharSequence className) {
		return new PatternInfoClassBuilder(messager, packageName, className);
	}

	public PatternInfoClassBuilder updatePatternInfoClass(Ldml ldml) {

		Optional<String> languageTag = Ldmls.getIdentityInfo(ldml).map(Ldmls::createLanguageTag);

		if (languageTag.isPresent()) {

			Optional<Calendars> clendars = Ldmls.get(ldml, Dates.class)
					.flatMap(d -> Ldmls.get(d, Calendars.class));
			clendars
					.map(c -> Ldmls.getAll(c, Calendar.class))
					.orElseGet(ImmutableList::of)
					.forEach(c -> updatePatternInfoClass(clendars.get(), languageTag.get(), c));
		}

		return this;
	}

	private void updatePatternInfoClass(Calendars calendars, String languageTag, Calendar calendar) {
		Optional<Chrono> chrono = Bases.ofCldr(calendar.getType());
		if (chrono.isPresent()) {
			Ldmls.get(calendar, DateFormats.class)
					.map(f -> to(calendars, f))
					.ifPresent(p -> updatePatternInfoClass(languageTag, chrono.get(), PatternType.DATE, p));
			Ldmls.get(calendar, TimeFormats.class)
					.map(f -> to(calendars, f))
					.ifPresent(p -> updatePatternInfoClass(languageTag, chrono.get(), PatternType.TIME, p));
			Ldmls.get(calendar, DateTimeFormats.class)
					.map(f -> to(calendars, f))
					.ifPresent(p -> updatePatternInfoClass(languageTag, chrono.get(), PatternType.DATE_TIME, p));
		}
	}

	private PatternProvider to(Calendars calendars, DateFormats dateFormats) {
		List<DateFormatLength> formats = getDateFormats(calendars, dateFormats);
		return new PatternProvider() {
			@SuppressWarnings("resource")
			@Override
			public Optional<String> getPattern(FormatStyle formatStyle) {
				return StreamEx.of(formats)
						.filterBy(DateFormatLength::getType, formatStyle.name().toLowerCase())
						.findAny()
						.flatMap(f -> Ldmls.get(f, DateFormat.class))
						.flatMap(f -> Ldmls.get(f, Pattern.class))
						.map(Pattern::getvalue);
			}
		};
	}

	private PatternProvider to(Calendars calendars, TimeFormats timeFormats) {
		List<TimeFormatLength> formats = getTimeFormats(calendars, timeFormats);
		return new PatternProvider() {
			@SuppressWarnings("resource")
			@Override
			public Optional<String> getPattern(FormatStyle formatStyle) {
				return StreamEx.of(formats)
						.filterBy(TimeFormatLength::getType, formatStyle.name().toLowerCase())
						.findAny()
						.flatMap(f -> Ldmls.get(f, TimeFormat.class))
						.flatMap(f -> Ldmls.get(f, Pattern.class))
						.map(Pattern::getvalue);
			}
		};
	}

	private PatternProvider to(Calendars calendars, DateTimeFormats dateFormats) {
		List<DateTimeFormatLength> formats = getDateTimeFormats(calendars, dateFormats);
		return new PatternProvider() {
			@SuppressWarnings("resource")
			@Override
			public Optional<String> getPattern(FormatStyle formatStyle) {
				return StreamEx.of(formats)
						.filterBy(DateTimeFormatLength::getType, formatStyle.name().toLowerCase())
						.findAny()
						.flatMap(f -> Ldmls.get(f, DateTimeFormat.class))
						.flatMap(f -> Ldmls.get(f, Pattern.class))
						.map(Pattern::getvalue);
			}
		};
	}

	@SuppressWarnings("resource")
	private List<TimeFormatLength> getTimeFormats(Calendars calendars, TimeFormats timeFormats) {
		List<TimeFormatLength> result = Ldmls.getAll(timeFormats, TimeFormatLength.class);
		// if no result check for alias
		if (result.isEmpty()) {
			String alias = Ldmls.get(timeFormats, Alias.class).map(Alias::getPath).orElse("");
			Matcher matcher = ALIAS_PATTERN.matcher(alias);
			if (matcher.find()) {
				String calendarType = matcher.group(1);
				result = StreamEx.of(Ldmls.getAll(calendars, Calendar.class))
						.filterBy(Calendar::getType, calendarType)
						.limit(1)
						.map(c -> Ldmls.get(c, TimeFormats.class))
						.flatMap(StreamEx::of)
						.flatCollection(f -> getTimeFormats(calendars, f))
						.toList();
			}
		}
		return result;
	}

	@SuppressWarnings("resource")
	private List<DateFormatLength> getDateFormats(Calendars calendars, DateFormats dateFormats) {
		List<DateFormatLength> result = Ldmls.getAll(dateFormats, DateFormatLength.class);
		// if no result check for alias
		if (result.isEmpty()) {
			String alias = Ldmls.get(dateFormats, Alias.class).map(Alias::getPath).orElse("");
			Matcher matcher = ALIAS_PATTERN.matcher(alias);
			if (matcher.find()) {
				String calendarType = matcher.group(1);
				result = StreamEx.of(Ldmls.getAll(calendars, Calendar.class))
						.filterBy(Calendar::getType, calendarType)
						.limit(1)
						.map(c -> Ldmls.get(c, DateFormats.class))
						.flatMap(StreamEx::of)
						.flatCollection(f -> getDateFormats(calendars, f))
						.toList();
			}
		}
		return result;
	}

	@SuppressWarnings("resource")
	private List<DateTimeFormatLength> getDateTimeFormats(Calendars calendars, DateTimeFormats dateTimeFormats) {
		List<DateTimeFormatLength> result = Ldmls.getAll(dateTimeFormats, DateTimeFormatLength.class);
		// if no result check for alias
		if (result.isEmpty()) {
			String alias = Ldmls.get(dateTimeFormats, Alias.class).map(Alias::getPath).orElse("");
			Matcher matcher = ALIAS_PATTERN.matcher(alias);
			if (matcher.find()) {
				String calendarType = matcher.group(1);
				result = StreamEx.of(Ldmls.getAll(calendars, Calendar.class))
						.filterBy(Calendar::getType, calendarType)
						.limit(1)
						.map(c -> Ldmls.get(c, DateTimeFormats.class))
						.flatMap(StreamEx::of)
						.flatCollection(f -> getDateTimeFormats(calendars, f))
						.toList();
			}
		}
		return result;
	}

	@SuppressWarnings("resource")
	private void updatePatternInfoClass(String languageTag, Chrono chrono, PatternType patternType, PatternProvider provider) {
		StreamEx.of(FormatStyle.values())
				.mapToEntry(provider::getPattern)
				.filterValues(Optional::isPresent)
				.mapValues(Optional::get)
				.forEach(e -> addPattern(languageTag, chrono, patternType, e.getKey(), e.getValue()));
	}

	public PatternInfoClassBuilder addPattern(String languageTag, Chrono chrono, PatternType patternType, FormatStyle formatStyle, String pattern) {
		if (!IGNORED_PATTERNS.contains(pattern)) {
			selectMap(patternType, formatStyle).put(pattern, ImmutableAptPatternCoordinates.of(chrono, languageTag));
		}
		return this;
	}

	private ListMultimap<String, AptPatternCoordinates> selectMap(PatternType patternType, FormatStyle formatStyle) {
		switch (patternType) {
		case DATE:
			return selectDateMap(formatStyle);
		case DATE_TIME:
			return selectDateTimeMap(formatStyle);
		case TIME:
			return selectTimeMap(formatStyle);
		default:
			throw new IllegalArgumentException(String.format("Unnsupported patternType: %s", patternType));
		}
	}

	private ListMultimap<String, AptPatternCoordinates> selectDateMap(FormatStyle formatStyle) {
		switch (formatStyle) {
		case FULL:
			return dateFullPatterns;
		case LONG:
			return dateLongPatterns;
		case MEDIUM:
			return dateMediumPatterns;
		case SHORT:
			return dateShortPatterns;
		default:
			throw new IllegalArgumentException(String.format("Unnsupported formatStyle: %s", formatStyle));
		}
	}

	private ListMultimap<String, AptPatternCoordinates> selectDateTimeMap(FormatStyle formatStyle) {
		switch (formatStyle) {
		case FULL:
			return dateTimeFullPatterns;
		case LONG:
			return dateTimeLongPatterns;
		case MEDIUM:
			return dateTimeMediumPatterns;
		case SHORT:
			return dateTimeShortPatterns;
		default:
			throw new IllegalArgumentException(String.format("Unnsupported formatStyle: %s", formatStyle));
		}
	}

	private ListMultimap<String, AptPatternCoordinates> selectTimeMap(FormatStyle formatStyle) {
		switch (formatStyle) {
		case FULL:
			return timeFullPatterns;
		case LONG:
			return timeLongPatterns;
		case MEDIUM:
			return timeMediumPatterns;
		case SHORT:
			return timeShortPatterns;
		default:
			throw new IllegalArgumentException(String.format("Unnsupported formatStyle: %s", formatStyle));
		}
	}

	public TypeSpec build() {
		generatePatterns(dateFullPatterns, "DATE_FULL_PATTERNS");
		generatePatterns(dateLongPatterns, "DATE_LONG_PATTERNS");
		generatePatterns(dateMediumPatterns, "DATE_MEDIUM_PATTERNS");
		generatePatterns(dateShortPatterns, "DATE_SHORT_PATTERNS");
		generatePatterns(dateTimeFullPatterns, "DATE_TIME_FULL_PATTERNS");
		generatePatterns(dateTimeLongPatterns, "DATE_TIME_LONG_PATTERNS");
		generatePatterns(dateTimeMediumPatterns, "DATE_TIME_MEDIUM_PATTERNS");
		generatePatterns(dateTimeShortPatterns, "DATE_TIME_SHORT_PATTERNS");
		generatePatterns(timeFullPatterns, "TIME_FULL_PATTERNS");
		generatePatterns(timeLongPatterns, "TIME_LONG_PATTERNS");
		generatePatterns(timeMediumPatterns, "TIME_MEDIUM_PATTERNS");
		generatePatterns(timeShortPatterns, "TIME_SHORT_PATTERNS");
		return poetBuilder
				.addStaticBlock(staticInitBlock.build())
				.build();
	}

	private void generatePatterns(ListMultimap<String, AptPatternCoordinates> map, String mapName) {
		messager.printMessage(Kind.NOTE, String.format("Generate %s init", mapName));
		map.keySet().forEach(p -> generatePatterns(map, mapName, p));
	}

	private void generatePatterns(ListMultimap<String, AptPatternCoordinates> map, String mapName, String pattern) {
		com.squareup.javapoet.CodeBlock.Builder builder = CodeBlock.builder();
		builder.add("$L.put($S, new PatternCoordinates[] {\n", mapName, pattern);
		map.get(pattern).forEach(pc -> builder.add("of($L, $T.forLanguageTag(\"$L\")),\n", pc.chrono(), Locale.class, pc.languageTag()));
		builder.add("})");
		staticInitBlock.addStatement(builder.build());
	}

	public List<ClassName> getStaticImports() {
		return staticImports;
	}

}
