package org.jresearch.gwt.time.apt;

import java.util.Locale;

import javax.lang.model.element.Modifier;

import org.jresearch.gwt.time.apt.cldr.ldml.Identity;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

/**
 * <pre>
 * public class LocaleInfo {
 *
 * 	public static final Locale AF = new Locale("af", "", "");
 *
 * 	public static final Locale[] LOCALES = new Locale[] { AF };
 *
 * }
 * </pre>
 */
@SuppressWarnings("nls")
public class LocaleInfoClassBuilder {

	private final Builder poetBuilder;
	private com.squareup.javapoet.FieldSpec.Builder localeArray;
	private com.squareup.javapoet.CodeBlock.Builder initializer;

	private LocaleInfoClassBuilder(final CharSequence packageName, final CharSequence className) {
		ArrayTypeName array = ArrayTypeName.of(Locale.class);
		localeArray = FieldSpec.builder(array, "LOCALES", Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
		initializer = CodeBlock.builder().add("new $T[] {", Locale.class);
		poetBuilder = TypeSpec
				.classBuilder(ClassName.get(packageName.toString(), className.toString()))
				.addModifiers(Modifier.PUBLIC);
	}

	public static LocaleInfoClassBuilder create(final CharSequence packageName, final CharSequence className) {
		return new LocaleInfoClassBuilder(packageName, className);
	}

	public LocaleInfoClassBuilder addLocale(final Identity identity) {
		return Ldmls.getIdentityInfo(identity)
				.map(this::addLocale)
				.orElse(this);
	}

	public LocaleInfoClassBuilder addLocale(final IdentityInfo info) {
			// add public static final Locale AF = new Locale("af", "", "");
		String fieldName = Ldmls.createName(info);
			FieldSpec locale = FieldSpec.builder(Locale.class, fieldName, Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
				.initializer("new $T($S, $S, $S)", Locale.class, info.language(), info.territory(), info.script())
					.build();
			poetBuilder.addField(locale);
			return addLocaleInt(fieldName);
	}

	private LocaleInfoClassBuilder addLocaleInt(final String localeField) {
		initializer.add("$L,", localeField);
		return this;
	}

	public TypeSpec build() {
		return poetBuilder
				.addField(localeArray
						.initializer(initializer
								.add("}")
								.build())
						.build())
				.build();
	}

}
