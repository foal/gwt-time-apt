package org.jresearch.gwt.time.apt.processor;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.jresearch.gwt.time.apt.cldr.ldml.Ldml;
import org.junit.jupiter.api.Test;

class XmlReadTest {

	@SuppressWarnings("static-method")
	@Test
	void test() throws JAXBException {
		System.setProperty("javax.xml.accessExternalDTD", "all");
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		URL data = contextClassLoader.getResource("cldr/common/main/zu_ZA.xml");
		JAXBContext context = JAXBContext.newInstance(Ldml.class);
		Ldml ldml = (Ldml) context.createUnmarshaller().unmarshal(data);
		assertNotNull(ldml);
	}

}
