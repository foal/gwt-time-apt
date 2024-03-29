package org.jresearch.gwt.time.apt;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

public class TerritoryEnumBuilder {
	/**
	 * <pre>
	 * LA,
	 * </pre>
	 */
	private final Builder poetBuilder;

	private TerritoryEnumBuilder(final String enumName) {
		poetBuilder = TypeSpec
			.enumBuilder(enumName)
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
	}

	public static TerritoryEnumBuilder create(final CharSequence enumName) {
		return new TerritoryEnumBuilder(enumName.toString());
	}

	public TerritoryEnumBuilder addEnumConstant(final String territoriType) {
		poetBuilder.addEnumConstant(toJava(territoriType));
		return this;
	}

	public TypeSpec build() {
		return poetBuilder.build();
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

}
