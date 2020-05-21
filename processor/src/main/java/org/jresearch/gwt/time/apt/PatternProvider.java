package org.jresearch.gwt.time.apt;

import java.time.format.FormatStyle;
import java.util.Optional;

public interface PatternProvider {

	Optional<String> getPattern(FormatStyle formatStyle);

}
