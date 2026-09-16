/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The {@code <advice>} elements in config.xml are the only thing that registers this module's AOP
 * advice in a deployed server, and module tests register advice themselves, so nothing else in the
 * build reads them. Getting one wrong fails quietly rather than loudly:
 * {@code AdvicePoint.getClassInstance} catches the reflection failure and logs a warning,
 * {@code ModuleFactory.loadAdvice} then logs at debug and carries on, and the module starts with
 * the cascade simply absent.
 */
public class ModuleAdviceConfigTest {
	
	private static final String MODULE_PACKAGE = "org.openmrs.module.queue";
	
	@Test
	public void everyAdvicePointAndClassInConfigXmlShouldBeUsable() throws Exception {
		NodeList adviceElements = queueConfigXml().getElementsByTagName("advice");
		assertTrue("config.xml should declare at least one advice", adviceElements.getLength() > 0);
		
		for (int i = 0; i < adviceElements.getLength(); i++) {
			Element advice = (Element) adviceElements.item(i);
			String point = childText(advice, "point");
			String adviceClassName = childText(advice, "class");
			assertNotNull("advice needs a point", point);
			assertNotNull("advice needs a class", adviceClassName);
			
			// this mirrors what ModuleFactory.loadAdvice and AdvicePoint.getClassInstance do: load the
			// point, load the advice class, call its public no-arg constructor, cast the result to Advice
			Class.forName(point);
			Class<?> adviceClass = Class.forName(adviceClassName);
			assertTrue(adviceClassName + " must implement " + Advice.class.getName() + " to be registered on " + point,
			    Advice.class.isAssignableFrom(adviceClass));
			assertNotNull(adviceClass.getConstructor().newInstance());
		}
	}
	
	/**
	 * Reads this module's packaged config.xml, picked out by its {@code <package>} rather than by
	 * taking the first {@code config.xml} on the classpath, because required modules ship one too.
	 */
	private Document queueConfigXml() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		DocumentBuilder builder = factory.newDocumentBuilder();
		
		List<Document> ours = new ArrayList<>();
		for (URL url : Collections.list(getClass().getClassLoader().getResources("config.xml"))) {
			try (InputStream in = url.openStream()) {
				Document document = builder.parse(in);
				if (MODULE_PACKAGE.equals(childText(document.getDocumentElement(), "package"))) {
					ours.add(document);
				}
			}
		}
		assertEquals("expected exactly one config.xml declaring package " + MODULE_PACKAGE, 1, ours.size());
		return ours.get(0);
	}
	
	private static String childText(Element parent, String tagName) {
		NodeList children = parent.getElementsByTagName(tagName);
		return children.getLength() == 0 ? null : children.item(0).getTextContent().trim();
	}
}
