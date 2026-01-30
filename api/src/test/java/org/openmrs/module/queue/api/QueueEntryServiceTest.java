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
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.time.DateUtils;
import org.junit.After;
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
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.VisitAttributeType;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.api.impl.QueueEntryServiceImpl;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.api.sort.ExistingValueSortWeightGenerator;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.QueueEntryTransition;

@RunWith(MockitoJUnitRunner.class)
public class QueueEntryServiceTest {
	
	private static final String QUEUE_ENTRY_UUID = "j8f0bb90-86f4-4d9c-8b6c-3713d748ef74";
	
	private static final Integer QUEUE_ENTRY_ID = 14;
	
	private QueueEntryServiceImpl queueEntryService;
	
	@Mock
	private QueueEntryDao dao;
	
	@Mock
	private VisitService visitService;
	
	@Captor
	ArgumentCaptor<QueueEntrySearchCriteria> queueEntrySearchCriteriaArgumentCaptor;
	
	AutoCloseable mocksCloser = null;
	
	@Before
	public void setupMocks() {
		mocksCloser = MockitoAnnotations.openMocks(this);
		queueEntryService = new QueueEntryServiceImpl() {
			
			@Override
			protected QueueEntryService getProxiedQueueEntryService() {
				return this;
			}
		};
		queueEntryService.setDao(dao);
		queueEntryService.setVisitService(visitService);
		queueEntryService.setSortWeightGenerator(new ExistingValueSortWeightGenerator());
	}
	
