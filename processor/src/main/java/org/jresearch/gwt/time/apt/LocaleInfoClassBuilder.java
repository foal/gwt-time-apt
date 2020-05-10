package org.jresearch.gwt.time.apt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

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

	public LocaleInfoClassBuilder addLocale(final TerritoryLangInfo info) {
		int langEnd = info.language().indexOf('_');
		if (langEnd == -1) {
			return addLocaleInt(info.territory(), info.language());
		}
		return addLocaleInt(info.territory(), info.language().substring(0, langEnd), info.language().substring(langEnd + 1));
	}

	private LocaleInfoClassBuilder addLocaleInt(final String territory, final String language) {
		staticInitBlock.addStatement("LOCALES.add(new $T($S, $S))", Locale.class, territory, language);
		return this;
	}

	private LocaleInfoClassBuilder addLocaleInt(final String territory, final String language, final String script) {
		staticInitBlock.addStatement("LOCALES.add(new $T($S, $S, $S))", Locale.class, territory, language, script);
		return this;
	}

	public TypeSpec build() {
		return poetBuilder
				.addStaticBlock(staticInitBlock.build())
				.build();
	}

}
