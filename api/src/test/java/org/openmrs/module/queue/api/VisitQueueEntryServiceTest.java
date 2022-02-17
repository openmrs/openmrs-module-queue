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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Visit;
import org.openmrs.module.queue.api.dao.VisitQueueEntryDao;
import org.openmrs.module.queue.api.impl.VisitQueueEntryServiceImpl;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.VisitQueueEntry;

@RunWith(MockitoJUnitRunner.class)
public class VisitQueueEntryServiceTest {
	
	private static final String VISIT_QUEUE_ENTRY_UUID = "j8f0bb90-86f4-4d9c-8b6c-3713d748ef74";
	
	private VisitQueueEntryServiceImpl visitQueueEntryService;
	
	@Mock
	private VisitQueueEntryDao<VisitQueueEntry> dao;
	
	@Before
	public void setupMocks() {
		MockitoAnnotations.openMocks(this);
		visitQueueEntryService = new VisitQueueEntryServiceImpl();
		visitQueueEntryService.setDao(dao);
	}
	
	@Test
	public void shouldGetVisitQueueEntryByUuid() {
		VisitQueueEntry visitQueueEntry = mock(VisitQueueEntry.class);
		when(visitQueueEntry.getUuid()).thenReturn(VISIT_QUEUE_ENTRY_UUID);
		when(dao.get(VISIT_QUEUE_ENTRY_UUID)).thenReturn(Optional.of(visitQueueEntry));
		
		Optional<VisitQueueEntry> result = visitQueueEntryService.getVisitQueueEntryByUuid(VISIT_QUEUE_ENTRY_UUID);
		assertThat(result.isPresent(), is(true));
		result.ifPresent(q -> assertThat(q.getUuid(), is(VISIT_QUEUE_ENTRY_UUID)));
	}
	
	@Test
	public void shouldCreateNewRecordForVisitQueueEntry() {
		VisitQueueEntry visitQueueEntry = mock(VisitQueueEntry.class);
		Visit visit = mock(Visit.class);
		QueueEntry queueEntry = mock(QueueEntry.class);
		
		when(visitQueueEntry.getQueueEntry()).thenReturn(queueEntry);
		when(visitQueueEntry.getVisit()).thenReturn(visit);
		when(dao.createOrUpdate(visitQueueEntry)).thenReturn(visitQueueEntry);
		
		VisitQueueEntry result = this.dao.createOrUpdate(visitQueueEntry);
		assertThat(result, notNullValue());
		assertThat(result.getVisit(), is(visit));
		assertThat(result.getQueueEntry(), is(queueEntry));
		
	}
	
	@Test
	public void shouldVoidVisitQueueEntryRecord() {
		when(dao.get(VISIT_QUEUE_ENTRY_UUID)).thenReturn(Optional.empty());
		
		visitQueueEntryService.voidVisitQueueEntry(VISIT_QUEUE_ENTRY_UUID, "voidReason");
		
		assertThat(visitQueueEntryService.getVisitQueueEntryByUuid(VISIT_QUEUE_ENTRY_UUID).isPresent(), is(false));
	}
	
}
