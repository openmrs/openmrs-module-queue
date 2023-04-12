/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.model.QueueRoom;
import org.openmrs.module.queue.model.RoomProviderMap;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class RoomProviderMapDaoTest extends BaseModuleContextSensitiveTest {
	
	private static final List<String> QUEUE_ROOM_INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueRoomDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/RoomProviderMapDaoTest_initialDataset.xml");
	
	private static final String ROOM_PROVIDER_MAP_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252cc6";
	
	private static final String RETIRED_ROOM_PROVIDER_MAP__UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252cc8";
	
	private static final String NEW_ROOM_PROVIDER_MAP_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252c10";
	
	private static final String QUEUE_ROOM_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252cc6";
	
	private static final String PROVIDER_UUID = "a2c3868a-6b90-11e0-93c3-18a905e044dc";
	
	private static final String UPDATE_PROVIDER_UUID = "a3a5913e-6b94-11e0-93c3-18a905e044dc";
	
	@Autowired
	@Qualifier("roomProviderMapDao")
	private RoomProviderMapDao dao;
	
	@Autowired
	@Qualifier("queueRoomDao")
	private QueueRoomDao queueRoomDao;
	
	@Before
	public void setup() {
		QUEUE_ROOM_INITIAL_DATASET_XML.forEach(this::executeDataSet);
	}
	
	@Test
	public void shouldGetRoomProviderMapById() {
		Optional<RoomProviderMap> providerRoom = dao.get(1);
		
		assertThat(providerRoom, notNullValue());
		assertThat(providerRoom.isPresent(), is(true));
		providerRoom.ifPresent(providerRoom1 -> assertThat(providerRoom1.getId(), is(1)));
	}
	
	@Test
	public void shouldGetRoomProviderMapByUuid() {
		Optional<RoomProviderMap> providerRoom = dao.get(QUEUE_ROOM_UUID);
		
		assertThat(providerRoom, notNullValue());
		assertThat(providerRoom.isPresent(), is(true));
		providerRoom.ifPresent(providerRoom1 -> assertThat(providerRoom1.getUuid(), is(QUEUE_ROOM_UUID)));
	}
	
	@Test
	public void shouldReturnNullForRetiredRoomProviderMap() {
		Optional<RoomProviderMap> queueRoom = dao.get(RETIRED_ROOM_PROVIDER_MAP__UUID);
		assertThat(queueRoom.isPresent(), is(false));
	}
	
	@Test
	public void shouldCreateNewRoomProviderMap() {
		QueueRoom queueRoom = queueRoomDao.get(QUEUE_ROOM_UUID).get();
		Provider provider = Context.getProviderService().getProviderByUuid(PROVIDER_UUID);
		
		RoomProviderMap providerRoom = new RoomProviderMap();
		providerRoom.setUuid(NEW_ROOM_PROVIDER_MAP_UUID);
		providerRoom.setQueueRoom(queueRoom);
		providerRoom.setProvider(provider);
		
		RoomProviderMap result = dao.createOrUpdate(providerRoom);
		assertThat(result, notNullValue());
		assertThat(result.getUuid(), is(NEW_ROOM_PROVIDER_MAP_UUID));
		
		//Get the saved provider's room version
		Optional<RoomProviderMap> newlyCreated = dao.get(NEW_ROOM_PROVIDER_MAP_UUID);
		assertThat(newlyCreated.isPresent(), is(true));
		newlyCreated.ifPresent(createdQueueRoom -> {
			assertThat(createdQueueRoom.getUuid(), is(NEW_ROOM_PROVIDER_MAP_UUID));
			assertThat(createdQueueRoom.getProvider().getUuid(), is(PROVIDER_UUID));
		});
	}
	
	@Test
	public void shouldUpdateRoomProviderMap() {
		//Get saved queue room
		Optional<RoomProviderMap> persistedRoom = dao.get(ROOM_PROVIDER_MAP_UUID);
		assertThat(persistedRoom.isPresent(), is(true));
		persistedRoom.ifPresent(queueDb -> {
			assertThat(queueDb.getUuid(), is(ROOM_PROVIDER_MAP_UUID));
			assertThat(queueDb.getProvider().getUuid(), is(PROVIDER_UUID));
		});
		//Update Queue room name
		RoomProviderMap toUpdate = persistedRoom.get();
		Provider provider = Context.getProviderService().getProviderByUuid(UPDATE_PROVIDER_UUID);
		toUpdate.setProvider(provider);
		dao.createOrUpdate(toUpdate);
		//Verify the update operation
		Optional<RoomProviderMap> updatedRoom = dao.get(toUpdate.getUuid());
		assertThat(updatedRoom.isPresent(), is(true));
		updatedRoom.ifPresent(pr -> assertThat(pr.getProvider().getUuid(), is(UPDATE_PROVIDER_UUID)));
	}
	
	@Test
	public void shouldDeleteRoomProviderMapByUuid() {
		dao.delete(ROOM_PROVIDER_MAP_UUID);
		
		Optional<RoomProviderMap> result = dao.get(ROOM_PROVIDER_MAP_UUID);
		//verify delete operation
		assertThat(result.isPresent(), is(false));
	}
	
	@Test
	public void shouldDeleteRoomProviderMapByEntity() {
		dao.get(ROOM_PROVIDER_MAP_UUID).ifPresent((queue) -> dao.delete(queue));
		
		Optional<RoomProviderMap> result = dao.get(ROOM_PROVIDER_MAP_UUID);
		//verify delete operation
		assertThat(result.isPresent(), is(false));
	}
	
	@Test
	public void shouldFindAllRoomProviderMaps() {
		Collection<RoomProviderMap> room = dao.findAll();
		assertThat(room.isEmpty(), is(false));
		assertThat(room, hasSize(2));
	}
	
	@Test
	public void shouldFindAllQueueRoomProviderMapsIncludingRetired() {
		Collection<RoomProviderMap> rooms = dao.findAll(true);
		assertThat(rooms.isEmpty(), is(false));
		assertThat(rooms, hasSize(3));
	}
	
	@Test
	public void shouldFindByQueueRoom() {
		Optional<QueueRoom> queueRoom = queueRoomDao.get(QUEUE_ROOM_UUID);
		
		List<RoomProviderMap> result = dao.getRoomProvider(null, queueRoom.get());
		
		assertThat(result, notNullValue());
		assertThat(result, hasSize(1));
		result.forEach(room -> assertThat(room.getQueueRoom().getUuid(), is(QUEUE_ROOM_UUID)));
	}
	
	@Test
	public void shouldFindByProvider() {
		Provider provider = Context.getProviderService().getProviderByUuid(PROVIDER_UUID);
		
		List<RoomProviderMap> result = dao.getRoomProvider(provider, null);
		
		assertThat(result, notNullValue());
		assertThat(result, hasSize(1));
		result.forEach(room -> assertThat(room.getProvider().getUuid(), is(PROVIDER_UUID)));
	}
}
