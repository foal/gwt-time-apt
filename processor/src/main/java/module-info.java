import javax.annotation.processing.Processor;

import org.jresearch.gwt.time.apt.CldrSupplementalDataProcessor;

module org.jresearch.gwt.time.apt.processor {
	requires com.google.auto.service;
	requires com.google.common;
	requires com.google.errorprone.annotations;
	requires com.squareup.javapoet;
	requires java.compiler;
	requires java.xml;
	requires java.xml.bind;
	requires jsr305;
	requires one.util.streamex;
	requires org.immutables.value.annotations;
	requires org.jresearch.gwt.time.apt.annotation;

	provides Processor with CldrSupplementalDataProcessor;
}