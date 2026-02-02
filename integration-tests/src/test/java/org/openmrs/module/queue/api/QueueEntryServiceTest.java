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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ValidationException;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.QueueEntryTransition;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class QueueEntryServiceTest extends BaseModuleContextSensitiveTest {
	
	private static final List<String> INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/VisitQueueEntryDaoTest_visitInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/validators/QueueEntryValidatorTest_globalPropertyInitialDataset.xml");
	
	private static final String PATIENT_UUID = "90b38324-e2fd-4feb-95b7-9e9a2a8876fg";
	
	private static final String STATUS_CONCEPT_UUID = "56b910bd-298c-4ecf-a632-661ae2f7865y";
	
	private static final String PRIORITY_CONCEPT_UUID = "90b910bd-298c-4ecf-a632-661ae2f446op";
	
	private static final String TEST_QUEUE_UUID = "5ob8gj90-9090-4kbc-80dc-2e5d30252bb3";
	
	@Autowired
	@Qualifier("queue.QueueEntryService")
	private QueueEntryService queueEntryService;
	
	@Autowired
	@Qualifier("queue.QueueService")
	private QueueService queueService;
	
	@Autowired
	private PatientService patientService;
	
	@Autowired
	private ConceptService conceptService;
	
	@Before
	public void setup() {
		INITIAL_DATASET_XML.forEach(this::executeDataSet);
	}
	
	@Test
	public void transitionQueueEntryShouldNotEndInitialIfNewIsDuplicate() {
		QueueEntry queueEntry = queueEntryService.getQueueEntryById(3).get();
		Queue targetQueue = queueService.getQueueByUuid(TEST_QUEUE_UUID).orElse(null);
		assertThat(targetQueue, is(notNullValue()));
		Patient patient = patientService.getPatientByUuid(PATIENT_UUID);
		Concept status = conceptService.getConceptByUuid(STATUS_CONCEPT_UUID);
		Concept priority = conceptService.getConceptByUuid(PRIORITY_CONCEPT_UUID);
		
		// Create an overlapping entry in the target queue that will conflict with the transition's new entry
		QueueEntry overlappingEntry = new QueueEntry();
		overlappingEntry.setQueue(targetQueue);
		overlappingEntry.setPatient(patient);
		overlappingEntry.setStatus(status);
		overlappingEntry.setPriority(priority);
		overlappingEntry.setStartedAt(DateUtils.addHours(new Date(), -1));
		queueEntryService.saveQueueEntry(overlappingEntry);
		
		QueueEntryTransition transition = new QueueEntryTransition();
		transition.setQueueEntryToTransition(queueEntry);
		transition.setNewQueue(targetQueue);
		transition.setTransitionDate(new Date());
		try {
			queueEntryService.transitionQueueEntry(transition);
			fail("Expected ValidationException to be thrown");
		}
		catch (ValidationException e) {
			assertThat(e.getMessage(), containsString("queue.entry.error.duplicate"));
		}
	}
	
	@Test
	public void transitionQueueEntryShouldEndInitialIfNewIsNotDuplicate() {
		QueueEntry queueEntry = queueEntryService.getQueueEntryById(2).get();
		assertThat(queueEntry.getEndedAt(), is(nullValue()));
		QueueEntryTransition transition = new QueueEntryTransition();
		transition.setQueueEntryToTransition(queueEntry);
		transition.setTransitionDate(new Date());
		queueEntryService.transitionQueueEntry(transition);
		assertThat(queueEntryService.getQueueEntryById(2).get().getEndedAt(), is(notNullValue()));
	}
}
