package org.jresearch.gwt.time.apt;

import java.util.Locale;

import javax.lang.model.element.Modifier;

import org.jresearch.gwt.time.apt.cldr.ldml.Identity;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

/**
 * <pre>
 * public class LocaleInfo {
 *
 * 	public static final Locale AF = LocaleRegistry.register("af", "", "", "");
 *
 * }
 * </pre>
 */
@SuppressWarnings("nls")
public class LocaleInfoClassBuilder {

	private final Builder poetBuilder;
	private ClassName localeUtil = ClassName.get("org.jresearch.gwt.locale.client.locale", "LocaleRegistry");

	private LocaleInfoClassBuilder(final CharSequence packageName, final CharSequence className) {
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
		// add public static final Locale AF = LocaleRegistry.register("af", "", "", "");
		String fieldName = Ldmls.createFieldName(info);
		FieldSpec locale = FieldSpec.builder(Locale.class, fieldName, Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
				.initializer("$T.register($S, $S, $S, $S)", localeUtil, info.language(), info.territory(), info.script(), info.variant())
				.build();
		poetBuilder.addField(locale);
		return this;
	}

	public TypeSpec build() {
		return poetBuilder.build();
	}
}
