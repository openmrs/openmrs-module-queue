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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
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
	
	private final VisitWithQueueEntriesDeleteAdvice advice = new VisitWithQueueEntriesDeleteAdvice();
	
	private Visit visit;
	
	@Autowired
	@Qualifier("queue.QueueEntryService")
	private QueueEntryService queueEntryService;
	
	@Autowired
	private VisitService visitService;
	
	@Before
	public void setup() {
		INITIAL_DATASET_XML.forEach(this::executeDataSet);
		visit = queueEntryService.getQueueEntryById(3).get().getVisit();
		// the module test harness does not register advice from config.xml, so register it here to
		// exercise the same interceptor chain that production purges go through (ModuleAdviceConfigTest
		// covers the config.xml declaration itself)
		Context.addAdvice(VisitService.class, advice);
	}
	
	@After
	public void tearDown() {
		// the Spring context is shared across test classes, so this must not outlive the test
		Context.removeAdvice(VisitService.class, advice);
	}
	
	@Test
	public void shouldPurgeQueueEntriesWhenVisitIsPurged() {
		Integer visitId = visit.getVisitId();
		assertTrue(queueEntryService.getQueueEntryById(3).isPresent());
		
		visitService.purgeVisit(visit);
		Context.flushSession();
		
		assertNull(visitService.getVisit(visitId));
		assertFalse(queueEntryService.getQueueEntryById(3).isPresent());
	}
	
	@Test
	public void shouldPurgeQueueEntriesForUserWithoutQueuePrivileges() {
		// "butch" from the standard test dataset has the Provider role and no privileges; proxy only the
		// core visit privileges so the advice has to obtain the queue privileges itself
		Integer visitId = visit.getVisitId();
		Context.becomeUser("3-4");
		Context.addProxyPrivilege(org.openmrs.util.PrivilegeConstants.PURGE_VISITS);
		Context.addProxyPrivilege(org.openmrs.util.PrivilegeConstants.GET_ENCOUNTERS);
		try {
			assertFalse(Context.hasPrivilege(org.openmrs.module.queue.utils.PrivilegeConstants.PURGE_QUEUE_ENTRIES));
			visitService.purgeVisit(visit);
			Context.flushSession();
		}
		finally {
			Context.removeProxyPrivilege(org.openmrs.util.PrivilegeConstants.PURGE_VISITS);
			Context.removeProxyPrivilege(org.openmrs.util.PrivilegeConstants.GET_ENCOUNTERS);
			authenticate();
		}
		assertNull(visitService.getVisit(visitId));
		assertFalse(queueEntryService.getQueueEntryById(3).isPresent());
	}
	
	@Test
	public void shouldPurgeVoidedQueueEntriesOfAPurgedVisit() {
		// entry 10 on visit 101 is voided, and a voided entry is just as much of a foreign key to the visit
		Visit visitWithVoidedEntry = visitService.getVisit(101);
		assertTrue(queueEntryService.getQueueEntryById(10).isPresent());
		
		visitService.purgeVisit(visitWithVoidedEntry);
		Context.flushSession();
		
		assertNull(visitService.getVisit(101));
		assertFalse(queueEntryService.getQueueEntryById(10).isPresent());
	}
	
	@Test
	public void shouldLeaveOtherVisitServiceCallsAlone() {
		// This is an around interceptor, so every VisitService call in a server with the module
		// installed passes through it, not just the purges. A no-arg method covers the empty argument
		// array it has to tolerate on the way past.
		assertFalse(visitService.getAllVisitTypes().isEmpty());
		assertNotNull(visitService.getVisit(visit.getVisitId()));
		assertTrue(queueEntryService.getQueueEntryById(3).isPresent());
	}
}
