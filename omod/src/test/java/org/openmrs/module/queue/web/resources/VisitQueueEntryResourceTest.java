/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.web.resources;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.VisitQueueEntryService;
import org.openmrs.module.queue.model.VisitQueueEntry;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class VisitQueueEntryResourceTest extends BaseQueueResourceTest<VisitQueueEntry, VisitQueueEntryResource> {
	
	private static final String VISIT_QUEUE_ENTRY_UUID = "6hje567a-fca0-11e5-9e59-08002719a7";
	
	@Mock
	private VisitQueueEntryService visitQueueEntryService;
	
	private VisitQueueEntry visitQueueEntry;
	
	@Before
	public void setup() {
		this.prepareMocks();
		visitQueueEntry = mock(VisitQueueEntry.class);
		
		when(visitQueueEntry.getUuid()).thenReturn(VISIT_QUEUE_ENTRY_UUID);
		when(Context.getService(VisitQueueEntryService.class)).thenReturn(visitQueueEntryService);
		
		this.setResource(new VisitQueueEntryResource());
		this.setObject(visitQueueEntry);
	}
	
	@Test
	public void shouldGetQueueEntryService() {
		assertThat(visitQueueEntryService, notNullValue());
	}
	
	@Test
	public void shouldGetResourceByUniqueUuid() {
		when(visitQueueEntryService.getVisitQueueEntryByUuid(VISIT_QUEUE_ENTRY_UUID))
		        .thenReturn(Optional.of(visitQueueEntry));
		
		VisitQueueEntry result = getResource().getByUniqueId(VISIT_QUEUE_ENTRY_UUID);
		assertThat(result, notNullValue());
		assertThat(result.getUuid(), is(VISIT_QUEUE_ENTRY_UUID));
	}
	
	@Test
	public void shouldCreateNewResource() {
		when(visitQueueEntryService.createVisitQueueEntry(getObject())).thenReturn(getObject());
		
		VisitQueueEntry newlyCreatedObject = getResource().save(getObject());
		assertThat(newlyCreatedObject, notNullValue());
		assertThat(newlyCreatedObject.getUuid(), is(VISIT_QUEUE_ENTRY_UUID));
	}
	
	@Test
	public void shouldInstantiateNewDelegate() {
		assertThat(getResource().newDelegate(), notNullValue());
	}
}
