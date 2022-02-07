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
import org.openmrs.module.queue.api.dao.QueueDao;
import org.openmrs.module.queue.api.impl.QueueServiceImpl;
import org.openmrs.module.queue.model.Queue;

@RunWith(MockitoJUnitRunner.class)
public class QueueServiceTest {
	
	private static final String QUEUE_UUID = "b5ffbb90-86f4-4d9c-8b6c-3713d748ef74";
	
	private static final String QUEUE_NAME = "Queue test name";
	
	private static final Integer QUEUE_ID = 123;
	
	private QueueServiceImpl queueService;
	
	@Mock
	private QueueDao<Queue> dao;
	
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
	public void shouldVoidQueue() {
		when(dao.get(QUEUE_UUID)).thenReturn(Optional.empty());
		
		queueService.voidQueue(QUEUE_UUID, "voidReason");
		
		assertThat(queueService.getQueueByUuid(QUEUE_UUID).isPresent(), is(false));
	}
	
	@Test
	public void shouldPurgeQueue() {
		Queue queue = mock(Queue.class);
		when(dao.get(QUEUE_UUID)).thenReturn(Optional.empty());
		
		queueService.purgeQueue(queue);
		assertThat(queueService.getQueueByUuid(QUEUE_UUID).isPresent(), is(false));
	}
}
