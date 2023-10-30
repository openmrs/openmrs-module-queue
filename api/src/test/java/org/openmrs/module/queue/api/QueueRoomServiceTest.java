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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.queue.api.dao.QueueRoomDao;
import org.openmrs.module.queue.api.impl.QueueRoomServiceImpl;
import org.openmrs.module.queue.api.search.QueueRoomSearchCriteria;
import org.openmrs.module.queue.model.QueueRoom;

@RunWith(MockitoJUnitRunner.class)
public class QueueRoomServiceTest {
	
	private static final String QUEUE_ROOM_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252cc6";
	
	private static final String QUEUE_ROOM_NAME = "Triage Room 1";
	
	private static final String QUEUE_UUID = "3eb7fe43-2813-4kbc-80dc-2e5d30252bb5";
	
	private static final String LOCATION_UUID = "d0938432-1691-11df-97a5-7038c098";
	
	private QueueRoomServiceImpl queueRoomService;
	
	@Mock
	private QueueRoomDao dao;
	
	@Captor
	ArgumentCaptor<QueueRoomSearchCriteria> queueRoomSearchCriteriaArgumentCaptor;
	
	@Before
	public void setupMocks() {
		MockitoAnnotations.openMocks(this);
		queueRoomService = new QueueRoomServiceImpl();
		queueRoomService.setDao(dao);
	}
	
	@Test
	public void shouldGetByUuid() {
		QueueRoom queueRoom = mock(QueueRoom.class);
		when(queueRoom.getUuid()).thenReturn(QUEUE_ROOM_UUID);
		when(dao.get(QUEUE_ROOM_UUID)).thenReturn(Optional.of(queueRoom));
		
		Optional<QueueRoom> result = queueRoomService.getQueueRoomByUuid(QUEUE_ROOM_UUID);
		assertThat(result.isPresent(), is(true));
		result.ifPresent(q -> assertThat(q.getUuid(), is(QUEUE_ROOM_UUID)));
	}
	
	@Test
	public void shouldCreateNewQueue() {
		QueueRoom queueRoom = mock(QueueRoom.class);
		when(queueRoom.getUuid()).thenReturn(QUEUE_ROOM_UUID);
		when(queueRoom.getName()).thenReturn(QUEUE_ROOM_NAME);
		when(dao.createOrUpdate(queueRoom)).thenReturn(queueRoom);
		
		QueueRoom result = queueRoomService.createQueueRoom(queueRoom);
		assertThat(result, notNullValue());
		assertThat(result.getUuid(), is(QUEUE_ROOM_UUID));
		assertThat(result.getName(), is(QUEUE_ROOM_NAME));
	}
	
	@Test
	public void shouldRetireQueueRoom() {
		User user = new User();
		UserContext userContext = mock(UserContext.class);
		when(userContext.getAuthenticatedUser()).thenReturn(user);
		Context.setUserContext(userContext);
		QueueRoom queueRoom = new QueueRoom();
		when(dao.createOrUpdate(queueRoom)).thenReturn(queueRoom);
		assertThat(queueRoom.getRetired(), equalTo(false));
		assertThat(queueRoom.getDateRetired(), nullValue());
		assertThat(queueRoom.getRetiredBy(), nullValue());
		assertThat(queueRoom.getRetireReason(), nullValue());
		queueRoomService.retireQueueRoom(queueRoom, "retireReason");
		assertThat(queueRoom.getRetired(), equalTo(true));
		assertThat(queueRoom.getDateRetired(), notNullValue());
		assertThat(queueRoom.getRetiredBy(), equalTo(user));
		assertThat(queueRoom.getRetireReason(), equalTo("retireReason"));
	}
	
	@Test
	public void shouldPurgeQueueRoom() {
		QueueRoom queueRoom = mock(QueueRoom.class);
		when(dao.get(QUEUE_ROOM_UUID)).thenReturn(Optional.empty());
		queueRoomService.purgeQueueRoom(queueRoom);
		assertThat(queueRoomService.getQueueRoomByUuid(QUEUE_ROOM_UUID).isPresent(), is(false));
	}
	
	@Test
	public void shouldGetQueueRoomsByCriteria() {
		QueueRoomSearchCriteria criteria = new QueueRoomSearchCriteria();
		queueRoomService.getQueueRooms(criteria);
		verify(dao).getQueueRooms(queueRoomSearchCriteriaArgumentCaptor.capture());
		QueueRoomSearchCriteria daoCriteria = queueRoomSearchCriteriaArgumentCaptor.getValue();
		assertThat(daoCriteria, equalTo(criteria));
	}
}
