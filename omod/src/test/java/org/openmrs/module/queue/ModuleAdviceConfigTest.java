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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;

import org.aopalliance.aop.Advice;
import org.junit.Test;
import org.springframework.aop.Advisor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The {@code <advice>} elements in config.xml are what registers this module's AOP advice in a
 * deployed server, and this test is the build's only check on them: module tests register advice
 * themselves rather than going through config.xml.
 * <p>
 * Two of the three things checked here fail quietly in a server. A {@code <class>} that will not
 * load leaves {@code ModuleFactory.loadAdvice} logging a warning, and one that will not instantiate
 * leaves {@code AdvicePoint.getClassInstance} returning null and {@code loadAdvice} logging at
 * debug; either way the module starts with the cascade simply absent. The type check is the
 * opposite: {@code loadAdvice} casts to {@code Advice} without testing, catching only
 * {@code ClassNotFoundException} and {@code NoClassDefFoundError}, and its caller in
 * {@code ModuleUtil.refreshApplicationContext} has no catch at all, so a wrong type aborts the
 * post-refresh loop for every started module rather than just this one.
 */
public class ModuleAdviceConfigTest {
	
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
				        adviceClassName + " needs a public no-arg constructor for AdvicePoint to instantiate it", e);
			}
			assertTrue(
			    adviceClassName + " must implement " + Advice.class.getName() + " or " + Advisor.class.getName()
			            + " to be registered on " + point,
			    Advice.class.isAssignableFrom(adviceClass) || Advisor.class.isAssignableFrom(adviceClass));
		}
	}
	
	/**
	 * Reads this module's own config.xml from the source tree. Reading it off the classpath instead
	 * would mean parsing every {@code config.xml} there to find this one, and a required module ships
	 * one: a malformed or entity-bearing document of somebody else's then decides whether this test
	 * passes. Nothing in the {@code <advice>} elements is Maven-filtered, so the source copy and the
	 * packaged copy say the same thing.
	 */
	private Document queueConfigXml() throws Exception {
		// surefire runs with the module directory as its working directory
		File configXml = new File("src/main/resources/config.xml");
		assertTrue("expected to find " + configXml.getAbsolutePath(), configXml.isFile());
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setExpandEntityReferences(false);
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(configXml);
	}
	
	private static String childText(Element parent, String tagName) {
		NodeList children = parent.getElementsByTagName(tagName);
		return children.getLength() == 0 ? null : children.item(0).getTextContent().trim();
	}
}
