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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;
import org.openmrs.module.queue.model.QueueEntry;

public class QueueUtilsTest {
	
	private static final Date NULL = null;
	
	private static final Date AUG_1 = QueueUtils.parseDate("2023-08-01 10:00:00");
	
	private static final Date AUG_2 = QueueUtils.parseDate("2023-08-02 10:00:00");
	
	private static final Date AUG_3 = QueueUtils.parseDate("2023-08-03 10:00:00");
	
	private static final Date AUG_4 = QueueUtils.parseDate("2023-08-04 10:00:00");
	
	@Test
	public void shouldReturnTrueIfDatesOverlap() {
		// Test that nulls are handled as open-ended dates
		assertThat(QueueUtils.datesOverlap(NULL, NULL, NULL, NULL), is(true));
		assertThat(QueueUtils.datesOverlap(NULL, AUG_2, AUG_3, AUG_4), is(false));
		assertThat(QueueUtils.datesOverlap(AUG_1, NULL, AUG_3, AUG_4), is(true));
		assertThat(QueueUtils.datesOverlap(AUG_1, AUG_2, NULL, AUG_4), is(true));
		assertThat(QueueUtils.datesOverlap(AUG_1, AUG_2, AUG_3, NULL), is(false));
		
		// Test that order of date periods does not matter
		assertThat(QueueUtils.datesOverlap(AUG_1, AUG_2, AUG_3, AUG_4), is(false));
		assertThat(QueueUtils.datesOverlap(AUG_1, AUG_3, AUG_2, AUG_4), is(true));
		assertThat(QueueUtils.datesOverlap(AUG_3, AUG_4, AUG_1, AUG_2), is(false));
		assertThat(QueueUtils.datesOverlap(AUG_2, AUG_4, AUG_1, AUG_3), is(true));
		
		// Test date overlaps without nulls
		assertThat(QueueUtils.datesOverlap(AUG_1, AUG_2, AUG_3, AUG_4), is(false)); // one before two
		assertThat(QueueUtils.datesOverlap(AUG_1, AUG_2, AUG_2, AUG_3), is(false)); // one ends when two begins
		assertThat(QueueUtils.datesOverlap(AUG_1, AUG_3, AUG_2, AUG_4), is(true)); // one ends within two
		assertThat(QueueUtils.datesOverlap(AUG_1, AUG_4, AUG_2, AUG_3), is(true)); // one ends after two ends
		assertThat(QueueUtils.datesOverlap(AUG_2, AUG_4, AUG_1, AUG_3), is(true)); // one starts within two
		assertThat(QueueUtils.datesOverlap(AUG_3, AUG_4, AUG_1, AUG_2), is(false)); // one after two
		assertThat(QueueUtils.datesOverlap(AUG_1, AUG_2, AUG_1, AUG_3), is(true)); // one starts when two starts
	}
	
	@Test
	public void shouldComputeAverageWaitTimeInMinutes() {
		// Entries that waited 24 and 48 hours average out to 36 hours
		assertThat(QueueUtils.computeAverageWaitTimeInMinutes(entries(entry(AUG_1, AUG_2), entry(AUG_1, AUG_3))),
		    is(2160.0));
		
		// Entries missing a start or an end are not part of the average
		assertThat(QueueUtils.computeAverageWaitTimeInMinutes(entries(entry(AUG_1, AUG_2), entry(AUG_1, NULL))), is(1440.0));
		assertThat(QueueUtils.computeAverageWaitTimeInMinutes(entries(entry(NULL, AUG_2), entry(AUG_1, AUG_2))), is(1440.0));
		assertThat(QueueUtils.computeAverageWaitTimeInMinutes(entries(entry(NULL, NULL), entry(AUG_1, AUG_2))), is(1440.0));
		
		// Test that there is no average to report rather than dividing by zero and returning NaN
		assertThat(QueueUtils.computeAverageWaitTimeInMinutes(entries(entry(AUG_1, NULL), entry(AUG_2, NULL))),
		    is(nullValue()));
		assertThat(QueueUtils.computeAverageWaitTimeInMinutes(entries(entry(NULL, NULL), entry(NULL, NULL))),
		    is(nullValue()));
		assertThat(QueueUtils.computeAverageWaitTimeInMinutes(entries()), is(nullValue()));
		assertThat(QueueUtils.computeAverageWaitTimeInMinutes(null), is(nullValue()));
	}
	
