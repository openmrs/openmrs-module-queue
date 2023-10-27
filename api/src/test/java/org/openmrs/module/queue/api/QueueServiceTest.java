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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
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
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.queue.api.dao.QueueDao;
import org.openmrs.module.queue.api.impl.QueueServiceImpl;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.utils.QueueSearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class QueueServiceTest {
	
	private static final String QUEUE_UUID = "b5ffbb90-86f4-4d9c-8b6c-3713d748ef74";
	
	private static final String QUEUE_NAME = "Queue test name";
	
	private static final Integer QUEUE_ID = 123;
	
	private QueueServiceImpl queueService;
	
	@Mock
	private QueueDao dao;
	
	@Captor
	ArgumentCaptor<Queue> queueArgumentCaptor;
	
	@Captor
	ArgumentCaptor<QueueSearchCriteria> queueSearchCriteriaArgumentCaptor;
	
	@Before
	public void setupMocks() {
		MockitoAnnotations.openMocks(this);
		queueService = new QueueServiceImpl();
		queueService.setDao(dao);
	}
	
	@Test
	public void shouldGetByUuid() {
		Queue queue = mock(Queue.class);
		when(queue.getUuid()).thenReturn(QUEUE_UUID);
		when(dao.get(QUEUE_UUID)).thenReturn(Optional.of(queue));
		Optional<Queue> result = queueService.getQueueByUuid(QUEUE_UUID);
		assertThat(result.isPresent(), is(true));
		result.ifPresent(q -> assertThat(q.getUuid(), is(QUEUE_UUID)));
	}
	
	@Test
	public void shouldGetById() {
		Queue queue = mock(Queue.class);
		when(queue.getId()).thenReturn(QUEUE_ID);
		when(dao.get(QUEUE_ID)).thenReturn(Optional.of(queue));
		Optional<Queue> result = queueService.getQueueById(QUEUE_ID);
		assertThat(result.isPresent(), is(true));
		result.ifPresent(q -> assertThat(q.getId(), is(QUEUE_ID)));
	}
	
	@Test
	public void shouldCreateNewQueue() {
		Queue queue = mock(Queue.class);
		when(queue.getUuid()).thenReturn(QUEUE_UUID);
		when(queue.getName()).thenReturn(QUEUE_NAME);
		when(dao.createOrUpdate(queue)).thenReturn(queue);
		Queue result = queueService.createQueue(queue);
		assertThat(result, notNullValue());
		assertThat(result.getUuid(), is(QUEUE_UUID));
		assertThat(result.getName(), is(QUEUE_NAME));
	}
	
	@Test
	public void shouldRetireQueue() {
		User user = new User();
		UserContext userContext = mock(UserContext.class);
		when(userContext.getAuthenticatedUser()).thenReturn(user);
		Context.setUserContext(userContext);
		Queue queue = new Queue();
		when(dao.createOrUpdate(queue)).thenReturn(queue);
		assertThat(queue.getRetired(), equalTo(false));
		assertThat(queue.getDateRetired(), nullValue());
		assertThat(queue.getRetiredBy(), nullValue());
		assertThat(queue.getRetireReason(), nullValue());
		queueService.retireQueue(queue, "retireReason");
		assertThat(queue.getRetired(), equalTo(true));
		assertThat(queue.getDateRetired(), notNullValue());
		assertThat(queue.getRetiredBy(), equalTo(user));
		assertThat(queue.getRetireReason(), equalTo("retireReason"));
	}
	
	@Test
	public void shouldPurgeQueue() {
		Queue queue = new Queue();
		queueService.purgeQueue(queue);
		verify(dao).delete(queueArgumentCaptor.capture());
		assertThat(queueArgumentCaptor.getValue(), equalTo(queue));
	}
	
	@Test
	public void shouldGetAllQueuesByCriteria() {
		Concept concept1 = new Concept();
		Concept concept2 = new Concept();
		Location location1 = new Location();
		Location location2 = new Location();
		List<Concept> services = Arrays.asList(concept1, concept2);
		List<Location> locations = Arrays.asList(location1, location2);
		QueueSearchCriteria criteria = new QueueSearchCriteria();
		criteria.setServices(services);
		criteria.setLocations(locations);
		criteria.setIncludeRetired(true);
		queueService.getQueues(criteria);
		verify(dao).getQueues(queueSearchCriteriaArgumentCaptor.capture());
		QueueSearchCriteria daoCriteria = queueSearchCriteriaArgumentCaptor.getValue();
		assertThat(daoCriteria.getServices(), contains(concept1, concept2));
		assertThat(daoCriteria.getLocations(), contains(location1, location2));
		assertThat(daoCriteria.isIncludeRetired(), equalTo(true));
	}
}
