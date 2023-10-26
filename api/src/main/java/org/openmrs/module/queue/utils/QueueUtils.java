/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for static methods useful within the Queue module
 */
@Slf4j
public class QueueUtils {
	
	/**
	 * Utility method for parsing a date from a string into a Date. TODO: This will need review and
	 * testing related to handling of timezones and other date formats
	 * 
	 * @param dateVal the date value ot parse
	 * @return the resulting date object
	 */
	public static Date parseDate(String dateVal) {
		try {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateVal);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
