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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class MergePatientsWithQueueEntriesSaveHandlerTest extends BaseModuleContextSensitiveTest {
	
	private static final List<String> INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/VisitQueueEntryDaoTest_visitInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/validators/QueueEntryValidatorTest_globalPropertyInitialDataset.xml");
	
	@Autowired
	@Qualifier("queue.QueueEntryService")
	private QueueEntryService queueEntryService;
	
	@Before
	public void setup() {
		INITIAL_DATASET_XML.forEach(this::executeDataSet);
	}
	
	@Test
	public void shouldChangeQueueEntryPatientOnMergingPatient() throws Exception {
		PatientService ps = Context.getPatientService();
		Patient winnerPatient = ps.getPatient(100);
		Patient loserPatient = ps.getPatient(101);
		
		// set up queue entry for loserPatient
		QueueEntry queueEntryToCloneFrom = queueEntryService.getQueueEntryById(1).get();
		QueueEntry queueEntry = new QueueEntry();
		queueEntry.setPatient(loserPatient);
		queueEntry.setQueue(queueEntryToCloneFrom.getQueue());
		queueEntry.setStatus(queueEntryToCloneFrom.getStatus());
		queueEntry.setPriority(queueEntryToCloneFrom.getPriority());
		queueEntry.setStartedAt(new Date());
		queueEntryService.saveQueueEntry(queueEntry);
		
		assertThat(queueEntry.getPatient().getPatientId(), is(loserPatient.getPatientId()));
		ps.mergePatients(winnerPatient, loserPatient);
		queueEntry = queueEntryService.getQueueEntryById(11).get();
		assertThat(queueEntry.getPatient().getPatientId(), is(winnerPatient.getPatientId()));
	}
}
