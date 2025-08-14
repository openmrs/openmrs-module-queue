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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.search.QueueSearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class QueueDaoTest extends BaseModuleContextSensitiveTest {
	
	private static final List<String> QUEUE_INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_queueWaitTimeInitialDataset.xml");
	
	private static final String QUEUE_UUID = "3eb7fe43-2813-4kbc-80dc-2e5d30252bb5";
	
	private static final String RETIRED_QUEUE_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252bb9";
	
	public static final String EMPTY_QUEUE_UUID = "5ob8gj90-9090-4kbc-80dc-2e5d30252bb3";
	
	private static final String NEW_QUEUE_UUID = "45b9fe43-2813-4kbc-80dc-2e5d30290iik";
	
	private static final String NEW_QUEUE_NAME = "Test triage queue";
	
	private static final String NEW_QUEUE_LOCATION_UUID = "d0938432-1691-11df-97a5-7038c098";
	
	private static final String CONCEPT_UUID = "67b910bd-298c-4ecf-a632-661ae2f446op";
	
	@Autowired
	@Qualifier("queueDao")
	private QueueDao dao;
	
	@Autowired
	private QueueServicesWrapper services;
	
	private QueueSearchCriteria criteria;
	
	@Before
	public void setup() {
		QUEUE_INITIAL_DATASET_XML.forEach(this::executeDataSet);
		criteria = new QueueSearchCriteria();
	}
	
	@Test
	public void shouldGetQueueByUuid() {
		Optional<Queue> queue = dao.get(QUEUE_UUID);
		assertThat(queue, notNullValue());
		assertThat(queue.isPresent(), is(true));
		queue.ifPresent(queue1 -> assertThat(queue1.getUuid(), is(QUEUE_UUID)));
	}
	
	@Test
	public void shouldReturnNullForRetiredQueue() {
		Optional<Queue> queue = dao.get(RETIRED_QUEUE_UUID);
		assertThat(queue.isPresent(), is(false));
	}
	
	@Test
	public void shouldCreateNewQueue() {
		Queue queue = new Queue();
		queue.setUuid(NEW_QUEUE_UUID);
		queue.setName(NEW_QUEUE_NAME);
		queue.setService(Context.getConceptService().getConceptByUuid(CONCEPT_UUID));
		queue.setLocation(Context.getLocationService().getLocationByUuid(NEW_QUEUE_LOCATION_UUID));
		
		Queue result = dao.createOrUpdate(queue);
		assertThat(result, notNullValue());
		assertThat(result.getUuid(), is(NEW_QUEUE_UUID));
		
		//Get the saved queue version
		Optional<Queue> newlyCreatedQueue = dao.get(NEW_QUEUE_UUID);
		assertThat(newlyCreatedQueue.isPresent(), is(true));
		newlyCreatedQueue.ifPresent(createdQueue -> {
			assertThat(createdQueue.getUuid(), is(NEW_QUEUE_UUID));
			assertThat(createdQueue.getLocation(), notNullValue());
			assertThat(createdQueue.getLocation().getUuid(), is(NEW_QUEUE_LOCATION_UUID));
		});
	}
	
	@Test
	public void shouldUpdateQueue() {
		//Get saved queue
		Optional<Queue> queueFromDB = dao.get(QUEUE_UUID);
		assertThat(queueFromDB.isPresent(), is(true));
		queueFromDB.ifPresent(queueDb -> {
			assertThat(queueDb.getUuid(), is(QUEUE_UUID));
			assertThat(queueDb.getName(), is("Triage Test Queue"));
		});
		//Update Queue name
		Queue queue = queueFromDB.get();
		queue.setName("Updated queue name");
		dao.createOrUpdate(queue);
		//Verify the update operation
		Optional<Queue> updatedQueue = dao.get(queue.getUuid());
		assertThat(updatedQueue.isPresent(), is(true));
		updatedQueue.ifPresent(revisedQueue -> assertThat(revisedQueue.getName(), is("Updated queue name")));
	}
	
	@Test
	public void shouldFindAllQueues() {
		Collection<Queue> queues = dao.findAll();
		assertThat(queues.isEmpty(), is(false));
		assertThat(queues, hasSize(3));
	}
	
	@Test
	public void shouldFindAllQueuesIncludingRetired() {
		Collection<Queue> queues = dao.findAll(true);
		assertThat(queues.isEmpty(), is(false));
		assertThat(queues, hasSize(4));
	}
	
	@Test
	public void shouldDeleteQueueByUuid() {
		dao.delete(EMPTY_QUEUE_UUID);
		Optional<Queue> result = dao.get(EMPTY_QUEUE_UUID);
		assertThat(result.isPresent(), is(false));
	}
	
	@Test
	public void shouldDeleteQueueByEntity() {
		dao.get(EMPTY_QUEUE_UUID).ifPresent((queue) -> dao.delete(queue));
		Optional<Queue> result = dao.get(EMPTY_QUEUE_UUID);
		assertThat(result.isPresent(), is(false));
	}
	
	@Test
	public void shouldGetQueuesByLocation() {
		Location location1 = services.getLocationService().getLocation(1);
		Location location3 = services.getLocationService().getLocation(3);
		assertResults(criteria, 1, 3, 4);
		criteria.setLocations(Collections.emptyList());
		assertResults(criteria);
		criteria.setLocations(Collections.singletonList(location1));
		assertResults(criteria, 1, 4);
		criteria.setLocations(Collections.singletonList(location3));
		assertResults(criteria, 3);
		criteria.setLocations(Arrays.asList(location1, location3));
		assertResults(criteria, 1, 3, 4);
	}
	
	@Test
	public void shouldGetQueuesByService() {
		Concept concept1 = services.getConceptService().getConcept(2001);
		Concept concept2 = services.getConceptService().getConcept(2002);
		assertResults(criteria, 1, 3, 4);
		criteria.setServices(Collections.emptyList());
		assertResults(criteria);
		criteria.setServices(Collections.singletonList(concept1));
		assertResults(criteria, 1);
		criteria.setServices(Collections.singletonList(concept2));
		assertResults(criteria, 3, 4);
		criteria.setServices(Arrays.asList(concept1, concept2));
		assertResults(criteria, 1, 3, 4);
	}
	
	@Test
	public void shouldReturnQueuesInAscendingOrderByName() {
		List<Queue> queues = dao.getQueues(criteria);
		assertThat(queues, notNullValue());
		assertThat(queues.size(), is(greaterThanOrEqualTo(2)));
		
		String previousName = "";
		for (Queue q : queues) {
			assertThat(q.getName().compareTo(previousName), is(greaterThanOrEqualTo(0)));
			previousName = q.getName();
		}
	}
	
	@Test
	public void shouldGetQueuesByIncludeRetired() {
		assertResults(criteria, 1, 3, 4);
		criteria.setIncludeRetired(true);
		assertResults(criteria, 1, 2, 3, 4);
	}
	
	/**
	 * Utility method that tests criteria against both DAO methods to getQueueEntries and
	 * getCountOfQueueEntries
	 */
	private void assertResults(QueueSearchCriteria criteria, Integer... queueIds) {
		List<Queue> queues = dao.getQueues(criteria);
		assertThat(queues, hasSize(queueIds.length));
		for (Integer queueId : queueIds) {
			Queue expected = dao.get(queueId).orElse(null);
			assertThat(expected, notNullValue());
			assertThat(queues.contains(expected), is(true));
		}
	}
}
