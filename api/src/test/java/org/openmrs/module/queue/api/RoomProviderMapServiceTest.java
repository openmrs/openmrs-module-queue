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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Provider;
import org.openmrs.module.queue.api.dao.RoomProviderMapDao;
import org.openmrs.module.queue.api.impl.RoomProviderMapServiceImpl;
import org.openmrs.module.queue.model.QueueRoom;
import org.openmrs.module.queue.model.RoomProviderMap;

@RunWith(MockitoJUnitRunner.class)
public class RoomProviderMapServiceTest {
	
	private static final String ROOM_PROVIDER_MAP_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252cc6";
	
	private static final String QUEUE_ROOM_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252cc6";
	
	private static final String PROVIDER_UUID = "a2c3868a-6b90-11e0-93c3-18a905e044dc";
	
	private RoomProviderMapServiceImpl roomProviderMapService;
	
	@Mock
	private RoomProviderMapDao dao;
	
	@Before
	public void setupMocks() {
		MockitoAnnotations.openMocks(this);
		roomProviderMapService = new RoomProviderMapServiceImpl();
		roomProviderMapService.setDao(dao);
	}
	
	@Test
	public void shouldGetByUuid() {
		RoomProviderMap roomProviderMap = mock(RoomProviderMap.class);
		when(roomProviderMap.getUuid()).thenReturn(ROOM_PROVIDER_MAP_UUID);
		when(dao.get(ROOM_PROVIDER_MAP_UUID)).thenReturn(Optional.of(roomProviderMap));
		
		Optional<RoomProviderMap> result = roomProviderMapService.getRoomProviderMapByUuid(ROOM_PROVIDER_MAP_UUID);
		assertThat(result.isPresent(), is(true));
		result.ifPresent(q -> assertThat(q.getUuid(), is(ROOM_PROVIDER_MAP_UUID)));
	}
	
	@Test
	public void shouldCreateNewRoomProviderMap() {
		RoomProviderMap roomProviderMap = mock(RoomProviderMap.class);
		when(roomProviderMap.getUuid()).thenReturn(ROOM_PROVIDER_MAP_UUID);
		when(dao.createOrUpdate(roomProviderMap)).thenReturn(roomProviderMap);
		
		RoomProviderMap result = roomProviderMapService.createRoomProviderMap(roomProviderMap);
		assertThat(result, notNullValue());
		assertThat(result.getUuid(), is(ROOM_PROVIDER_MAP_UUID));
	}
	
	@Test
	public void shouldVoidRoomProviderMap() {
		when(dao.get(ROOM_PROVIDER_MAP_UUID)).thenReturn(Optional.empty());
		
		roomProviderMapService.voidRoomProviderMap(ROOM_PROVIDER_MAP_UUID, "API Call");
		
		assertThat(roomProviderMapService.getRoomProviderMapByUuid(ROOM_PROVIDER_MAP_UUID).isPresent(), is(false));
	}
	
	@Test
	public void shouldPurgeRoomProviderMap() {
		RoomProviderMap roomProviderMap = mock(RoomProviderMap.class);
		when(dao.get(QUEUE_ROOM_UUID)).thenReturn(Optional.empty());
		
		roomProviderMapService.purgeRoomProviderMap(roomProviderMap);
		assertThat(roomProviderMapService.getRoomProviderMapByUuid(ROOM_PROVIDER_MAP_UUID).isPresent(), is(false));
	}
	
	@Test
	public void shouldGetAllRoomProviderMapByProvider() {
		RoomProviderMap roomProviderMap = mock(RoomProviderMap.class);
		Provider provider = new Provider();
		provider.setUuid(PROVIDER_UUID);
		when(dao.getRoomProvider(provider, null)).thenReturn(Collections.singletonList(roomProviderMap));
		
		List<RoomProviderMap> result = roomProviderMapService.getRoomProvider(provider, null);
		assertThat(result, notNullValue());
		assertThat(result, hasSize(1));
	}
	
	@Test
	public void shouldGetAllRoomProviderMapByQueueRoom() {
		QueueRoom queueRoom = new QueueRoom();
		queueRoom.setUuid(QUEUE_ROOM_UUID);
		RoomProviderMap roomProviderMap = mock(RoomProviderMap.class);
		when(dao.getRoomProvider(null, queueRoom)).thenReturn(Collections.singletonList(roomProviderMap));
		
		List<RoomProviderMap> result = roomProviderMapService.getRoomProvider(null, queueRoom);
		assertThat(result, notNullValue());
		assertThat(result, hasSize(1));
	}
}