	@Test
	public void shouldComputeAverageOpenWaitTimeInMinutes() {
		// Measured against the given instant rather than the end of the entry, so entries that have
		// not ended are exactly the ones that count. Waits of 48 and 24 hours average out to 36.
		assertThat(QueueUtils.computeAverageOpenWaitTimeInMinutes(entries(entry(AUG_1, NULL), entry(AUG_2, NULL)), AUG_3),
		    is(2160.0));
		
		// An entry that has already ended is not still waiting, whatever its duration was
		assertThat(QueueUtils.computeAverageOpenWaitTimeInMinutes(entries(entry(AUG_1, NULL), entry(AUG_1, AUG_2)), AUG_2),
		    is(1440.0));
		
		// Entries without a start cannot be measured
		assertThat(QueueUtils.computeAverageOpenWaitTimeInMinutes(entries(entry(NULL, NULL), entry(AUG_1, NULL)), AUG_2),
		    is(1440.0));
		
		// Test that there is no average to report rather than dividing by zero and returning NaN
		assertThat(QueueUtils.computeAverageOpenWaitTimeInMinutes(entries(entry(AUG_1, AUG_2)), AUG_3), is(nullValue()));
		assertThat(QueueUtils.computeAverageOpenWaitTimeInMinutes(entries(), AUG_3), is(nullValue()));
		assertThat(QueueUtils.computeAverageOpenWaitTimeInMinutes(null, AUG_3), is(nullValue()));
		assertThat(QueueUtils.computeAverageOpenWaitTimeInMinutes(entries(entry(AUG_1, NULL)), NULL), is(nullValue()));
	}
	
	@Test
	public void shouldFindLongestOpenWait() {
		QueueEntry waitingSinceAug1 = entry(AUG_1, NULL);
		QueueEntry waitingSinceAug2 = entry(AUG_2, NULL);
		QueueEntry endedAfterTwoDays = entry(AUG_1, AUG_3);
		
		// The entry that has been waiting longest is the one that started earliest
		assertThat(QueueUtils.findLongestOpenWait(entries(waitingSinceAug2, waitingSinceAug1)), is(waitingSinceAug1));
		
		// An entry that has ended is not waiting at all, however long it ran for
		assertThat(QueueUtils.findLongestOpenWait(entries(endedAfterTwoDays, waitingSinceAug2)), is(waitingSinceAug2));
		
		assertThat(QueueUtils.findLongestOpenWait(entries(endedAfterTwoDays)), is(nullValue()));
		assertThat(QueueUtils.findLongestOpenWait(entries(entry(NULL, NULL))), is(nullValue()));
		assertThat(QueueUtils.findLongestOpenWait(entries()), is(nullValue()));
		assertThat(QueueUtils.findLongestOpenWait(null), is(nullValue()));
	}
	
	@Test
	public void shouldComputeOpenWaitTimeInMinutes() {
		assertThat(QueueUtils.computeOpenWaitTimeInMinutes(entry(AUG_1, NULL), AUG_2), is(1440L));
		assertThat(QueueUtils.computeOpenWaitTimeInMinutes(entry(AUG_1, AUG_2), AUG_3), is(nullValue()));
		assertThat(QueueUtils.computeOpenWaitTimeInMinutes(entry(NULL, NULL), AUG_2), is(nullValue()));
		assertThat(QueueUtils.computeOpenWaitTimeInMinutes(null, AUG_2), is(nullValue()));
		assertThat(QueueUtils.computeOpenWaitTimeInMinutes(entry(AUG_1, NULL), NULL), is(nullValue()));
		// An entry whose startedAt is in the future has not started waiting yet
		assertThat(QueueUtils.computeOpenWaitTimeInMinutes(entry(AUG_2, NULL), AUG_1), is(0L));
	}
	
	@Test
	public void shouldMeasureAnOpenWaitAcrossADaylightSavingChange() {
		TimeZone originalTimeZone = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));
			// British clocks go forward an hour at 01:00 UTC on this date, so an hour of waiting looks
			// like two if it is measured between local date times rather than between instants
			Date startedAt = Date.from(Instant.parse("2023-03-26T00:30:00Z"));
			Date asOf = Date.from(Instant.parse("2023-03-26T01:30:00Z"));
			
			assertThat(QueueUtils.computeOpenWaitTimeInMinutes(entry(startedAt, NULL), asOf), is(60L));
		}
		finally {
			TimeZone.setDefault(originalTimeZone);
		}
	}
	
	private List<QueueEntry> entries(QueueEntry... queueEntries) {
		return Arrays.asList(queueEntries);
	}
	
	private QueueEntry entry(Date startedAt, Date endedAt) {
		QueueEntry queueEntry = new QueueEntry();
		queueEntry.setStartedAt(startedAt);
		queueEntry.setEndedAt(endedAt);
		return queueEntry;
	}
}
