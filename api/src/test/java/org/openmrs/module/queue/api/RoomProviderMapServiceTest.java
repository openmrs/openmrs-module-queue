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
import org.openmrs.module.queue.api.dao.RoomProviderMapDao;
import org.openmrs.module.queue.api.impl.RoomProviderMapServiceImpl;
import org.openmrs.module.queue.api.search.RoomProviderMapSearchCriteria;
import org.openmrs.module.queue.model.RoomProviderMap;

@RunWith(MockitoJUnitRunner.class)
public class RoomProviderMapServiceTest {
	
	private static final String ROOM_PROVIDER_MAP_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252cc6";
	
	private static final String QUEUE_ROOM_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252cc6";
	
	private static final String PROVIDER_UUID = "a2c3868a-6b90-11e0-93c3-18a905e044dc";
	
	private RoomProviderMapServiceImpl roomProviderMapService;
	
	@Mock
	private RoomProviderMapDao dao;
	
	@Captor
	ArgumentCaptor<RoomProviderMapSearchCriteria> roomProviderMapSearchCriteriaArgumentCaptor;
	
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
		
		RoomProviderMap result = roomProviderMapService.saveRoomProviderMap(roomProviderMap);
		assertThat(result, notNullValue());
		assertThat(result.getUuid(), is(ROOM_PROVIDER_MAP_UUID));
	}
	
	@Test
	public void shouldVoidRoomProviderMap() {
		User user = new User(1);
		UserContext userContext = mock(UserContext.class);
		when(userContext.getAuthenticatedUser()).thenReturn(user);
		Context.setUserContext(userContext);
		RoomProviderMap roomProviderMap = new RoomProviderMap();
		when(dao.createOrUpdate(roomProviderMap)).thenReturn(roomProviderMap);
		assertThat(roomProviderMap.getVoided(), equalTo(false));
		assertThat(roomProviderMap.getDateVoided(), nullValue());
		assertThat(roomProviderMap.getVoidedBy(), nullValue());
		assertThat(roomProviderMap.getVoidReason(), nullValue());
		roomProviderMapService.voidRoomProviderMap(roomProviderMap, "voidReason");
		assertThat(roomProviderMap.getVoided(), equalTo(true));
		assertThat(roomProviderMap.getDateVoided(), notNullValue());
		assertThat(roomProviderMap.getVoidedBy(), equalTo(user));
		assertThat(roomProviderMap.getVoidReason(), equalTo("voidReason"));
	}
	
	@Test
	public void shouldPurgeRoomProviderMap() {
		RoomProviderMap roomProviderMap = mock(RoomProviderMap.class);
		when(dao.get(QUEUE_ROOM_UUID)).thenReturn(Optional.empty());
		
		roomProviderMapService.purgeRoomProviderMap(roomProviderMap);
		assertThat(roomProviderMapService.getRoomProviderMapByUuid(ROOM_PROVIDER_MAP_UUID).isPresent(), is(false));
	}
	
	@Test
	public void shouldGetRoomProviderMapsByCriteria() {
		RoomProviderMapSearchCriteria criteria = new RoomProviderMapSearchCriteria();
		roomProviderMapService.getRoomProviderMaps(criteria);
		verify(dao).getRoomProviderMaps(roomProviderMapSearchCriteriaArgumentCaptor.capture());
		RoomProviderMapSearchCriteria daoCriteria = roomProviderMapSearchCriteriaArgumentCaptor.getValue();
		assertThat(daoCriteria, equalTo(criteria));
	}
}
