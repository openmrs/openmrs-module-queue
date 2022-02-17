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
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class QueueEntryDaoTest extends BaseModuleContextSensitiveTest {
	
	private static final String QUEUE_ENTRY_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252cc6";
	
	private static final String VOIDED_QUEUE_ENTRY_UUID = "4gb8fe43-2813-4kbc-80dc-2e5d30252ff0";
	
	private static final String NEW_QUEUE_ENTRY_UUID = "5kb8fe43-2813-4kbc-80dc-2e5d30252cc87";
	
	private static final String PATIENT_UUID = "90b38324-e2fd-4feb-95b7-9e9a2a8876fg";
	
	private static final String QUEUE_UUID = "3eb7fe43-2813-4kbc-80dc-2e5d30252bb5";
	
	private static final String QUEUE_STATUS_CONCEPT_UUID = "31b910bd-298c-4ecf-a632-661ae2f4460y";
	
	private static final String QUEUE_PRIORITY_CONCEPT_UUID = "90b910bd-298c-4ecf-a632-661ae2f446op";
	
	private static final List<String> QUEUE_INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_initialDataset.xml");
	
	private static final String QUEUE_ENTRY_STATUS = "Waiting for service";
	
	private static final String BAD_QUEUE_ENTRY_STATUS = "Bad Waiting for service";
	
	@Autowired
	@Qualifier("queue.QueueEntryDao")
	private QueueEntryDao<QueueEntry> dao;
	
	@Autowired
	@Qualifier("queue.QueueDao")
	private QueueDao<Queue> queueDao;
	
	@Before
	public void setup() {
		QUEUE_INITIAL_DATASET_XML.forEach(this::executeDataSet);
	}
	
	@Test
	public void shouldGetQueueEntryByUuid() {
		Optional<QueueEntry> queueEntry = dao.get(QUEUE_ENTRY_UUID);
		
		assertThat(queueEntry, notNullValue());
		assertThat(queueEntry.isPresent(), is(true));
		queueEntry.ifPresent(queueEntry1 -> assertThat(queueEntry1.getUuid(), is(QUEUE_ENTRY_UUID)));
	}
	
	@Test
	public void shouldReturnNullForRetiredQueueEntry() {
		Optional<QueueEntry> queueEntry = dao.get(VOIDED_QUEUE_ENTRY_UUID);
		assertThat(queueEntry.isPresent(), is(false));
	}
	
	@Test
	public void shouldCreateNewQueueEntry() {
		//Verify queue created
		Optional<Queue> optionalQueue = queueDao.get(QUEUE_UUID);
		assertThat(optionalQueue.isPresent(), is(true));
		
		QueueEntry queueEntry = new QueueEntry();
		queueEntry.setUuid(NEW_QUEUE_ENTRY_UUID);
		queueEntry.setQueue(optionalQueue.get());
		queueEntry.setStartedAt(new Date());
		queueEntry.setSortWeight(Double.parseDouble("0"));
		
		verifyQueueEntryPatientProperty(queueEntry);
		verifyQueueEntryConceptStatusProperty(queueEntry);
		verifyQueueEntryConceptPriorityProperty(queueEntry);
		
		QueueEntry result = dao.createOrUpdate(queueEntry);
		assertThat(result, notNullValue());
		assertThat(result.getUuid(), is(NEW_QUEUE_ENTRY_UUID));
		
		//Get the saved queue version
		Optional<QueueEntry> newlyCreatedQueue = dao.get(NEW_QUEUE_ENTRY_UUID);
		assertThat(newlyCreatedQueue.isPresent(), is(true));
		newlyCreatedQueue.ifPresent(newlyCreatedQueueEntry -> {
			assertThat(newlyCreatedQueueEntry.getUuid(), is(NEW_QUEUE_ENTRY_UUID));
			assertThat(newlyCreatedQueueEntry.getPatient(), notNullValue());
			assertThat(newlyCreatedQueueEntry.getPatient().getUuid(), is(PATIENT_UUID));
			assertThat(newlyCreatedQueueEntry.getStatus().getUuid(), is(QUEUE_STATUS_CONCEPT_UUID));
		});
	}
	
	@Test
	public void shouldUpdateQueueEntry() {
		//Get saved queue entry
		Optional<QueueEntry> queueEntryFromDatabase = dao.get(QUEUE_ENTRY_UUID);
		assertThat(queueEntryFromDatabase.isPresent(), is(true));
		//verify queue entry values
		queueEntryFromDatabase.ifPresent(queueEntryDb -> {
			assertThat(queueEntryDb.getUuid(), is(QUEUE_ENTRY_UUID));
			assertThat(queueEntryDb.getPriorityComment(), nullValue());
		});
		
		//Update Queue name
		QueueEntry queueEntry = queueEntryFromDatabase.get();
		queueEntry.setPriorityComment("Priority comment");
		dao.createOrUpdate(queueEntry);
		//Verify the update operation
		Optional<QueueEntry> updatedQueue = dao.get(queueEntry.getUuid());
		assertThat(updatedQueue.isPresent(), is(true));
		updatedQueue.ifPresent(revisedQueue -> assertThat(revisedQueue.getPriorityComment(), is("Priority comment")));
	}
	
	@Test
	public void shouldFindAllQueueEntries() {
		Collection<QueueEntry> queues = dao.findAll();
		assertThat(queues.isEmpty(), is(false));
		assertThat(queues, hasSize(1));
	}
	
	@Test
	public void shouldFindAllQueueEntriesIncludingRetired() {
		Collection<QueueEntry> queues = dao.findAll(true);
		assertThat(queues.isEmpty(), is(false));
		assertThat(queues, hasSize(2));
	}
	
	@Test
	public void shouldDeleteQueueEntryByUuid() {
		dao.delete(QUEUE_ENTRY_UUID);
		
		Optional<QueueEntry> result = dao.get(QUEUE_ENTRY_UUID);
		//verify delete operation
		assertThat(result.isPresent(), is(false));
	}
	
	@Test
	public void shouldDeleteQueueEntryByEntity() {
		dao.get(QUEUE_ENTRY_UUID).ifPresent((queueEntry) -> dao.delete(queueEntry));
		
		Optional<QueueEntry> result = dao.get(QUEUE_ENTRY_UUID);
		//verify delete operation
		assertThat(result.isPresent(), is(false));
	}
	
	void verifyQueueEntryPatientProperty(QueueEntry queueEntry) {
		Patient patient = Context.getPatientService().getPatientByUuid(PATIENT_UUID);
		assertThat(patient, notNullValue());
		assertThat(patient.getUuid(), is(PATIENT_UUID));
		queueEntry.setPatient(patient);
	}
	
	void verifyQueueEntryConceptStatusProperty(QueueEntry queueEntry) {
		Concept conceptStatus = Context.getConceptService().getConceptByUuid(QUEUE_STATUS_CONCEPT_UUID);
		assertThat(conceptStatus, notNullValue());
		assertThat(conceptStatus.getUuid(), is(QUEUE_STATUS_CONCEPT_UUID));
		queueEntry.setStatus(conceptStatus);
	}
	
	void verifyQueueEntryConceptPriorityProperty(QueueEntry queueEntry) {
		Concept conceptQueuePriority = Context.getConceptService().getConceptByUuid(QUEUE_PRIORITY_CONCEPT_UUID);
		assertThat(conceptQueuePriority, notNullValue());
		assertThat(conceptQueuePriority.getUuid(), is(QUEUE_PRIORITY_CONCEPT_UUID));
		queueEntry.setPriority(conceptQueuePriority);
	}
	
	@Test
	public void shouldSearchQueueEntriesByStatus() {
		Collection<QueueEntry> queueEntries = dao.SearchQueueEntries(QUEUE_ENTRY_STATUS, false);
		
		assertThat(queueEntries.isEmpty(), is(false));
		assertThat(queueEntries, hasSize(1));
		queueEntries.forEach(queueEntry -> {
			assertThat(queueEntry.getStatus(), notNullValue());
			assertThat(queueEntry.getStatus().getName().getName(), is(QUEUE_ENTRY_STATUS));
		});
	}
	
	@Test
	public void shouldSearchQueueEntriesByStatusIncludingVoidedQueueEntries() {
		Collection<QueueEntry> queueEntries = dao.SearchQueueEntries(QUEUE_ENTRY_STATUS, true);
		
		assertThat(queueEntries.isEmpty(), is(false));
		assertThat(queueEntries, hasSize(2));
		queueEntries.forEach(queueEntry -> {
			assertThat(queueEntry.getStatus(), notNullValue());
			assertThat(queueEntry.getStatus().getName().getName(), is(QUEUE_ENTRY_STATUS));
		});
	}
	
	@Test
	public void shouldCountQueueEntriesByStatus() {
		Long queueEntriesCountByStatusCount = dao.getQueueEntriesCountByStatus(QUEUE_ENTRY_STATUS);
		
		assertThat(queueEntriesCountByStatusCount, notNullValue());
		assertThat(queueEntriesCountByStatusCount, is(1L));
	}
	
	@Test
	public void shouldZeroCountQueueEntriesByBadStatus() {
		Long queueEntriesCountByStatusCount = dao.getQueueEntriesCountByStatus(BAD_QUEUE_ENTRY_STATUS);
		
		assertThat(queueEntriesCountByStatusCount, notNullValue());
		assertThat(queueEntriesCountByStatusCount, is(0L));
	}
}
