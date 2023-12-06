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

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

public class QueueUtilsTest {
	
	@Test
	public void shouldReturnFalseWhenDatesAreNull() {
		assertThat(QueueUtils.datesOverlap(null, null, null, null), is(false));
	}
	
	@Test
	public void shouldReturnFalseWhenSecondTimeIntervalIsBeforeFirstTimeInterval() {
		// time interval startDate2-endDate2(t-12hr, t-10hr) is to the left of time interval startDate1-endDate1
		Calendar calendar = Calendar.getInstance();
		Date startDate1 = calendar.getTime();
		calendar.add(Calendar.HOUR, -12);
		Date startDate2 = calendar.getTime();
		calendar.add(Calendar.HOUR, 2);
		Date endDate2 = calendar.getTime();
		assertThat(QueueUtils.datesOverlap(startDate1, null, startDate2, endDate2), is(false));
	}
	
	@Test
	public void shouldReturnFalseWhenSecondTimeIntervalEndsWhenFirstTimeIntervalStarts() {
		// time interval startDate2-endDate2(t-12hr, t) ends when time interval startDate1-endDate1(t) starts
		Calendar calendar = Calendar.getInstance();
		Date startDate1 = calendar.getTime();
		Date endDate2 = calendar.getTime();
		calendar.add(Calendar.HOUR, -12);
		Date startDate2 = calendar.getTime();
		assertThat(QueueUtils.datesOverlap(startDate1, null, startDate2, endDate2), is(false));
	}
	
	@Test
	public void shouldReturnTrueWhenDatesOverlap() {
		Calendar calendar = Calendar.getInstance();
		Date startDate1 = calendar.getTime();
		calendar.add(Calendar.HOUR, 1);
		Date startDate2 = calendar.getTime();
		assertThat(QueueUtils.datesOverlap(startDate1, null, startDate2, null), is(true));
	}
	
	@Test
	public void shouldReturnTrueWhenDateIntervalsAreBoundedAndInclusive() {
		// Dt1 = t,         t+12
		// Dt2 =   t+1,t+8
		Calendar calendar = Calendar.getInstance();
		Date startDate1 = calendar.getTime();
		calendar.add(Calendar.HOUR, 1);
		Date startDate2 = calendar.getTime();
		calendar.add(Calendar.HOUR, 7);
		Date endDate2 = calendar.getTime();
		calendar.add(Calendar.HOUR, 5);
		Date endDate1 = calendar.getTime();
		assertThat(QueueUtils.datesOverlap(startDate1, endDate1, startDate2, endDate2), is(true));
	}
	
	@Test
	public void shouldReturnTrueWhenDateIntervalsAreBoundedAndOverlap() {
		// DT1 = t,      t+4
		// DT2 =   t+1,       t+8
		Calendar calendar = Calendar.getInstance();
		Date startDate1 = calendar.getTime();
		calendar.add(Calendar.HOUR, 1);
		Date startDate2 = calendar.getTime();
		calendar.add(Calendar.HOUR, 3);
		Date endDate1 = calendar.getTime();
		calendar.add(Calendar.HOUR, 4);
		Date endDate2 = calendar.getTime();
		assertThat(QueueUtils.datesOverlap(startDate1, endDate1, startDate2, endDate2), is(true));
	}
	
	@Test
	public void shouldReturnFalseWhenSecondTimeIntervalStartsAfterFirstTimeIntervalEndsl() {
		// time interval startDate2-endDate2(t) is to the right of time interval startDate1-endDate1(t-12hr, t-10hr)
		Calendar calendar = Calendar.getInstance();
		Date startDate2 = calendar.getTime();
		calendar.add(Calendar.HOUR, -10);
		Date endDate1 = calendar.getTime();
		calendar.add(Calendar.HOUR, -2);
		Date startDate1 = calendar.getTime();
		assertThat(QueueUtils.datesOverlap(startDate1, endDate1, startDate2, null), is(false));
	}
	
	@Test
	public void shouldReturnFalseWhenSecondTimeIntervalStartsWhenFirstTimeIntervalEndsl() {
		// time interval startDate2(t) starts when first time interval startDate1-endDate1(t-12hr, t) ends
		// DT1 = t-12, t
		// DT2 =       t
		Calendar calendar = Calendar.getInstance();
		Date startDate2 = calendar.getTime();
		Date endDate1 = calendar.getTime();
		calendar.add(Calendar.HOUR, -12);
		Date startDate1 = calendar.getTime();
		assertThat(QueueUtils.datesOverlap(startDate1, endDate1, startDate2, null), is(false));
	}
}
