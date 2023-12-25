package org.jresearch.gwt.time.apt.data.client;

import java.util.Locale;

import org.jresearch.gwt.time.apt.base.Chrono;

public final class PatternCoordinates {

	private final Chrono chrono;
	private final Locale locale;

	private PatternCoordinates(Chrono chrono, Locale locale) {
		this.chrono = chrono;
		this.locale = locale;
	}

	public static PatternCoordinates of(Chrono chrono, String tag) {
		return new PatternCoordinates(chrono, Locale.forLanguageTag(tag));
	}

	public Chrono chrono() {
		return chrono;
	}

	public Locale locale() {
		return locale;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chrono == null) ? 0 : chrono.hashCode());
		result = prime * result + ((locale == null) ? 0 : locale.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PatternCoordinates other = (PatternCoordinates) obj;
		if (chrono != other.chrono)
			return false;
		if (locale == null) {
			if (other.locale != null)
				return false;
		} else if (!locale.equals(other.locale))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PatternCoordinates [chrono=" + chrono + ", locale=" + locale + "]";
	}

}
