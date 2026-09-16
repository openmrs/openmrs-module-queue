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
	 * Utility method for parsing a date from a string into a Date
	 *
	 * @param dateVal the date value ot parse
	 * @return the resulting date object
	 * @deprecated as of 3.1.0, as this only accepts yyyy-MM-dd HH:mm:ss in the server's default
	 *             timezone. REST date parameters now use ConversionUtil.convert(String, Date.class).
	 */
	@Deprecated
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
	 *         both are non-null, or null if no entry has both a startedAt and an endedAt
	 */
	public static Double computeAverageWaitTimeInMinutes(List<QueueEntry> queueEntries) {
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
			// Returning 0.0 here would be indistinguishable from a genuine zero-minute wait
			if (numEntries > 0) {
				return totalWaitTime / numEntries;
			}
		}
		return null;
	}
	
	/**
	 * Measures those still waiting, unlike {@link #computeAverageWaitTimeInMinutes(List)}, which
	 * averages waits that have already finished
	 *
	 * @param queueEntries the QueueEntries to check
	 * @param asOf the point in time to measure the open durations against
	 * @return the average duration, in minutes, between startedAt and asOf for entries that have a
	 *         startedAt and no endedAt, or null if there are no such entries
	 */
	public static Double computeAverageOpenWaitTimeInMinutes(List<QueueEntry> queueEntries, Date asOf) {
		if (queueEntries != null) {
			double totalWaitTime = 0.0;
			int numEntries = 0;
			for (QueueEntry e : queueEntries) {
				Long waitTime = computeOpenWaitTimeInMinutes(e, asOf);
				if (waitTime != null) {
					totalWaitTime += waitTime;
					numEntries++;
				}
			}
			// Returning 0.0 here would be indistinguishable from a genuine zero-minute wait
			if (numEntries > 0) {
				return totalWaitTime / numEntries;
			}
		}
		return null;
	}
	
	/**
	 * @param queueEntry the QueueEntry to measure
	 * @param asOf the point in time to measure the open duration against
	 * @return how long the entry has been waiting, in minutes, never negative, or null if it has no
	 *         startedAt or has already ended
	 */
	public static Long computeOpenWaitTimeInMinutes(QueueEntry queueEntry, Date asOf) {
		if (queueEntry == null || asOf == null || queueEntry.getStartedAt() == null || queueEntry.getEndedAt() != null) {
			return null;
		}
		// Measured between instants rather than between local date times, so that a wait spanning a
		// daylight saving change reports the time that actually elapsed. Floored at zero, so that an
		// entry with a startedAt in the future reports as not yet waiting rather than as negative
		return Math.max(0, Duration.between(queueEntry.getStartedAt().toInstant(), asOf.toInstant()).toMinutes());
	}
	
	/**
	 * @param queueEntries the QueueEntries to check
	 * @return the entry that has a startedAt, has no endedAt, and started earliest, or null if there is
	 *         no such entry
	 */
	public static QueueEntry findLongestOpenWait(List<QueueEntry> queueEntries) {
		QueueEntry longestWaiting = null;
		if (queueEntries != null) {
			for (QueueEntry e : queueEntries) {
				if (e.getStartedAt() != null && e.getEndedAt() == null
				        && (longestWaiting == null || e.getStartedAt().before(longestWaiting.getStartedAt()))) {
					longestWaiting = e;
				}
			}
		}
		return longestWaiting;
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
