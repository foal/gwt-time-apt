package org.jresearch.gwt.time.apt.data.client;

import java.util.Locale;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;
import org.immutables.value.Value.Style;
import org.jresearch.gwt.time.apt.base.Chrono;

@Immutable
//Due GWT disable annotation auto-discover (see https://github.com/immutables/immutables/issues/740)
@Style(allowedClasspathAnnotations = Override.class)
public interface PatternCoordinates {

	@Parameter
	Chrono chrono();

	@Parameter
	Locale locale();

}
