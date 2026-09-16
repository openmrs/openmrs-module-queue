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
import org.springframework.aop.Advisor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The {@code <advice>} elements in config.xml are what registers this module's AOP advice in a
 * deployed server, and this test is the build's only check on them: module tests register advice
 * themselves rather than going through config.xml. Getting one wrong fails quietly rather than
 * loudly: {@code AdvicePoint.getClassInstance} catches the reflection failure and logs a warning,
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
			
			// AdvicePoint.getClassInstance loads the class and calls its public no-arg constructor, and
			// ModuleFactory.loadAdvice then registers the instance as an Advisor if it is one and as an
			// Advice otherwise, so either type is legal here. The constructor is looked up rather than
			// called, so an advice that legitimately touches Context on construction is not failed for it.
			Class.forName(point);
			Class<?> adviceClass = Class.forName(adviceClassName);
			try {
				adviceClass.getConstructor();
			}
			catch (NoSuchMethodException e) {
				throw new AssertionError(
				        adviceClassName + " needs a public no-arg constructor for AdvicePoint to" + " instantiate it", e);
			}
			assertTrue(
			    adviceClassName + " must implement " + Advice.class.getName() + " or " + Advisor.class.getName()
			            + " to be registered on " + point,
			    Advice.class.isAssignableFrom(adviceClass) || Advisor.class.isAssignableFrom(adviceClass));
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
