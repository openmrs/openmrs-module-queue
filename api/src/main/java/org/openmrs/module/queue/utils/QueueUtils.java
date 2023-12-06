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
	
	/**
	 * @param startDate1, endDate1 - the start and end date of one timeframe
	 * @param startDate2, endDate2 - the start and end date of second timeframe
	 * @return boolean - indicating whether the timeframes overlap
	 */
	public static boolean datesOverlap(Date startDate1, Date endDate1, Date startDate2, Date endDate2) {
		long startTime1 = (startDate1 == null ? Long.MIN_VALUE : startDate1.getTime());
		long endTime1 = (endDate1 == null ? Long.MAX_VALUE : endDate1.getTime());
		long startTime2 = (startDate2 == null ? Long.MIN_VALUE : startDate2.getTime());
		long endTime2 = (endDate2 == null ? Long.MAX_VALUE : endDate2.getTime());
		// If time1 starts earlier, then it overlaps time2 if it ends after time2 starts
		if (startTime1 < startTime2) {
			return endTime1 > startTime2;
		}
		// Otherwise, if time2 starts earlier, then it overlaps time1 if it ends after time1 starts
		else if (startTime2 < startTime1) {
			return endTime2 > startTime1;
		}
		// Otherwise, if both start at the same time, they overlap
		else {
			return true;
		}
	}
}
