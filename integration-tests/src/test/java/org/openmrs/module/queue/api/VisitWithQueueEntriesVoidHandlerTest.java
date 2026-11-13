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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class VisitWithQueueEntriesVoidHandlerTest extends BaseModuleContextSensitiveTest {
	
	private static final List<String> INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/VisitQueueEntryDaoTest_visitInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/validators/QueueEntryValidatorTest_globalPropertyInitialDataset.xml");
	
	// Visit 102 owns the single active queue entry 3
	private static final int VISIT_ID = 102;
	
	private static final int QUEUE_ENTRY_ID = 3;
	
	// Visit 101 owns queue entries 1 (ended), 2 (active) and 10 (voided on its own)
	private static final int VISIT_WITH_VOIDED_ENTRY_ID = 101;
	
	private Visit visit;
	
	private QueueEntry queueEntry;
	
	@Autowired
	@Qualifier("queue.QueueEntryService")
	private QueueEntryService queueEntryService;
	
	@Autowired
	private VisitWithQueueEntriesVoidHandler handler;
	
	@Autowired
	private VisitService visitService;
	
	@Before
	public void setup() {
		INITIAL_DATASET_XML.forEach(this::executeDataSet);
		queueEntry = queueEntryService.getQueueEntryById(QUEUE_ENTRY_ID).get();
		visit = queueEntry.getVisit();
	}
	
	@Test
	public void shouldVoidQueueEntriesWhenVisitIsVoided() {
		assertFalse(queueEntry.getVoided());
		String voidReason = "for testing";
		
		visitService.voidVisit(visit, voidReason);
		Context.flushSession();
		Context.clearSession();
		
		Visit voidedVisit = visitService.getVisit(VISIT_ID);
		QueueEntry voidedEntry = queueEntryService.getQueueEntryById(QUEUE_ENTRY_ID).get();
		assertTrue(voidedVisit.getVoided());
		assertTrue(voidedEntry.getVoided());
		assertThat(voidedEntry.getVoidReason(), equalTo(voidReason));
		assertThat(voidedEntry.getDateVoided().getTime(), equalTo(voidedVisit.getDateVoided().getTime()));
		assertThat(voidedEntry.getVoidedBy(), equalTo(voidedVisit.getVoidedBy()));
	}
	
	@Test
	public void shouldVoidQueueEntriesBeforeTheVisitItselfIsFlaggedVoided() {
		// This is the state the handler sees whenever it is ordered ahead of core's BaseVoidHandler, and
		// nothing pins that order: the two carry the same @Handler order, HandlerUtil sorts stably, and
		// the tie falls to the iteration order of the map ServiceContext.getRegisteredComponents builds,
		// which moves with the number of VoidHandler beans a deployment registers. Taking the void state
		// from the arguments rather than from the visit is what makes the cascade independent of that.
		assertFalse(visit.getVoided());
		assertFalse(queueEntry.getVoided());
		// whole seconds: date_voided does not round-trip milliseconds
		Date voidedDate = new Date(System.currentTimeMillis() / 1000 * 1000);
		
		handler.handle(visit, Context.getAuthenticatedUser(), voidedDate, "for testing");
		Context.flushSession();
		Context.clearSession();
		
		QueueEntry voidedEntry = queueEntryService.getQueueEntryById(QUEUE_ENTRY_ID).get();
		assertFalse(visitService.getVisit(VISIT_ID).getVoided());
		assertTrue(voidedEntry.getVoided());
		assertThat(voidedEntry.getVoidReason(), equalTo("for testing"));
		assertThat(voidedEntry.getDateVoided().getTime(), equalTo(voidedDate.getTime()));
		assertThat(voidedEntry.getVoidedBy(), equalTo(Context.getAuthenticatedUser()));
	}
	
	@Test
	public void shouldVoidQueueEntriesForUserWithoutQueuePrivileges() {
		// "butch" from the standard test dataset has the Provider role and no privileges; proxy only the
		// core visit privileges so the handler has to obtain the queue privileges itself
		Context.becomeUser("3-4");
		Context.addProxyPrivilege(org.openmrs.util.PrivilegeConstants.DELETE_VISITS);
		Context.addProxyPrivilege(org.openmrs.util.PrivilegeConstants.GET_ENCOUNTERS);
		try {
			assertFalse(Context.hasPrivilege(org.openmrs.module.queue.utils.PrivilegeConstants.GET_QUEUE_ENTRIES));
			visitService.voidVisit(visit, "for testing");
			Context.flushSession();
		}
		finally {
			Context.removeProxyPrivilege(org.openmrs.util.PrivilegeConstants.DELETE_VISITS);
			Context.removeProxyPrivilege(org.openmrs.util.PrivilegeConstants.GET_ENCOUNTERS);
			authenticate();
		}
		Context.clearSession();
		assertTrue(visitService.getVisit(VISIT_ID).getVoided());
		assertTrue(queueEntryService.getQueueEntryById(QUEUE_ENTRY_ID).get().getVoided());
	}
	
	@Test
	public void shouldVoidOverlappingQueueEntriesInTheSameQueueIfVisitIsVoided() {
		executeDataSet("org/openmrs/module/queue/api/dao/QueueEntryDaoTest_overlappingEntriesInitialDataset.xml");
		
		visitService.voidVisit(visit, "for testing");
		Context.flushSession();
		Context.clearSession();
		
		assertTrue(queueEntryService.getQueueEntryById(3).get().getVoided());
		assertTrue(queueEntryService.getQueueEntryById(12).get().getVoided());
	}
	
	@Test
	public void shouldLeaveTheStampOfAnAlreadyVoidedQueueEntryAlone() {
		// entry 10 was voided on its own, so an unvoid must be able to tell it apart from the entries
		// that went down with the visit
		QueueEntry alreadyVoided = queueEntryService.getQueueEntryById(10).get();
		assertTrue(alreadyVoided.getVoided());
		Date originalDateVoided = alreadyVoided.getDateVoided();
		String originalVoidReason = alreadyVoided.getVoidReason();
		assertNotNull(originalDateVoided);
		
		visitService.voidVisit(visitService.getVisit(VISIT_WITH_VOIDED_ENTRY_ID), "for testing");
		Context.flushSession();
		Context.clearSession();
		
		Visit voidedVisit = visitService.getVisit(VISIT_WITH_VOIDED_ENTRY_ID);
		alreadyVoided = queueEntryService.getQueueEntryById(10).get();
		assertThat(alreadyVoided.getDateVoided().getTime(), equalTo(originalDateVoided.getTime()));
		assertThat(alreadyVoided.getVoidReason(), equalTo(originalVoidReason));
		assertThat(alreadyVoided.getDateVoided().getTime(), not(equalTo(voidedVisit.getDateVoided().getTime())));
		
		QueueEntry cascaded = queueEntryService.getQueueEntryById(2).get();
		assertTrue(cascaded.getVoided());
		assertThat(cascaded.getDateVoided().getTime(), equalTo(voidedVisit.getDateVoided().getTime()));
	}
	
	@Test
	public void shouldVoidQueueEntriesThatHadAlreadyEnded() {
		// An entry is ended rather than removed when the patient is served or moved on to another queue,
		// so a visit deleted after that has ended entries on it and they belong to a visit that now never
		// happened. Nothing else pins this: the cascade covers them only because
		// QueueEntrySearchCriteria.isEnded defaults to null, and the sibling save handler sets it to false.
		QueueEntry ended = queueEntryService.getQueueEntryById(1).get();
		assertNotNull(ended.getEndedAt());
		assertFalse(ended.getVoided());
		
		visitService.voidVisit(visitService.getVisit(VISIT_WITH_VOIDED_ENTRY_ID), "for testing");
		Context.flushSession();
		Context.clearSession();
		
		Visit voidedVisit = visitService.getVisit(VISIT_WITH_VOIDED_ENTRY_ID);
		ended = queueEntryService.getQueueEntryById(1).get();
		assertTrue(ended.getVoided());
		assertThat(ended.getDateVoided().getTime(), equalTo(voidedVisit.getDateVoided().getTime()));
	}
}
