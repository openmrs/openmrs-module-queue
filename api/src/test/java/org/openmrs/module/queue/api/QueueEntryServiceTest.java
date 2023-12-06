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
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.VisitAttributeType;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.api.impl.QueueEntryServiceImpl;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.exception.DuplicateQueueEntryException;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;

@RunWith(MockitoJUnitRunner.class)
public class QueueEntryServiceTest {
	
	private static final String QUEUE_ENTRY_UUID = "j8f0bb90-86f4-4d9c-8b6c-3713d748ef74";
	
	private static final Integer QUEUE_ENTRY_ID = 14;
	
	private QueueEntryServiceImpl queueEntryService;
	
	@Mock
	private QueueEntryDao<QueueEntry> dao;
	
	@Mock
	private VisitService visitService;
	
	@Captor
	ArgumentCaptor<QueueEntrySearchCriteria> queueEntrySearchCriteriaArgumentCaptor;
	
	@Before
	public void setupMocks() {
		MockitoAnnotations.openMocks(this);
		queueEntryService = new QueueEntryServiceImpl();
		queueEntryService.setDao(dao);
		queueEntryService.setVisitService(visitService);
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
		Concept conceptPriority = mock(Concept.class);
		
		when(queueEntry.getQueueEntryId()).thenReturn(QUEUE_ENTRY_ID);
		when(queueEntry.getStatus()).thenReturn(conceptStatus);
		when(queueEntry.getPriority()).thenReturn(conceptPriority);
		when(dao.createOrUpdate(queueEntry)).thenReturn(queueEntry);
		
		QueueEntry result = queueEntryService.saveQueueEntry(queueEntry);
		assertThat(result, notNullValue());
		assertThat(result.getQueueEntryId(), is(QUEUE_ENTRY_ID));
		assertThat(result.getStatus(), is(conceptStatus));
		assertThat(result.getPriority(), is(conceptPriority));
	}
	
	@Test
	public void shouldNotCreateDuplicateOverlappingQueueEntryRecords() {
		Queue queue = new Queue();
		Patient patient = new Patient();
		Concept conceptStatus = new Concept();
		Concept conceptPriority = new Concept();
		Date queueStartDate = new Date();
		
		QueueEntry savedQueueEntry = new QueueEntry();
		savedQueueEntry.setQueueEntryId(QUEUE_ENTRY_ID);
		savedQueueEntry.setQueue(queue);
		savedQueueEntry.setPatient(patient);
		savedQueueEntry.setStatus(conceptStatus);
		savedQueueEntry.setPriority(conceptPriority);
		savedQueueEntry.setStartedAt(queueStartDate);
		
		QueueEntry duplicateQueueEntry = new QueueEntry();
		duplicateQueueEntry.setQueue(queue);
		duplicateQueueEntry.setPatient(patient);
		duplicateQueueEntry.setStartedAt(queueStartDate);
		
		QueueEntrySearchCriteria searchCriteria = new QueueEntrySearchCriteria();
		searchCriteria.setPatient(patient);
		searchCriteria.setQueues(Collections.singletonList(queue));
		
		when(dao.createOrUpdate(savedQueueEntry)).thenReturn(savedQueueEntry);
		when(dao.getQueueEntries(searchCriteria)).thenReturn(Collections.singletonList(savedQueueEntry));
		
		// Should be able to save and re-save a queue entry without causing validation failure
		savedQueueEntry = queueEntryService.saveQueueEntry(savedQueueEntry);
		queueEntryService.saveQueueEntry(savedQueueEntry);
		
		// Should hit a validation error if a new queue entry is saved with overlapping start date
		try {
			queueEntryService.saveQueueEntry(duplicateQueueEntry);
			fail("Expected DuplicateQueueEntryException");
		}
		catch (DuplicateQueueEntryException e) {
			assertThat(e.getMessage(), is("queue.entry.duplicate.patient"));
		}
	}
	
	@Test
	public void shouldVoidQueueEntry() {
		User user = new User();
		UserContext userContext = mock(UserContext.class);
		when(userContext.getAuthenticatedUser()).thenReturn(user);
		Context.setUserContext(userContext);
		QueueEntry queueEntry = new QueueEntry();
		when(dao.createOrUpdate(queueEntry)).thenReturn(queueEntry);
		assertThat(queueEntry.getVoided(), equalTo(false));
		assertThat(queueEntry.getDateVoided(), nullValue());
		assertThat(queueEntry.getVoidedBy(), nullValue());
		assertThat(queueEntry.getVoidReason(), nullValue());
		queueEntryService.voidQueueEntry(queueEntry, "voidReason");
		assertThat(queueEntry.getVoided(), equalTo(true));
		assertThat(queueEntry.getDateVoided(), notNullValue());
		assertThat(queueEntry.getVoidedBy(), equalTo(user));
		assertThat(queueEntry.getVoidReason(), equalTo("voidReason"));
	}
	
	@Test
	public void shouldPurgeQueueEntry() {
		QueueEntry queueEntry = mock(QueueEntry.class);
		when(dao.get(QUEUE_ENTRY_UUID)).thenReturn(Optional.empty());
		queueEntryService.purgeQueueEntry(queueEntry);
		assertThat(queueEntryService.getQueueEntryByUuid(QUEUE_ENTRY_UUID).isPresent(), is(false));
	}
	
	@Test
	public void shouldReturnCountOfQueueEntriesByStatus() {
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		when(dao.getCountOfQueueEntries(criteria)).thenReturn(1L);
		assertThat(queueEntryService.getCountOfQueueEntries(criteria), is(1L));
	}
	
	@Test
	public void shouldGetQueuesEntriesByCriteria() {
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		queueEntryService.getQueueEntries(criteria);
		verify(dao).getQueueEntries(queueEntrySearchCriteriaArgumentCaptor.capture());
		QueueEntrySearchCriteria daoCriteria = queueEntrySearchCriteriaArgumentCaptor.getValue();
		assertThat(daoCriteria, equalTo(criteria));
	}
	
	@Test
	public void shouldGenerateVisitQueueNumber() {
		Visit visit = new Visit();
		Location location = new Location();
		Queue queue = new Queue();
		queue.setName("Consultation Queue");
		VisitAttributeType visitAttributeType = new VisitAttributeType();
		when(visitService.saveVisit(visit)).thenReturn(visit);
		when(queueEntryService.getCountOfQueueEntries(any())).thenReturn(52L);
		String queueNumber = queueEntryService.generateVisitQueueNumber(location, queue, visit, visitAttributeType);
		assertThat(queueNumber, notNullValue());
		assertThat(queueNumber, equalTo("CON-053"));
	}
}
