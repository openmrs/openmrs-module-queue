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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openmrs.module.queue.QueueModuleConstants.AUTO_CLOSE_QUEUE_ENTRIES_FOR_QUEUES;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.APIException;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;

public class AutoCloseQueueEntryTaskTest {
	
	final List<QueueEntry> queueEntries = new ArrayList<>();
	
	private String configuredTime;
	
	private List<Queue> configuredQueues;
	
	private Date now;
	
	private final List<QueueEntry> evictedFromSession = new ArrayList<>();
	
	private QueueEntry saveFailsFor;
	
	private RuntimeException saveFailure;
	
	private QueueEntry modifiedSinceLoading;
	
	private RuntimeException getQueueEntriesFailure;
	
	private int sessionFlushes;
	
	class TestAutoCloseQueueEntryTask extends AutoCloseQueueEntryTask {
		
		@Override
		protected String getConfiguredCloseTime() {
			return configuredTime;
		}
		
		@Override
		protected List<Queue> getQueuesToClear() {
			return configuredQueues;
		}
		
		@Override
		protected Date now() {
			return now;
		}
		
		@Override
		protected List<QueueEntry> getQueueEntries(QueueEntrySearchCriteria criteria) {
			if (getQueueEntriesFailure != null) {
				throw getQueueEntriesFailure;
			}
			// Emulate the DB-level filtering that getQueueEntries would normally perform
			return queueEntries.stream()
			        .filter(e -> criteria.getIsEnded() == null || criteria.getIsEnded().equals(e.getEndedAt() != null))
			        .filter(e -> e.getStartedAt() == null || !e.getStartedAt().after(criteria.getStartedOnOrBefore()))
			        .filter(e -> criteria.getQueues() == null || criteria.getQueues().contains(e.getQueue()))
			        .collect(Collectors.toList());
		}
		
		/**
		 * Emulates
		 * {@link org.openmrs.module.queue.api.QueueEntryService#closeQueueEntry(QueueEntry, Date)}, which
		 * ends the entry unless it has been modified since it was loaded
		 */
		@Override
		protected boolean endQueueEntry(QueueEntry queueEntry, Date endedAt) {
			if (queueEntry == saveFailsFor) {
				throw saveFailure;
			}
			if (queueEntry == modifiedSinceLoading) {
				return false;
			}
			queueEntry.setEndedAt(endedAt);
			return true;
		}
		
		@Override
		protected void evictFromSession(QueueEntry queueEntry) {
			evictedFromSession.add(queueEntry);
		}
		
		@Override
		protected void flushAndClearSession() {
			sessionFlushes++;
		}
	}
	
	@Before
	public void setup() throws Exception {
		queueEntries.clear();
		evictedFromSession.clear();
		configuredQueues = null;
		saveFailsFor = null;
		saveFailure = null;
		modifiedSinceLoading = null;
		getQueueEntriesFailure = null;
		sessionFlushes = 0;
		now = getDate("2020-01-01 23:59");
	}
	
	@Test
	public void shouldDoNothingWhenTimeIsBlank() throws Exception {
		configuredTime = "";
		QueueEntry queueEntry = queueEntryStartedAt("2020-01-01 09:00", null);
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(queueEntry.getEndedAt(), nullValue());
	}
	
	@Test
	public void shouldDoNothingWhenTimeIsUnparseable() throws Exception {
		configuredTime = "nonsense";
		QueueEntry queueEntry = queueEntryStartedAt("2020-01-01 09:00", null);
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(queueEntry.getEndedAt(), nullValue());
	}
	
	@Test
	public void shouldDoNothingWhenTimeHasTrailingCharacters() throws Exception {
		configuredTime = "11:00 PM";
		QueueEntry queueEntry = queueEntryStartedAt("2020-01-01 09:00", null);
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(queueEntry.getEndedAt(), nullValue());
	}
	
	@Test
	public void shouldNotClearBeforeConfiguredTime() throws Exception {
		configuredTime = "23:59";
		now = getDate("2020-01-01 17:00");
		QueueEntry queueEntry = queueEntryStartedAt("2020-01-01 09:00", null);
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(queueEntry.getEndedAt(), nullValue());
	}
	
	@Test
	public void shouldClearActiveEntriesAtOrAfterConfiguredTime() throws Exception {
		configuredTime = "23:59";
		QueueEntry queueEntry = queueEntryStartedAt("2020-01-01 09:00", null);
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(queueEntry.getEndedAt(), equalTo(now));
	}
	
