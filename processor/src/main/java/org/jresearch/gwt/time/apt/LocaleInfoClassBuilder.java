package org.jresearch.gwt.time.apt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.lang.model.element.Modifier;

import org.jresearch.gwt.time.apt.cldr.ldml.Identity;
import org.jresearch.gwt.time.apt.cldr.ldml.Language;
import org.jresearch.gwt.time.apt.cldr.ldml.Script;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

import one.util.streamex.StreamEx;

/**
 * <pre>
 * public class LocaleInfo {
 *
 * 	private static final List<Locale> LOCALES = new ArrayList<>();
 *
 * 	static {
 * 		LOCALES.add(new Locale("fa", "AF"));
 * 	}
 *
 * 	public static List<Locale> getAvailable() {
 * 		return Collections.unmodifiableList(LOCALES);
 * 	}
 *
 * }
 * </pre>
 */
@SuppressWarnings("nls")
public class LocaleInfoClassBuilder {

	private final Builder poetBuilder;
	private final com.squareup.javapoet.CodeBlock.Builder staticInitBlock;

	private LocaleInfoClassBuilder(final CharSequence packageName, final CharSequence className) {
		ParameterizedTypeName list = ParameterizedTypeName.get(List.class, Locale.class);
		staticInitBlock = CodeBlock.builder();
		FieldSpec localeList = FieldSpec.builder(list, "LOCALES", Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
				.initializer("new $T<>()", ArrayList.class)
				.build();
		MethodSpec getAvailable = MethodSpec.methodBuilder("getAvailable")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(list)
				.addStatement("return $T.unmodifiableList(LOCALES)", Collections.class)
				.build();
		poetBuilder = TypeSpec
				.classBuilder(ClassName.get(packageName.toString(), className.toString()))
				.addModifiers(Modifier.PUBLIC)
				.addField(localeList)
				.addMethod(getAvailable);
	}

	public static LocaleInfoClassBuilder create(final CharSequence packageName, final CharSequence className) {
		return new LocaleInfoClassBuilder(packageName, className);
	}

	public LocaleInfoClassBuilder addLocale(final Identity info) {
		Optional<String> language = get(info, Language.class).map(Language::getType);
		if (language.isPresent()) {
			String territory = get(info, org.jresearch.gwt.time.apt.cldr.ldml.Territory.class).map(org.jresearch.gwt.time.apt.cldr.ldml.Territory::getType).orElse("");
			String script = get(info, Script.class).map(Script::getType).orElse("");
			return addLocaleInt(language.get(), territory, script);
		}
		return this;
	}

	private static <T> Optional<T> get(Identity identity, Class<T> propertyType) {
		return StreamEx.of(identity.getAliasOrVersionOrGenerationOrLanguageOrScriptOrTerritoryOrVariantOrSpecial())
				.findAny(o -> propertyType.isAssignableFrom(o.getClass()))
				.map(propertyType::cast);
	}

	private LocaleInfoClassBuilder addLocaleInt(final String language, final String territory, final String script) {
		staticInitBlock.addStatement("LOCALES.add(new $T($S, $S, $S))", Locale.class, language, territory, script);
		return this;
	}

	public TypeSpec build() {
		return poetBuilder
				.addStaticBlock(staticInitBlock.build())
				.build();
	}

}
