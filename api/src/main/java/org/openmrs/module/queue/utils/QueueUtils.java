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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.module.queue.model.QueueEntry;

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
	
	/**
	 * @param date the date to convert
	 * @return a LocalDateTime representation of the given date at the system timezone
	 */
	public static LocalDateTime convertToLocalDateTimeInSystemDefaultTimezone(Date date) {
		if (date == null) {
			return null;
		}
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}
	
	/**
	 * @param queueEntries the QueueEntries to check
	 * @return the average duration for the entries, in minutes, between startedAt and endedAt, where
	 *         both are non-null
	 */
	public static double computeAverageWaitTimeInMinutes(List<QueueEntry> queueEntries) {
		double averageWaitTime = 0.0;
		if (queueEntries != null && !queueEntries.isEmpty()) {
			double totalWaitTime = 0.0;
			int numEntries = 0;
			for (QueueEntry e : queueEntries) {
				LocalDateTime startedAt = convertToLocalDateTimeInSystemDefaultTimezone(e.getStartedAt());
				LocalDateTime endedAt = convertToLocalDateTimeInSystemDefaultTimezone(e.getEndedAt());
				if (startedAt != null && endedAt != null) {
					totalWaitTime += Duration.between(startedAt, endedAt).toMinutes();
					numEntries++;
				}
			}
			averageWaitTime = totalWaitTime / numEntries;
		}
		return averageWaitTime;
	}
}
