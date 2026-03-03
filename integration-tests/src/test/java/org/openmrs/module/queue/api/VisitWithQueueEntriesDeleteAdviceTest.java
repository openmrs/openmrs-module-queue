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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class VisitWithQueueEntriesDeleteAdviceTest extends BaseModuleContextSensitiveTest {
	
	private static final List<String> INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/VisitQueueEntryDaoTest_visitInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/validators/QueueEntryValidatorTest_globalPropertyInitialDataset.xml");
	
	private Visit visit;
	
	private QueueEntry queueEntry;
	
	@Autowired
	@Qualifier("queue.QueueEntryService")
	private QueueEntryService queueEntryService;
	
	@Autowired
	private VisitService visitService;
	
	@Before
	public void setup() {
		INITIAL_DATASET_XML.forEach(this::executeDataSet);
		queueEntry = queueEntryService.getQueueEntryById(3).get();
		visit = queueEntry.getVisit();
	}
	
	@Test
	public void shouldPurgeQueueEntriesWhenVisitIsPurged() throws Throwable {
		assertFalse(queueEntry.getVoided());
		int queueEntryId = queueEntry.getId();
		
		// Simulate what the AOP advice does before purgeVisit
		VisitWithQueueEntriesDeleteAdvice advice = new VisitWithQueueEntriesDeleteAdvice();
		java.lang.reflect.Method purgeMethod = VisitService.class.getMethod("purgeVisit", Visit.class);
		advice.before(purgeMethod, new Object[] { visit }, visitService);
		
		// Verify the queue entry was purged
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		criteria.setVisit(visit);
		criteria.setIncludedVoided(true);
		List<QueueEntry> remainingEntries = queueEntryService.getQueueEntries(criteria);
		assertTrue("Queue entries should be purged before visit purge", remainingEntries.isEmpty());
		
		// Now the visit can be purged without FK constraint violation
		assertFalse(queueEntryService.getQueueEntryById(queueEntryId).isPresent());
	}
}
