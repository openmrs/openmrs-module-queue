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
import static org.hamcrest.Matchers.nullValue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class QueueEntrySubResourceTest extends BaseQueueResourceTest<QueueEntry, QueueEntrySubResource> {
	
	private static final String QUEUE_ENTRY_UUID = "6hje567a-fca0-11e5-9e59-08002719a7";
	
	@Mock
	private QueueEntryService queueEntryService;
	
	private QueueEntry queueEntry;
	
	@Before
	public void setup() {
		this.prepareMocks();
		queueEntry = mock(QueueEntry.class);
		
		when(queueEntry.getUuid()).thenReturn(QUEUE_ENTRY_UUID);
		when(Context.getService(QueueEntryService.class)).thenReturn(queueEntryService);
		
		this.setResource(new QueueEntrySubResource());
		this.setObject(queueEntry);
	}
	
	@Test
	public void shouldGetQueueEntryService() {
		assertThat(queueEntryService, notNullValue());
	}
	
	@Test
	public void shouldReturnDefaultRepresentation() {
		verifyDefaultRepresentation("uuid", "priority", "priorityComment", "sortWeight", "patient", "locationWaitingFor",
		    "providerWaitingFor", "startedAt", "endedAt", "display");
	}
	
	@Test
	public void shouldReturnFullRepresentation() {
		verifyFullRepresentation("queue", "status", "priority", "priorityComment", "sortWeight", "patient",
		    "locationWaitingFor", "providerWaitingFor", "startedAt", "endedAt", "display", "uuid", "display", "auditInfo");
	}
	
	@Test
	public void shouldReturnNullForCustomRepresentation() {
		CustomRepresentation customRepresentation = new CustomRepresentation("custom-representation");
		assertThat(getResource().getRepresentationDescription(customRepresentation), is(nullValue()));
	}
	
	@Test
	public void shouldNOTReturnNullForDefaultRepresentation() {
		assertThat(getResource().getRepresentationDescription(new DefaultRepresentation()), is(notNullValue()));
	}
	
	@Test
	public void shouldNOTReturnNullForFullRepresentation() {
		assertThat(getResource().getRepresentationDescription(new FullRepresentation()), is(notNullValue()));
	}
	
	@Test
	public void shouldNOTReturnNullForRefRepresentation() {
		assertThat(getResource().getRepresentationDescription(new RefRepresentation()), is(notNullValue()));
	}
	
	@Test
	public void shouldGetResourceByUniqueUuid() {
		when(queueEntryService.getQueueEntryByUuid(QUEUE_ENTRY_UUID)).thenReturn(Optional.of(queueEntry));
		
		QueueEntry result = getResource().getByUniqueId(QUEUE_ENTRY_UUID);
		assertThat(result, notNullValue());
		assertThat(result.getUuid(), is(QUEUE_ENTRY_UUID));
	}
	
	@Test
	public void shouldCreateNewResource() {
		when(queueEntryService.createQueueEntry(getObject())).thenReturn(getObject());
		
		QueueEntry newlyCreatedObject = getResource().save(getObject());
		assertThat(newlyCreatedObject, notNullValue());
		assertThat(newlyCreatedObject.getUuid(), is(QUEUE_ENTRY_UUID));
	}
	
	@Test
	public void shouldInstantiateNewDelegate() {
		assertThat(getResource().newDelegate(), notNullValue());
	}
	
	@Test
	public void verifyResourceVersion() {
		assertThat(getResource().getResourceVersion(), is("2.3"));
	}
}