	@After
	public void tearDownMocks() throws Exception {
		mocksCloser.close();
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
	public void shouldVoidQueueEntry() {
		User user = new User(1);
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
		Context.logout();
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
	public void shouldTransitionQueueEntry() {
		Patient patient1 = new Patient();
		Visit visit1 = new Visit();
		visit1.setPatient(patient1);
		Queue queue0 = new Queue();
		Queue queue1 = new Queue();
		Queue queue2 = new Queue();
		Concept concept1 = new Concept();
		Concept concept2 = new Concept();
		String string1 = "starting";
		String string2 = "some string";
		double double1 = 5.0;
		Location location1 = new Location();
		Provider provider1 = new Provider();
		Date date1 = DateUtils.addHours(new Date(), -12);
		Date date2 = DateUtils.addHours(date1, 6);
		Date date3 = DateUtils.addHours(date1, 3);
		
		QueueEntry queueEntry1 = new QueueEntry();
		queueEntry1.setQueueEntryId(1);
		queueEntry1.setQueue(queue1);
		queueEntry1.setPatient(patient1);
		queueEntry1.setVisit(visit1);
		queueEntry1.setPriority(concept1);
		queueEntry1.setPriorityComment(string1);
		queueEntry1.setStatus(concept1);
		queueEntry1.setSortWeight(double1);
		queueEntry1.setLocationWaitingFor(location1);
		queueEntry1.setProviderWaitingFor(provider1);
		queueEntry1.setQueueComingFrom(queue0);
		queueEntry1.setStartedAt(date1);
		assertNull(queueEntry1.getEndedAt());
		
		// Mock the DAO to return the object being saved and assign IDs to new entries
		AtomicInteger idCounter = new AtomicInteger(1);
		Map<Integer, QueueEntry> entryMap = new HashMap<>();
		entryMap.put(1, queueEntry1);
		when(dao.createOrUpdate(any())).thenAnswer(invocation -> {
			QueueEntry entry = invocation.getArgument(0);
			if (entry.getId() == null) {
				entry.setQueueEntryId(idCounter.incrementAndGet());
			}
			entryMap.put(entry.getId(), entry);
			return entry;
		});
		when(dao.get(anyInt())).thenAnswer(invocation -> {
			int id = invocation.getArgument(0);
			return Optional.ofNullable(entryMap.get(id));
		});
		when(dao.updateIfUnmodified(any(), any())).thenReturn(true);
		
		// First transition test that no changes are required and all values will be pulled from existing queue entry
		QueueEntryTransition transition1 = new QueueEntryTransition();
		transition1.setQueueEntryToTransition(queueEntry1);
		transition1.setTransitionDate(date2);
		QueueEntry queueEntry2 = queueEntryService.transitionQueueEntry(transition1);
		assertThat(queueEntry1.getEndedAt(), equalTo(date2));
		assertThat(queueEntry2.getQueue(), equalTo(queue1));
		assertThat(queueEntry2.getPatient(), equalTo(patient1));
		assertThat(queueEntry2.getVisit(), equalTo(visit1));
		assertThat(queueEntry2.getPriority(), equalTo(concept1));
		assertThat(queueEntry2.getPriorityComment(), equalTo(string1));
		assertThat(queueEntry2.getStatus(), equalTo(concept1));
		assertThat(queueEntry2.getSortWeight(), equalTo(double1));
		assertThat(queueEntry2.getLocationWaitingFor(), equalTo(location1));
		assertThat(queueEntry2.getProviderWaitingFor(), equalTo(provider1));
		assertThat(queueEntry2.getQueueComingFrom(), equalTo(queue1));
		assertThat(queueEntry2.getStartedAt(), equalTo(date2));
		assertNull(queueEntry2.getEndedAt());
		
		// Next transition test that appropriate fields can be changed
		QueueEntryTransition transition2 = new QueueEntryTransition();
		transition2.setQueueEntryToTransition(queueEntry2);
		transition2.setTransitionDate(date3);
		transition2.setNewQueue(queue2);
		transition2.setNewPriority(concept2);
		transition2.setNewPriorityComment(string2);
		transition2.setNewStatus(concept2);
		QueueEntry queueEntry3 = queueEntryService.transitionQueueEntry(transition2);
		assertThat(queueEntry2.getEndedAt(), equalTo(date3));
		assertThat(queueEntry3.getQueue(), equalTo(queue2));
		assertThat(queueEntry3.getPatient(), equalTo(patient1));
		assertThat(queueEntry3.getVisit(), equalTo(visit1));
		assertThat(queueEntry3.getPriority(), equalTo(concept2));
		assertThat(queueEntry3.getPriorityComment(), equalTo(string2));
		assertThat(queueEntry3.getStatus(), equalTo(concept2));
		assertThat(queueEntry3.getSortWeight(), equalTo(double1));
		assertThat(queueEntry3.getLocationWaitingFor(), equalTo(location1));
		assertThat(queueEntry3.getProviderWaitingFor(), equalTo(provider1));
		assertThat(queueEntry3.getQueueComingFrom(), equalTo(queue1));
		assertThat(queueEntry3.getStartedAt(), equalTo(date3));
		assertNull(queueEntry3.getEndedAt());
	}
	
	@Test
	public void shouldUndoTransitionQueueEntry() {
		Patient patient1 = new Patient();
		Visit visit1 = new Visit();
		visit1.setPatient(patient1);
		Queue queue0 = new Queue();
		Queue queue1 = new Queue();
		Concept concept1 = new Concept();
		String string1 = "starting";
		double double1 = 5.0;
		Location location1 = new Location();
		Provider provider1 = new Provider();
		Date date1 = DateUtils.addHours(new Date(), -12);
		Date date2 = DateUtils.addHours(date1, 6);
		
		QueueEntry queueEntry1 = new QueueEntry();
		queueEntry1.setQueueEntryId(1);
		queueEntry1.setQueue(queue1);
		queueEntry1.setPatient(patient1);
		queueEntry1.setVisit(visit1);
		queueEntry1.setPriority(concept1);
		queueEntry1.setPriorityComment(string1);
		queueEntry1.setStatus(concept1);
		queueEntry1.setSortWeight(double1);
		queueEntry1.setLocationWaitingFor(location1);
		queueEntry1.setProviderWaitingFor(provider1);
		queueEntry1.setQueueComingFrom(queue0);
		queueEntry1.setStartedAt(date1);
		assertNull(queueEntry1.getEndedAt());
		
		// Mock the DAO to return the object being saved and assign IDs to new entries
		AtomicInteger idCounter = new AtomicInteger(1);
		Map<Integer, QueueEntry> entryMap = new HashMap<>();
		entryMap.put(1, queueEntry1);
		when(dao.createOrUpdate(any())).thenAnswer(invocation -> {
			QueueEntry entry = invocation.getArgument(0);
			if (entry.getId() == null) {
				entry.setQueueEntryId(idCounter.incrementAndGet());
			}
			entryMap.put(entry.getId(), entry);
			return entry;
		});
		when(dao.get(anyInt())).thenAnswer(invocation -> {
			int id = invocation.getArgument(0);
			return Optional.ofNullable(entryMap.get(id));
		});
		when(dao.updateIfUnmodified(any(), any())).thenReturn(true);
		
		// Create transition
		QueueEntryTransition transition1 = new QueueEntryTransition();
		transition1.setQueueEntryToTransition(queueEntry1);
		transition1.setTransitionDate(date2);
		QueueEntry queueEntry2 = queueEntryService.transitionQueueEntry(transition1);
		
		when(dao.getQueueEntries(any())).thenReturn(Arrays.asList(queueEntry1));
		User user = new User(1);
		UserContext userContext = mock(UserContext.class);
		when(userContext.getAuthenticatedUser()).thenReturn(user);
		Context.setUserContext(userContext);
		
		try {
			queueEntryService.undoTransition(queueEntry2);
			
			assertThat(queueEntry2.getVoided(), equalTo(true));
			assertNull(queueEntry1.getEndedAt());
		}
		finally {
			Context.setUserContext(null);
		}
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowWhenTransitioningUnsavedEntry() {
		QueueEntry unsavedEntry = new QueueEntry();
		QueueEntryTransition transition = new QueueEntryTransition();
		transition.setQueueEntryToTransition(unsavedEntry);
		transition.setTransitionDate(new Date());
		queueEntryService.transitionQueueEntry(transition);
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowWhenTransitioningVoidedEntry() {
		QueueEntry voidedEntry = new QueueEntry();
		voidedEntry.setQueueEntryId(1);
		voidedEntry.setVoided(true);
		when(dao.get(1)).thenReturn(Optional.of(voidedEntry));
		
		QueueEntryTransition transition = new QueueEntryTransition();
		transition.setQueueEntryToTransition(voidedEntry);
		transition.setTransitionDate(new Date());
		queueEntryService.transitionQueueEntry(transition);
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowWhenTransitioningEndedEntry() {
		QueueEntry endedEntry = new QueueEntry();
		endedEntry.setQueueEntryId(1);
		endedEntry.setEndedAt(new Date());
		when(dao.get(1)).thenReturn(Optional.of(endedEntry));
		
		QueueEntryTransition transition = new QueueEntryTransition();
		transition.setQueueEntryToTransition(endedEntry);
		transition.setTransitionDate(new Date());
		queueEntryService.transitionQueueEntry(transition);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowWhenUndoingTransitionOnUnsavedEntry() {
		QueueEntry unsavedEntry = new QueueEntry();
		queueEntryService.undoTransition(unsavedEntry);
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowWhenUndoingTransitionOnVoidedEntry() {
		QueueEntry voidedEntry = new QueueEntry();
		voidedEntry.setQueueEntryId(1);
		voidedEntry.setVoided(true);
		when(dao.get(1)).thenReturn(Optional.of(voidedEntry));
		
		queueEntryService.undoTransition(voidedEntry);
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowWhenUndoingTransitionOnEndedEntry() {
		QueueEntry endedEntry = new QueueEntry();
		endedEntry.setQueueEntryId(1);
		endedEntry.setEndedAt(new Date());
		when(dao.get(1)).thenReturn(Optional.of(endedEntry));
		
		queueEntryService.undoTransition(endedEntry);
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowWhenTransitioningConcurrentlyModifiedEntry() {
		QueueEntry queueEntry = new QueueEntry();
		queueEntry.setQueueEntryId(1);
		queueEntry.setQueue(new Queue());
		queueEntry.setPatient(new Patient());
		queueEntry.setStatus(new Concept());
		queueEntry.setPriority(new Concept());
		queueEntry.setStartedAt(new Date());
		when(dao.get(1)).thenReturn(Optional.of(queueEntry));
		when(dao.updateIfUnmodified(any(), any())).thenReturn(false);
		
		QueueEntryTransition transition = new QueueEntryTransition();
		transition.setQueueEntryToTransition(queueEntry);
		transition.setTransitionDate(new Date());
		queueEntryService.transitionQueueEntry(transition);
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowWhenUndoingTransitionOnConcurrentlyModifiedPreviousEntry() {
		Queue queue1 = new Queue();
		Patient patient1 = new Patient();
		Visit visit1 = new Visit();
		visit1.setPatient(patient1);
		Concept concept1 = new Concept();
		Date date1 = DateUtils.addHours(new Date(), -12);
		Date date2 = DateUtils.addHours(date1, 6);
		
		QueueEntry prevEntry = new QueueEntry();
		prevEntry.setQueueEntryId(1);
		prevEntry.setQueue(queue1);
		prevEntry.setPatient(patient1);
		prevEntry.setVisit(visit1);
		prevEntry.setStatus(concept1);
		prevEntry.setPriority(concept1);
		prevEntry.setStartedAt(date1);
		prevEntry.setEndedAt(date2);
		
		QueueEntry currentEntry = new QueueEntry();
		currentEntry.setQueueEntryId(2);
		currentEntry.setQueue(queue1);
		currentEntry.setPatient(patient1);
		currentEntry.setVisit(visit1);
		currentEntry.setStatus(concept1);
		currentEntry.setPriority(concept1);
		currentEntry.setStartedAt(date2);
		currentEntry.setQueueComingFrom(queue1);
		
		when(dao.get(2)).thenReturn(Optional.of(currentEntry));
		when(dao.getQueueEntries(any())).thenReturn(Arrays.asList(prevEntry));
		when(dao.updateIfUnmodified(any(), any())).thenReturn(false);
		
		queueEntryService.undoTransition(currentEntry);
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