	@Test
	public void shouldClearEntriesWhenTheConfiguredTimeWasMissed() throws Exception {
		configuredTime = "23:59";
		now = getDate("2020-01-02 08:00");
		QueueEntry queueEntry = queueEntryStartedAt("2020-01-01 09:00", null);
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(queueEntry.getEndedAt(), equalTo(getDate("2020-01-01 23:59")));
	}
	
	@Test
	public void shouldEndEntriesStartedAtTheCloseTimeOneSecondLater() throws Exception {
		configuredTime = "18:00";
		now = getDate("2020-01-01 18:30");
		QueueEntry queueEntry = queueEntryStartedAt("2020-01-01 18:00", null);
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(queueEntry.getEndedAt(), equalTo(new Date(getDate("2020-01-01 18:00").getTime() + 1000L)));
	}
	
	@Test
	public void shouldClearEntriesOnALaterRunOfTheSameTaskInstance() throws Exception {
		configuredTime = "23:59";
		TestAutoCloseQueueEntryTask task = new TestAutoCloseQueueEntryTask();
		task.execute();
		
		QueueEntry queueEntry = queueEntryStartedAt("2020-01-02 09:00", null);
		now = getDate("2020-01-02 23:59");
		task.execute();
		assertThat(queueEntry.getEndedAt(), equalTo(now));
	}
	
	@Test
	public void shouldLeaveEntriesModifiedSinceTheyWereLoadedAlone() throws Exception {
		configuredTime = "23:59";
		QueueEntry transitionedInTheMeantime = queueEntryStartedAt("2020-01-01 09:00", null);
		QueueEntry stillActive = queueEntryStartedAt("2020-01-01 10:00", null);
		modifiedSinceLoading = transitionedInTheMeantime;
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(transitionedInTheMeantime.getEndedAt(), nullValue());
		assertThat(stillActive.getEndedAt(), equalTo(now));
	}
	
	@Test
	public void shouldFlushTheSessionPeriodicallyWhileClearingALargeNumberOfEntries() throws Exception {
		configuredTime = "23:59";
		for (int i = 0; i < 501; i++) {
			queueEntryStartedAt("2020-01-01 09:00", null);
		}
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(sessionFlushes, equalTo(2));
	}
	
	@Test
	public void shouldNotFlushTheSessionWhenClearingAHandfulOfEntries() throws Exception {
		configuredTime = "23:59";
		queueEntryStartedAt("2020-01-01 09:00", null);
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(sessionFlushes, equalTo(0));
	}
	
	@Test
	public void shouldNotPropagateWhenFetchingQueueEntriesFails() throws Exception {
		configuredTime = "23:59";
		QueueEntry queueEntry = queueEntryStartedAt("2020-01-01 09:00", null);
		getQueueEntriesFailure = new APIException("could not query queue entries");
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(queueEntry.getEndedAt(), nullValue());
	}
	
	@Test
	public void shouldNotClearEntriesStartedAfterTheMostRecentCloseTime() throws Exception {
		configuredTime = "23:59";
		now = getDate("2020-01-02 08:00");
		QueueEntry queueEntry = queueEntryStartedAt("2020-01-02 07:00", null);
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(queueEntry.getEndedAt(), nullValue());
	}
	
	@Test
	public void shouldNotRewriteEntriesThatAreAlreadyEnded() throws Exception {
		configuredTime = "23:59";
		Date alreadyEndedAt = getDate("2020-01-01 10:00");
		QueueEntry queueEntry = queueEntryStartedAt("2020-01-01 09:00", null);
		queueEntry.setEndedAt(alreadyEndedAt);
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(queueEntry.getEndedAt(), equalTo(alreadyEndedAt));
	}
	
	@Test
	public void shouldNotClearEntriesStartedAfterConfiguredTime() throws Exception {
		configuredTime = "18:00";
		now = getDate("2020-01-01 18:30");
		QueueEntry beforeCloseTime = queueEntryStartedAt("2020-01-01 09:00", null);
		QueueEntry afterCloseTime = queueEntryStartedAt("2020-01-01 18:15", null);
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(beforeCloseTime.getEndedAt(), notNullValue());
		assertThat(afterCloseTime.getEndedAt(), nullValue());
	}
	
