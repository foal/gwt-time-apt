package org.jresearch.gwt.time.apt.base;

public class LocationData {

	private final String language;
	private final String region;
	private final String script;
	private final String variant;

	public static LocationData of(String language, String region, String script, String variant) {
		return new LocationData(language, region, script, variant);
	}

	private LocationData(String language, String region, String script, String variant) {
		this.language = language;
		this.region = region;
		this.script = script;
		this.variant = variant;
	}

	public String getLanguage() {
		return language;
	}

	public String getRegion() {
		return region;
	}

	public String getScript() {
		return script;
	}

	public String getVariant() {
		return variant;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((language == null) ? 0 : language.hashCode());
		result = prime * result + ((region == null) ? 0 : region.hashCode());
		result = prime * result + ((script == null) ? 0 : script.hashCode());
		result = prime * result + ((variant == null) ? 0 : variant.hashCode());
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
		LocationData other = (LocationData) obj;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		if (region == null) {
			if (other.region != null)
				return false;
		} else if (!region.equals(other.region))
			return false;
		if (script == null) {
			if (other.script != null)
				return false;
		} else if (!script.equals(other.script))
			return false;
		if (variant == null) {
			if (other.variant != null)
				return false;
		} else if (!variant.equals(other.variant))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "LocationData [language=" + language + ", region=" + region + ", script=" + script + ", variant=" + variant + "]";
	}

}
