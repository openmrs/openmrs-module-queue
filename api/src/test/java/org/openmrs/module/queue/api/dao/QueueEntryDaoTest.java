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
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.utils.QueueUtils;
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
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/VisitQueueEntryDaoTest_visitInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_initialDataset.xml");
	
	@Autowired
	@Qualifier("queueEntryDao")
	private QueueEntryDao<QueueEntry> dao;
	
	@Autowired
	private QueueServicesWrapper services;
	
	private QueueEntrySearchCriteria criteria;
	
	@Before
	public void setup() {
		QUEUE_INITIAL_DATASET_XML.forEach(this::executeDataSet);
		criteria = new QueueEntrySearchCriteria();
		criteria.setIsEnded(null);
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
		Optional<Queue> optionalQueue = services.getQueueService().getQueueByUuid(QUEUE_UUID);
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
		assertThat(queues, hasSize(4));
	}
	
	@Test
	public void shouldFindAllQueueEntriesIncludingRetired() {
		Collection<QueueEntry> queues = dao.findAll(true);
		assertThat(queues.isEmpty(), is(false));
		assertThat(queues, hasSize(5));
	}
	
	@Test
	public void shouldDeleteQueueEntryByUuid() {
		dao.delete(QUEUE_ENTRY_UUID);
		Optional<QueueEntry> result = dao.get(QUEUE_ENTRY_UUID);
		assertThat(result.isPresent(), is(false));
	}
	
	@Test
	public void shouldDeleteQueueEntryByEntity() {
		dao.get(QUEUE_ENTRY_UUID).ifPresent((queueEntry) -> dao.delete(queueEntry));
		Optional<QueueEntry> result = dao.get(QUEUE_ENTRY_UUID);
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
	public void shouldSearchAndCountQueueEntriesByQueue() {
		Queue queue1 = services.getQueueService().getQueueById(1).orElse(null);
		Queue queue2 = services.getQueueService().getQueueById(2).orElse(null);
		Queue queue3 = services.getQueueService().getQueueById(3).orElse(null);
		assertNumberOfResults(criteria, 4);
		criteria.setQueues(Collections.emptyList());
		assertNumberOfResults(criteria, 0);
		criteria.setQueues(Collections.singletonList(queue1));
		assertResults(criteria, 1);
		criteria.setQueues(Collections.singletonList(queue2));
		assertResults(criteria, 2);
		criteria.setQueues(Collections.singletonList(queue3));
		assertResults(criteria, 3, 4);
		criteria.setQueues(Arrays.asList(queue2, queue3));
		assertResults(criteria, 2, 3, 4);
		criteria.setIncludedVoided(true);
		assertResults(criteria, 2, 3, 4, 10);
	}
	
	@Test
	public void shouldSearchAndCountQueueEntriesByQueueLocation() {
		Location location1 = services.getLocationService().getLocation(1);
		Location location3 = services.getLocationService().getLocation(3);
		assertNumberOfResults(criteria, 4);
		criteria.setLocations(Collections.emptyList());
		assertNumberOfResults(criteria, 0);
		criteria.setLocations(Collections.singletonList(location1));
		assertResults(criteria, 1, 2);
		criteria.setLocations(Collections.singletonList(location3));
		assertResults(criteria, 3, 4);
		criteria.setLocations(Arrays.asList(location1, location3));
		assertResults(criteria, 1, 2, 3, 4);
		criteria.setIncludedVoided(true);
		assertResults(criteria, 1, 2, 3, 4, 10);
	}
	
	@Test
	public void shouldSearchAndCountQueueEntriesByQueueService() {
		Concept service1 = services.getConceptService().getConcept(2001);
		Concept service2 = services.getConceptService().getConcept(2002);
		assertNumberOfResults(criteria, 4);
		criteria.setServices(Collections.emptyList());
		assertNumberOfResults(criteria, 0);
		criteria.setServices(Collections.singletonList(service1));
		assertResults(criteria, 1, 2);
		criteria.setServices(Collections.singletonList(service2));
		assertResults(criteria, 3, 4);
		criteria.setServices(Arrays.asList(service1, service2));
		assertResults(criteria, 1, 2, 3, 4);
		criteria.setIncludedVoided(true);
		assertResults(criteria, 1, 2, 3, 4, 10);
	}
	
	@Test
	public void shouldSearchAndCountQueueEntriesByPatient() {
		Patient patient1 = services.getPatientService().getPatient(100);
		Patient patient2 = services.getPatientService().getPatient(2);
		assertNumberOfResults(criteria, 4);
		criteria.setPatient(patient1);
		assertResults(criteria, 1, 2, 3);
		criteria.setPatient(patient2);
		assertResults(criteria, 4);
	}
	
	@Test
	public void shouldSearchAndCountQueueEntriesByVisit() {
		Visit visit1 = services.getVisitService().getVisit(101);
		Visit visit2 = services.getVisitService().getVisit(102);
		assertNumberOfResults(criteria, 4);
		criteria.setVisit(visit1);
		assertResults(criteria, 1, 2);
		criteria.setVisit(visit2);
		assertResults(criteria, 3);
		criteria.setVisit(null);
		criteria.setHasVisit(true);
		assertResults(criteria, 1, 2, 3);
		criteria.setHasVisit(false);
		assertResults(criteria, 4);
	}
	
	@Test
	public void shouldSearchAndCountQueueEntriesByPastVisit() {
		Visit visit3 = services.getVisitService().getVisit(103);


		// criteria.setIsEnded(Boolean.TRUE);
		assertNumberOfResults(criteria, 5);
		criteria.setVisit(visit3);
		assertResults(criteria, 11);
	}

	@Test
	public void shouldSearchCountQueueEntriesFromPastVisit() {
		// this is an old QueueEntry which has ended_at value not null
		QueueEntry oldQueueEntry = dao.get(11).orElse(null);
		assertThat(oldQueueEntry, notNullValue());
		assertThat(oldQueueEntry.getEndedAt(), notNullValue());

		QueueEntrySearchCriteria qeSearchCriteria = new QueueEntrySearchCriteria();
		Patient patient = services.getPatientService().getPatient(100);
		Queue queue = services.getQueueService().getQueueById(1).orElse(null);
		qeSearchCriteria.setQueues(Collections.singletonList(queue));
		qeSearchCriteria.setPatient(patient);
		List<QueueEntry> queueEntries = dao.getQueueEntries(qeSearchCriteria);
		assertThat(queueEntries, hasSize(1));
		// the only QueueEntry found is the one that has not end date (), queue_entry_id="12"
		assertThat(queueEntries.get(0).getQueueEntryId(), is(12));
		assertThat(queueEntries.get(0).getEndedAt(), nullValue());
		qeSearchCriteria.setIsEnded(null);
		queueEntries = dao.getQueueEntries(qeSearchCriteria);
		// now it finds all patient's queue entries on this queue including the old queue entry that has ended_at not null
		assertThat(queueEntries, hasSize(3));
		assertThat(queueEntries.contains(oldQueueEntry), is(true));
	}
	
	@Test
	public void shouldSearchAndCountQueueEntriesByPriority() {
		Concept priority1 = services.getConceptService().getConcept(1001);
		Concept priority2 = services.getConceptService().getConcept(1002);
		Concept nonPriority = services.getConceptService().getConcept(3001);
		assertNumberOfResults(criteria, 4);
		criteria.setPriorities(Collections.emptyList());
		assertNumberOfResults(criteria, 0);
		criteria.setPriorities(Collections.singletonList(nonPriority));
		assertNumberOfResults(criteria, 0);
		criteria.setPriorities(Collections.singletonList(priority1));
		assertResults(criteria, 1, 3, 4);
		criteria.setPriorities(Collections.singletonList(priority2));
		assertResults(criteria, 2);
		criteria.setPriorities(Arrays.asList(priority1, priority2));
		assertResults(criteria, 1, 2, 3, 4);
		criteria.setIncludedVoided(true);
		assertResults(criteria, 1, 2, 3, 4, 10);
	}
	
	@Test
	public void shouldSearchAndCountQueueEntriesByStatus() {
		Concept status1 = services.getConceptService().getConcept(3001);
		Concept status2 = services.getConceptService().getConcept(3002);
		Concept nonStatus = services.getConceptService().getConcept(1001);
		assertNumberOfResults(criteria, 4);
		criteria.setStatuses(Collections.emptyList());
		assertNumberOfResults(criteria, 0);
		criteria.setStatuses(Collections.singletonList(nonStatus));
		assertNumberOfResults(criteria, 0);
		criteria.setStatuses(Collections.singletonList(status1));
		assertResults(criteria, 1);
		criteria.setStatuses(Collections.singletonList(status2));
		assertResults(criteria, 2, 3, 4);
		criteria.setStatuses(Arrays.asList(status1, status2));
		assertResults(criteria, 1, 2, 3, 4);
		criteria.setIncludedVoided(true);
		assertResults(criteria, 1, 2, 3, 4, 10);
	}
	
	@Test
	public void shouldSearchAndCountQueueEntriesByLocationWaitingFor() {
		Location location1 = services.getLocationService().getLocation(1);
		Location location3 = services.getLocationService().getLocation(3);
		assertNumberOfResults(criteria, 4);
		criteria.setLocationsWaitingFor(Collections.emptyList());
		assertResults(criteria, 3); // Empty list = entries with null value
		criteria.setLocationsWaitingFor(Collections.singletonList(location1));
		assertResults(criteria, 4);
		criteria.setLocationsWaitingFor(Collections.singletonList(location3));
		assertResults(criteria, 1, 2);
		criteria.setLocationsWaitingFor(Arrays.asList(location1, location3));
		assertResults(criteria, 1, 2, 4);
	}
	
	@Test
	public void shouldSearchAndCountQueueEntriesByProviderWaitingFor() {
		Provider provider1 = services.getProviderService().getProvider(1);
		assertNumberOfResults(criteria, 4);
		criteria.setProvidersWaitingFor(Collections.emptyList());
		assertResults(criteria, 2, 4); // Empty list = entries with null value
		criteria.setProvidersWaitingFor(Collections.singletonList(provider1));
		assertResults(criteria, 1, 3);
	}
	
	@Test
	public void shouldSearchAndCountQueueEntriesByQueueComingFrom() {
		Queue queue1 = services.getQueueService().getQueueById(1).orElse(null);
		assertNumberOfResults(criteria, 4);
		criteria.setQueuesComingFrom(Collections.emptyList());
		assertResults(criteria, 1, 3, 4);
		criteria.setQueuesComingFrom(Collections.singletonList(queue1));
		assertResults(criteria, 2);
	}
	
	@Test
	// 2022-02-02 16:40:56.0, 2022-02-02 18:40:56.0, 2022-02-03 16:40:56.0, 2022-03-02 16:40:56.0
	public void shouldSearchAndCountQueueEntriesStartedOnOrAfterDate() {
		assertNumberOfResults(criteria, 4);
		criteria.setStartedOnOrAfter(date("2022-02-02 16:40:56"));
		assertResults(criteria, 1, 2, 3, 4);
		criteria.setStartedOnOrAfter(date("2022-02-02 16:40:57"));
		assertResults(criteria, 2, 3, 4);
		criteria.setStartedOnOrAfter(date("2022-02-02 18:40:56"));
		assertResults(criteria, 2, 3, 4);
		criteria.setStartedOnOrAfter(date("2022-02-02 18:40:57"));
		assertResults(criteria, 3, 4);
		criteria.setStartedOnOrAfter(date("2022-02-03 16:40:56"));
		assertResults(criteria, 3, 4);
		criteria.setStartedOnOrAfter(date("2022-02-03 16:40:57"));
		assertResults(criteria, 4);
		criteria.setStartedOnOrAfter(date("2022-03-02 16:40:56"));
		assertResults(criteria, 4);
		criteria.setStartedOnOrAfter(date("2022-03-02 16:40:57"));
		assertNumberOfResults(criteria, 0);
	}
	
	@Test
	// 2022-02-02 16:40:56.0, 2022-02-02 18:40:56.0, 2022-02-03 16:40:56.0, 2022-03-02 16:40:56.0
	public void shouldSearchAndCountQueueEntriesStartedOnOrBeforeDate() {
		assertNumberOfResults(criteria, 4);
		criteria.setStartedOnOrBefore(date("2022-02-02 16:40:55"));
		assertNumberOfResults(criteria, 0);
		criteria.setStartedOnOrBefore(date("2022-02-02 16:40:56"));
		assertResults(criteria, 1);
		criteria.setStartedOnOrBefore(date("2022-02-02 18:40:56"));
		assertResults(criteria, 1, 2);
		criteria.setStartedOnOrBefore(date("2022-02-03 16:40:56"));
		assertResults(criteria, 1, 2, 3);
		criteria.setStartedOnOrBefore(date("2022-03-02 16:40:56"));
		assertResults(criteria, 1, 2, 3, 4);
	}
	
	@Test
	// 2022-02-02 18:40:56.0, 2022-02-02 18:41:56.0
	public void shouldSearchAndCountQueueEntriesEndedOnOrBeforeDate() {
		assertNumberOfResults(criteria, 4);
		criteria.setEndedOnOrBefore(date("2022-02-02 18:40:55"));
		assertNumberOfResults(criteria, 0);
		criteria.setEndedOnOrBefore(date("2022-02-02 18:40:56"));
		assertResults(criteria, 1);
		criteria.setEndedOnOrBefore(date("2022-02-02 18:41:56"));
		assertResults(criteria, 1, 4);
		criteria.setEndedOnOrBefore(null);
		criteria.setIsEnded(true);
		assertResults(criteria, 1, 4);
		criteria.setIsEnded(false);
		assertResults(criteria, 2, 3);
	}
	
	@Test
	// 2022-02-02 18:40:56.0, 2022-02-02 18:41:56.0
	public void shouldSearchAndCountQueueEntriesEndedOnOrAfterDate() {
		assertNumberOfResults(criteria, 4);
		criteria.setEndedOnOrAfter(date("2022-02-02 18:41:57"));
		assertNumberOfResults(criteria, 0);
		criteria.setEndedOnOrAfter(date("2022-02-02 18:41:56"));
		assertResults(criteria, 4);
		criteria.setEndedOnOrAfter(date("2022-02-02 18:40:56"));
		assertResults(criteria, 1, 4);
		criteria.setEndedOnOrAfter(null);
		criteria.setIsEnded(true);
		assertResults(criteria, 1, 4);
		criteria.setIsEnded(false);
		assertResults(criteria, 2, 3);
	}
	
	/**
	 * Utility method that tests criteria against both DAO methods to getQueueEntries and
	 * getCountOfQueueEntries
	 */
	private void assertNumberOfResults(QueueEntrySearchCriteria criteria, int expectedNumber) {
		List<QueueEntry> queueEntries = dao.getQueueEntries(criteria);
		assertThat(queueEntries, hasSize(expectedNumber));
		Long numResults = dao.getCountOfQueueEntries(criteria);
		assertThat(numResults.intValue(), equalTo(expectedNumber));
	}
	
	/**
	 * Utility method that tests criteria against both DAO methods to getQueueEntries and
	 * getCountOfQueueEntries
	 */
	private void assertResults(QueueEntrySearchCriteria criteria, Integer... queueEntryIds) {
		List<QueueEntry> queueEntries = dao.getQueueEntries(criteria);
		assertThat(queueEntries, hasSize(queueEntryIds.length));
		for (Integer queueEntryId : queueEntryIds) {
			QueueEntry expected = dao.get(queueEntryId).orElse(null);
			assertThat(expected, notNullValue());
			assertThat(queueEntries.contains(expected), is(true));
		}
		Long numResults = dao.getCountOfQueueEntries(criteria);
		assertThat(numResults.intValue(), equalTo(queueEntryIds.length));
	}
	
	/**
	 * @return the date for the given string value
	 */
	private Date date(String dateVal) {
		return QueueUtils.parseDate(dateVal);
	}
}
