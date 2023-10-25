/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.tasks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.openmrs.Visit;
import org.openmrs.module.queue.model.QueueEntry;

public class AutoCloseVisitQueueEntryTaskTest {
	
	final List<QueueEntry> queueEntries = new ArrayList<>();
	
	class TestAutoCloseVisitEntryTask extends AutoCloseVisitQueueEntryTask {
		
		@Override
		protected List<QueueEntry> getActiveVisitQueueEntries() {
			return queueEntries.stream().filter(e -> e.getEndedAt() == null).collect(Collectors.toList());
		}
		
		@Override
		protected void saveQueueEntry(QueueEntry queueEntry) {
			// Do nothing
		}
	}
	
	@Test
	public void shouldAutoCloseVisitQueueEntriesIfVisitIsClosed() throws Exception {
		
		Visit visit1 = new Visit();
		visit1.setStartDatetime(getDate("2020-01-01 10:00"));
		QueueEntry queueEntry1 = new QueueEntry();
		queueEntry1.setStartedAt(getDate("2020-01-01 10:10"));
		queueEntry1.setVisit(visit1);
		queueEntries.add(queueEntry1);
		
		Visit visit2 = new Visit();
		visit2.setStartDatetime(getDate("2021-01-01 10:00"));
		QueueEntry queueEntry2 = new QueueEntry();
		queueEntry2.setStartedAt(getDate("2021-01-01 10:20"));
		queueEntry2.setVisit(visit2);
		queueEntries.add(queueEntry2);
		
		TestAutoCloseVisitEntryTask task = new TestAutoCloseVisitEntryTask();
		task.run();
		assertThat(queueEntry1.getEndedAt(), nullValue());
		assertThat(queueEntry2.getEndedAt(), nullValue());
		
		visit1.setStopDatetime(getDate("2020-01-01 23:15"));
		task.run();
		assertThat(queueEntry1.getEndedAt(), equalTo(visit1.getStopDatetime()));
		assertThat(queueEntry2.getEndedAt(), nullValue());
		
		visit2.setStopDatetime(getDate("2021-01-05 11:30"));
		task.run();
		assertThat(queueEntry1.getEndedAt(), equalTo(visit1.getStopDatetime()));
		assertThat(queueEntry2.getEndedAt(), equalTo(visit2.getStopDatetime()));
	}
	
	Date getDate(String dateStr) throws Exception {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return df.parse(dateStr);
	}
}
