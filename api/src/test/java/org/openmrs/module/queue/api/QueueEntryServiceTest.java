/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.api.impl.QueueEntryServiceImpl;
import org.openmrs.module.queue.model.QueueEntry;

@RunWith(MockitoJUnitRunner.class)
public class QueueEntryServiceTest {
	
	private static final String QUEUE_ENTRY_UUID = "j8f0bb90-86f4-4d9c-8b6c-3713d748ef74";
	
	private static final Integer QUEUE_ENTRY_ID = 14;
	
	private QueueEntryServiceImpl queueEntryService;
	
	@Mock
	private QueueEntryDao<QueueEntry> dao;
	
	@Before
	public void setupMocks() {
		MockitoAnnotations.openMocks(this);
		queueEntryService = new QueueEntryServiceImpl();
		queueEntryService.setDao(dao);
	}
	
	@Test
	public void shouldGetQueueEntryByUuid() {
		QueueEntry queueEntry = mock(QueueEntry.class);
		when(queueEntry.getUuid()).thenReturn(QUEUE_ENTRY_UUID);
		when(dao.get(QUEUE_ENTRY_UUID)).thenReturn(Optional.of(queueEntry));
		
		Optional<QueueEntry> result = queueEntryService.getQueueEntryByUuid(QUEUE_ENTRY_UUID);
		assertThat(result.isPresent(), is(true));
		result.ifPresent(q -> assertThat(q.getUuid(), is(QUEUE_ENTRY_UUID)));
	}
	
	@Test
	public void shouldGetQueueEntryById() {
		QueueEntry queueEntry = mock(QueueEntry.class);
		when(queueEntry.getQueueEntryId()).thenReturn(QUEUE_ENTRY_ID);
		when(dao.get(QUEUE_ENTRY_ID)).thenReturn(Optional.of(queueEntry));
		
		Optional<QueueEntry> result = queueEntryService.getQueueEntryById(QUEUE_ENTRY_ID);
		assertThat(result.isPresent(), is(true));
		result.ifPresent(q -> assertThat(q.getQueueEntryId(), is(QUEUE_ENTRY_ID)));
	}
	
	@Test
	public void shouldCreateNewQueueEntryRecord() {
		QueueEntry queueEntry = mock(QueueEntry.class);
		Concept conceptStatus = mock(Concept.class);
		Concept conceptService = mock(Concept.class);
		Concept conceptPriority = mock(Concept.class);
		
		when(queueEntry.getQueueEntryId()).thenReturn(QUEUE_ENTRY_ID);
		when(queueEntry.getStatus()).thenReturn(conceptStatus);
		when(queueEntry.getService()).thenReturn(conceptService);
		when(queueEntry.getPriority()).thenReturn(conceptPriority);
		when(dao.createOrUpdate(queueEntry)).thenReturn(queueEntry);
		
		QueueEntry result = queueEntryService.createQueueEntry(queueEntry);
		assertThat(result, notNullValue());
		assertThat(result.getQueueEntryId(), is(QUEUE_ENTRY_ID));
		assertThat(result.getStatus(), is(conceptStatus));
		assertThat(result.getService(), is(conceptService));
		assertThat(result.getPriority(), is(conceptPriority));
	}
	
	@Test
	public void shouldVoidQueue() {
		when(dao.get(QUEUE_ENTRY_UUID)).thenReturn(Optional.empty());
		
		queueEntryService.voidQueueEntry(QUEUE_ENTRY_UUID, "voidReason");
		
		assertThat(queueEntryService.getQueueEntryByUuid(QUEUE_ENTRY_UUID).isPresent(), is(false));
	}
	
	@Test
	public void shouldPurgeQueue() {
		QueueEntry queueEntry = mock(QueueEntry.class);
		when(dao.get(QUEUE_ENTRY_UUID)).thenReturn(Optional.empty());
		
		queueEntryService.purgeQueueEntry(queueEntry);
		assertThat(queueEntryService.getQueueEntryByUuid(QUEUE_ENTRY_UUID).isPresent(), is(false));
	}
}
