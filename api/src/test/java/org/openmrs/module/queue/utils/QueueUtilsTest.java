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

import java.util.Date;

import org.junit.Test;

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
}
