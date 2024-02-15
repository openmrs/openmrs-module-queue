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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.search.QueueRoomSearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueRoom;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class QueueRoomDaoTest extends BaseModuleContextSensitiveTest {
	
	private static final List<String> QUEUE_ROOM_INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueRoomDaoTest_initialDataset.xml");
	
	private static final String QUEUE_ROOM_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252cc6";
	
	private static final String RETIRED_QUEUE_ROOM_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252cc8";
	
	private static final String NEW_QUEUE_ROOM_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252c10";
	
	private static final String NEW_QUEUE_ROOM_NAME = "Triage Room 1";
	
	private static final String QUEUE_UUID = "3eb7fe43-2813-4kbc-80dc-2e5d30252bb5";
	
	private static final String LOCATION_UUID = "d0938432-1691-11df-97a5-7038c098";
	
	@Autowired
	@Qualifier("queueRoomDao")
	private QueueRoomDao dao;
	
	@Autowired
	private QueueServicesWrapper services;
	
	@Before
	public void setup() {
		QUEUE_ROOM_INITIAL_DATASET_XML.forEach(this::executeDataSet);
	}
	
	@Test
	public void shouldGetQueueRoomById() {
		Optional<QueueRoom> queueRoom = dao.get(1);
		
		assertThat(queueRoom, notNullValue());
		assertThat(queueRoom.isPresent(), is(true));
		queueRoom.ifPresent(queueRoom1 -> assertThat(queueRoom1.getId(), is(1)));
	}
	
	@Test
	public void shouldGetQueueRoomByUuid() {
		Optional<QueueRoom> queueRoom = dao.get(QUEUE_ROOM_UUID);
		
		assertThat(queueRoom, notNullValue());
		assertThat(queueRoom.isPresent(), is(true));
		queueRoom.ifPresent(queueRoom1 -> assertThat(queueRoom1.getUuid(), is(QUEUE_ROOM_UUID)));
	}
	
	@Test
	public void shouldReturnNullForRetiredQueueRoom() {
		Optional<QueueRoom> queueRoom = dao.get(RETIRED_QUEUE_ROOM_UUID);
		assertThat(queueRoom.isPresent(), is(false));
	}
	
	@Test
	public void shouldCreateNewQueueRoom() {
		QueueRoom queueRoom = new QueueRoom();
		queueRoom.setUuid(NEW_QUEUE_ROOM_UUID);
		queueRoom.setName(NEW_QUEUE_ROOM_NAME);
		queueRoom.setQueue(services.getQueueService().getQueueByUuid(QUEUE_UUID).get());
		
		QueueRoom result = dao.createOrUpdate(queueRoom);
		assertThat(result, notNullValue());
		assertThat(result.getUuid(), is(NEW_QUEUE_ROOM_UUID));
		
		//Get the saved queue room version
		Optional<QueueRoom> newlyCreatedQueueRoom = dao.get(NEW_QUEUE_ROOM_UUID);
		assertThat(newlyCreatedQueueRoom.isPresent(), is(true));
		newlyCreatedQueueRoom.ifPresent(createdQueueRoom -> {
			assertThat(createdQueueRoom.getUuid(), is(NEW_QUEUE_ROOM_UUID));
			assertThat(createdQueueRoom.getName(), is(NEW_QUEUE_ROOM_NAME));
		});
	}
	
	@Test
	public void shouldUpdateQueueRoom() {
		//Get saved queue room
		Optional<QueueRoom> persistedRoom = dao.get(QUEUE_ROOM_UUID);
		assertThat(persistedRoom.isPresent(), is(true));
		persistedRoom.ifPresent(queueDb -> {
			assertThat(queueDb.getUuid(), is(QUEUE_ROOM_UUID));
			assertThat(queueDb.getName(), is(NEW_QUEUE_ROOM_NAME));
		});
		//Update Queue room name
		QueueRoom toUpdate = persistedRoom.get();
		toUpdate.setName("Triage New Room");
		dao.createOrUpdate(toUpdate);
		//Verify the update operation
		Optional<QueueRoom> updatedQueue = dao.get(toUpdate.getUuid());
		assertThat(updatedQueue.isPresent(), is(true));
		updatedQueue.ifPresent(revisedQueue -> assertThat(revisedQueue.getName(), is("Triage New Room")));
	}
	
	@Test
	public void shouldFindQueueRoomByLocation() {
		QueueRoomSearchCriteria criteria = new QueueRoomSearchCriteria();
		Location location = services.getLocationService().getLocationByUuid(LOCATION_UUID);
		criteria.setLocations(Collections.singletonList(location));
		List<QueueRoom> roomsByLocation = dao.getQueueRooms(criteria);
		assertThat(roomsByLocation, notNullValue());
		assertThat(roomsByLocation, hasSize(2));
		roomsByLocation.forEach(room -> assertThat(room.getQueue().getLocation(), equalTo(location)));
	}
	
	@Test
	public void shouldFindQueueRoomByQueue() {
		QueueRoomSearchCriteria criteria = new QueueRoomSearchCriteria();
		Queue queue = services.getQueueService().getQueueByUuid(QUEUE_UUID).get();
		criteria.setQueues(Collections.singletonList(queue));
		List<QueueRoom> roomsByQueue = dao.getQueueRooms(criteria);
		assertThat(roomsByQueue, notNullValue());
		assertThat(roomsByQueue, hasSize(2));
		roomsByQueue.forEach(room -> assertThat(room.getQueue(), equalTo(queue)));
	}
	
	@Test
	public void shouldDeleteQueueRoomByUuid() {
		dao.delete(QUEUE_ROOM_UUID);
		Optional<QueueRoom> result = dao.get(QUEUE_ROOM_UUID);
		assertThat(result.isPresent(), is(false));
	}
	
	@Test
	public void shouldDeleteQueueRoomByEntity() {
		dao.get(QUEUE_ROOM_UUID).ifPresent((queue) -> dao.delete(queue));
		Optional<QueueRoom> result = dao.get(QUEUE_ROOM_UUID);
		assertThat(result.isPresent(), is(false));
	}
	
	@Test
	public void shouldFindAllQueueRooms() {
		Collection<QueueRoom> queueRooms = dao.findAll();
		assertThat(queueRooms.isEmpty(), is(false));
		assertThat(queueRooms, hasSize(2));
	}
	
	@Test
	public void shouldFindAllQueueRoomsIncludingRetired() {
		Collection<QueueRoom> queueRooms = dao.findAll(true);
		assertThat(queueRooms.isEmpty(), is(false));
		assertThat(queueRooms, hasSize(3));
	}
}
