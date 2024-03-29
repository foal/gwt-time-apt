package org.jresearch.gwt.time.apt;

import java.time.DayOfWeek;
//import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

import one.util.streamex.StreamEx;

/**
 * <pre>
 * public class WeekInfo {
 *
 * 	public static final Integer DEFAULT_MIN_DAYS = Integer.valueOf(1);
 * 	public static final int DEFAULT_FIRST_DAY = MONDAY.getValue();
 *
 * 	public static final Map<Integer, EnumSet<Region>> MIN_DAYS = new HashMap<>();
 * 	public static final Map<Integer, EnumSet<Region>> FIRST_DAY = new HashMap<>();
 *
 * 	static {
 * 		MIN_DAYS.put(Integer.valueOf(4), EnumSet.of(AD, AN, AT, AX, BE, BG, CH, CZ, DE, DK, EE, ES, FI, FJ, FO, FR, GB, GF, GG, GI, GP, GR, HU, IE, IM, IS));
 * 		FIRST_DAY.put(Integer.valueOf(5), EnumSet.of(MV));
 * 		FIRST_DAY.put(Integer.valueOf(6), EnumSet.of(AE, AF, BH));
 * 		FIRST_DAY.put(Integer.valueOf(7), EnumSet.of(AG, AS, AU, BD, BR, BS));
 * 	}
 *
 * }
 * </pre>
 */
public class WeekInfoClassBuilder {

	private final List<ClassName> staticImports;
	private final Builder poetBuilder;
	private final com.squareup.javapoet.CodeBlock.Builder staticInitBlock;

	private WeekInfoClassBuilder(final CharSequence packageName, final CharSequence className) {
		ClassName region = ClassName.get(packageName.toString(), CldrDataProcessor.REGION_ENUM_NAME);
		ParameterizedTypeName setOfEnum = ParameterizedTypeName.get(ClassName.get(EnumSet.class), region);
		ParameterizedTypeName map = ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(Integer.class), setOfEnum);
		staticImports = List.of(region);
		staticInitBlock = CodeBlock.builder();
		FieldSpec minDaysMap = FieldSpec.builder(map, "MIN_DAYS", Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
			.initializer("new $T<>()", HashMap.class)
			.build();
		FieldSpec firstDayMap = FieldSpec.builder(map, "FIRST_DAY", Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
			.initializer("new $T<>()", HashMap.class)
			.build();
		poetBuilder = TypeSpec
			.classBuilder(ClassName.get(packageName.toString(), className.toString()))
			.addModifiers(Modifier.PUBLIC)
			.addField(minDaysMap)
			.addField(firstDayMap);
	}

	public static WeekInfoClassBuilder create(final CharSequence packageName, final CharSequence className) {
		return new WeekInfoClassBuilder(packageName, className);
	}

	@SuppressWarnings("boxing")
	public WeekInfoClassBuilder addDefaultMinDays(final int defaultMinDays) {
		FieldSpec values = FieldSpec.builder(Integer.class, "DEFAULT_MIN_DAYS", Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
			.initializer("$T.valueOf($L)", Integer.class, defaultMinDays)
			.build();

		poetBuilder.addField(values);
		return this;
	}

	@SuppressWarnings("boxing")
	public WeekInfoClassBuilder addDefaultFirstDay(final DayOfWeek defaultFirstDay) {
		FieldSpec values = FieldSpec.builder(Integer.class, "DEFAULT_FIRST_DAY", Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
			.initializer("$T.valueOf($L)", Integer.class, defaultFirstDay.getValue())
			.build();

		poetBuilder.addField(values);
		return this;
	}

	@SuppressWarnings("boxing")
	public WeekInfoClassBuilder addFirstDayEntry(final DayOfWeek firstDay, List<String> territories) {
		staticInitBlock.addStatement("FIRST_DAY.put($T.valueOf($L), EnumSet.of($L))", Integer.class, firstDay.getValue(), createTerritoryBlock(territories));
		return this;
	}

	@SuppressWarnings("boxing")
	public WeekInfoClassBuilder addMinDaysEntry(final int minFays, List<String> territories) {
		staticInitBlock.addStatement("MIN_DAYS.put($T.valueOf($L), EnumSet.of($L))", Integer.class, minFays, createTerritoryBlock(territories));
		return this;
	}

	@SuppressWarnings("resource")
	private static CodeBlock createTerritoryBlock(List<String> territories) {
		return StreamEx.of(territories)
			.map(WeekInfoClassBuilder::toJava)
			.map(CodeBlock::of)
			.toListAndThen(l -> CodeBlock.join(l, ", "));
	}

	public TypeSpec build() {
		return poetBuilder
			.addStaticBlock(staticInitBlock.build())
			.build();
	}

	private static String toJava(final String name) {
		final String identifier = toJavaIdentifier(name);
		return SourceVersion.isKeyword(identifier) ? '_' + identifier : identifier;
	}

	private static String toJavaIdentifier(final String name) {
		final StringBuilder sb = new StringBuilder(toFirstJavaCharacter(name.charAt(0)));
		for (int i = 1; i < name.length(); i++) {
			sb.append(toJavaCharacter(name.charAt(i)));
		}
		return sb.toString();
	}

	private static char toJavaCharacter(final char nameCharacter) {
		return Character.isJavaIdentifierPart(nameCharacter) ? nameCharacter : '_';
	}

	private static String toFirstJavaCharacter(final char firstNameCharacter) {
		return Character.isJavaIdentifierStart(firstNameCharacter) ? String.valueOf(firstNameCharacter) : "_" + toJavaCharacter(firstNameCharacter);
	}

	public List<ClassName> getStaticImports() {
		return staticImports;
	}

}
