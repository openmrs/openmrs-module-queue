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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Visit;
import org.openmrs.api.APIException;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Covers what {@link VisitWithQueueEntriesDeleteAdviceTest} structurally cannot: that the queue
 * entries the advice purges go only if the visit goes too. This needs its own class because the
 * test has to run outside the transaction the other tests rely on. Inside one the cascade's deletes
 * are still unflushed when the delete is refused and {@code Context.clearSession()} discards them,
 * so the entry reads as present again whether or not anything was rolled back, and the assertion
 * holds either way.
 */
@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class VisitWithQueueEntriesDeleteAdviceTransactionTest extends BaseModuleContextSensitiveTest {
	
	private static final List<String> INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/VisitQueueEntryDaoTest_visitInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/validators/QueueEntryValidatorTest_globalPropertyInitialDataset.xml");
	
	private static final int QUEUE_ENTRY_ID = 3;
	
	private final VisitWithQueueEntriesDeleteAdvice advice = new VisitWithQueueEntriesDeleteAdvice();
	
	@Autowired
	@Qualifier("queue.QueueEntryService")
	private QueueEntryService queueEntryService;
	
	@Autowired
	private VisitService visitService;
	
	@Autowired
	private EncounterService encounterService;
	
	@Autowired
	private LocationService locationService;
	
	@Before
	public void setup() {
		INITIAL_DATASET_XML.forEach(this::executeDataSet);
		// the module test harness does not register advice from config.xml, so register it here to
		// exercise the same interceptor chain that production purges go through (ModuleAdviceConfigTest
		// covers the config.xml declaration itself)
		Context.addAdvice(VisitService.class, advice);
	}
	
	@After
	public void tearDown() {
		Context.removeAdvice(VisitService.class, advice);
		// Nothing this test wrote was rolled back. Core wipes after the last method of a class anyway,
		// so this is here for the second test method: without it that method's executeDataSet would
		// meet the rows this one committed.
		deleteAllData();
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldKeepQueueEntriesWhenCoreRefusesToPurgeTheVisit() {
		Visit visit = queueEntryService.getQueueEntryById(QUEUE_ENTRY_ID).get().getVisit();
		Integer visitId = visit.getVisitId();
		giveTheVisitAnEncounter(visit);
		assertFalse("the visit needs an encounter for core to refuse the purge",
		    encounterService.getEncountersByVisit(visit, true).isEmpty());
		
		// core checks for encounters only once this advice has already run
		APIException refused = assertThrows(APIException.class, () -> visitService.purgeVisit(visit));
		// pin what refused it, so this cannot pass on an exception raised before the cascade ran at all
		// (the wording is core's Visit.purge.inUse)
		assertThat(refused.getMessage(), containsString("encounters"));
		Context.clearSession();
		
		assertNotNull("the visit survived, so its queue entries must too", visitService.getVisit(visitId));
		assertTrue("the refused purge must not have taken the queue entry down with it",
		    queueEntryService.getQueueEntryById(QUEUE_ENTRY_ID).isPresent());
	}
	
	private void giveTheVisitAnEncounter(Visit visit) {
		Encounter encounter = new Encounter();
		encounter.setPatient(visit.getPatient());
		encounter.setEncounterType(encounterService.getEncounterType(1));
		encounter.setEncounterDatetime(visit.getStartDatetime());
		encounter.setLocation(locationService.getLocation(1));
		encounter.setVisit(visit);
		encounterService.saveEncounter(encounter);
	}
}