	@Test
	public void shouldOnlyClearConfiguredQueues() throws Exception {
		configuredTime = "23:59";
		Queue queueA = new Queue();
		Queue queueB = new Queue();
		configuredQueues = new ArrayList<>();
		configuredQueues.add(queueA);
		
		QueueEntry inQueueA = queueEntryStartedAt("2020-01-01 09:00", queueA);
		QueueEntry inQueueB = queueEntryStartedAt("2020-01-01 09:00", queueB);
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(inQueueA.getEndedAt(), notNullValue());
		assertThat(inQueueB.getEndedAt(), nullValue());
	}
	
	@Test
	public void shouldEvictAndContinueWhenTheDaoRejectsAnEntry() throws Exception {
		configuredTime = "23:59";
		QueueEntry rejected = queueEntryStartedAt("2020-01-01 09:00", null);
		QueueEntry saved = queueEntryStartedAt("2020-01-01 10:00", null);
		saveFailsFor = rejected;
		saveFailure = new IllegalArgumentException("Queue entry endedAt must be after startedAt");
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(evictedFromSession, contains(rejected));
		assertThat(saved.getEndedAt(), equalTo(now));
	}
	
	@Test
	public void shouldEvictAndContinueWhenSavingAnEntryFails() throws Exception {
		configuredTime = "23:59";
		QueueEntry failed = queueEntryStartedAt("2020-01-01 09:00", null);
		QueueEntry saved = queueEntryStartedAt("2020-01-01 10:00", null);
		saveFailsFor = failed;
		saveFailure = new APIException("could not save");
		
		new TestAutoCloseQueueEntryTask().execute();
		assertThat(evictedFromSession, contains(failed));
		assertThat(saved.getEndedAt(), equalTo(now));
	}
	
	@Test
	public void getQueuesToClearShouldReturnNullWhenNoQueuesAreConfigured() {
		assertThat(taskForConfiguredQueues("  ").getQueuesToClear(), nullValue());
	}
	
	@Test
	public void getQueuesToClearShouldResolveConfiguredUuids() {
		Queue queueA = new Queue();
		Queue queueB = new Queue();
		AutoCloseQueueEntryTask task = taskForConfiguredQueues(" uuid-a , ,uuid-b,");
		when(task.getServices().getQueue("uuid-a")).thenReturn(queueA);
		when(task.getServices().getQueue("uuid-b")).thenReturn(queueB);
		
		assertThat(task.getQueuesToClear(), contains(queueA, queueB));
	}
	
	@Test
	public void getQueuesToClearShouldSkipUnknownUuids() {
		Queue queueA = new Queue();
		AutoCloseQueueEntryTask task = taskForConfiguredQueues("uuid-a,not-a-queue");
		when(task.getServices().getQueue("uuid-a")).thenReturn(queueA);
		when(task.getServices().getQueue("not-a-queue")).thenThrow(new IllegalArgumentException());
		
		assertThat(task.getQueuesToClear(), contains(queueA));
	}
	
	@Test
	public void getQueuesToClearShouldReturnEmptyListWhenNoConfiguredUuidResolves() {
		AutoCloseQueueEntryTask task = taskForConfiguredQueues("not-a-queue");
		when(task.getServices().getQueue("not-a-queue")).thenThrow(new IllegalArgumentException());
		
		assertThat(task.getQueuesToClear(), empty());
	}
	
	/**
	 * @return a task whose services report the given value for the configured queues global property,
	 *         so that the real {@link AutoCloseQueueEntryTask#getQueuesToClear()} is exercised
	 */
	private AutoCloseQueueEntryTask taskForConfiguredQueues(String configuredQueueUuids) {
		QueueServicesWrapper services = mock(QueueServicesWrapper.class);
		when(services.getGlobalProperty(AUTO_CLOSE_QUEUE_ENTRIES_FOR_QUEUES)).thenReturn(configuredQueueUuids);
		return new AutoCloseQueueEntryTask() {
			
			@Override
			protected QueueServicesWrapper getServices() {
				return services;
			}
		};
	}
	
	private QueueEntry queueEntryStartedAt(String startedAt, Queue queue) throws Exception {
		QueueEntry queueEntry = new QueueEntry();
		queueEntry.setStartedAt(getDate(startedAt));
		queueEntry.setQueue(queue);
		queueEntries.add(queueEntry);
		return queueEntry;
	}
	
	Date getDate(String dateStr) throws Exception {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return df.parse(dateStr);
	}
}
