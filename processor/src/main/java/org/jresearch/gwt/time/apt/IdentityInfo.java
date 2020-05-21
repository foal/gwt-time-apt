package org.jresearch.gwt.time.apt;

import org.immutables.value.Value.Immutable;

@Immutable
public interface IdentityInfo {

	String language();

	String territory();

	String script();

}
