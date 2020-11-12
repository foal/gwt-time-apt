package org.jresearch.gwt.time.apt;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;
import org.jresearch.gwt.time.apt.base.Chrono;

@Immutable
public interface AptPatternCoordinates {
	@Parameter
	Chrono chrono();

	@Parameter
	String languageTag();
}
