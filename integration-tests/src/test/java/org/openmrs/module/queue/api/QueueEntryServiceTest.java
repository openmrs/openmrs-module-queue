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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.exception.DuplicateQueueEntryException;
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
	
	@Autowired
	@Qualifier("queue.QueueEntryService")
	private QueueEntryService queueEntryService;
	
	@Before
	public void setup() {
		INITIAL_DATASET_XML.forEach(this::executeDataSet);
	}
	
	@Test(expected = DuplicateQueueEntryException.class)
	public void transitionQueueEntryShouldNotEndInitialIfNewIsDuplicate() {
		QueueEntry queueEntry = queueEntryService.getQueueEntryById(3).get();
		QueueEntryTransition transition = new QueueEntryTransition();
		transition.setQueueEntryToTransition(queueEntry);
		transition.setTransitionDate(queueEntry.getStartedAt());
		queueEntryService.transitionQueueEntry(transition);
	}
	
	@Test
	public void transitionQueueEntryShouldEndInitialIfNewIsNotDuplicate() {
		QueueEntry queueEntry = queueEntryService.getQueueEntryById(2).get();
		assertNull(queueEntry.getEndedAt());
		QueueEntryTransition transition = new QueueEntryTransition();
		transition.setQueueEntryToTransition(queueEntry);
		transition.setTransitionDate(new Date());
		queueEntryService.transitionQueueEntry(transition);
		assertNotNull(queueEntryService.getQueueEntryById(2).get().getEndedAt());
	}
	
	@Test
	public void removeQueueEntryFromQueue() {
		QueueEntry queueEntry = queueEntryService.getQueueEntryById(2).get();
		assertNull(queueEntry.getEndedAt());
		queueEntry.setEndedAt(new Date());
		queueEntryService.saveQueueEntry(queueEntry);
		assertNotNull(queueEntryService.getQueueEntryById(2).get().getEndedAt());
	}
}
