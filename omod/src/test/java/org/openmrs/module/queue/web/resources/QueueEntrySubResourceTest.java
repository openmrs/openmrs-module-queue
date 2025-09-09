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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.QueueRoomService;
import org.openmrs.module.queue.api.QueueService;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.RoomProviderMapService;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;

@ExtendWith(MockitoExtension.class)
public class QueueEntrySubResourceTest extends BaseQueueResourceTest<QueueEntry, QueueEntrySubResource> {
	
	private static final String QUEUE_ENTRY_UUID = "6hje567a-fca0-11e5-9e59-08002719a7";
	
	@Mock
	private QueueService queueService;
	
	@Mock
	private QueueEntryService queueEntryService;
	
	@Mock
	private QueueRoomService queueRoomService;
	
	@Mock
	private RoomProviderMapService roomProviderMapService;
	
	@Mock
	private ConceptService conceptService;
	
	@Mock
	private LocationService locationService;
	
	@Mock
	private PatientService patientService;
	
	@Mock
	private QueueServicesWrapper queueServicesWrapper;
	
	private QueueEntry queueEntry;
	
	@BeforeEach
	public void setup() {
		this.cleanup();
		this.prepareMocks();
		queueEntry = mock(QueueEntry.class);
		lenient().when(queueServicesWrapper.getQueueService()).thenReturn(queueService);
		lenient().when(queueServicesWrapper.getQueueEntryService()).thenReturn(queueEntryService);
		lenient().when(queueServicesWrapper.getQueueRoomService()).thenReturn(queueRoomService);
		lenient().when(queueServicesWrapper.getRoomProviderMapService()).thenReturn(roomProviderMapService);
		lenient().when(queueServicesWrapper.getConceptService()).thenReturn(conceptService);
		lenient().when(queueServicesWrapper.getLocationService()).thenReturn(locationService);
		lenient().when(queueServicesWrapper.getPatientService()).thenReturn(patientService);
		
		lenient().when(queueEntry.getUuid()).thenReturn(QUEUE_ENTRY_UUID);
		getContext().when(() -> Context.getRegisteredComponents(QueueServicesWrapper.class))
		        .thenReturn(Collections.singletonList(queueServicesWrapper));
		
		this.setResource(new QueueEntrySubResource());
		this.setObject(queueEntry);
	}
	
	@Test
	public void shouldReturnDefaultRepresentation() {
		verifyDefaultRepresentation("uuid", "status", "visit", "priority", "priorityComment", "sortWeight", "patient",
		    "locationWaitingFor", "providerWaitingFor", "startedAt", "endedAt", "display");
	}
	
	@Test
	public void shouldReturnFullRepresentation() {
		verifyFullRepresentation("status", "priority", "priorityComment", "sortWeight", "patient", "locationWaitingFor",
		    "providerWaitingFor", "startedAt", "endedAt", "display", "uuid", "display", "auditInfo");
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
		when(queueEntryService.saveQueueEntry(getObject())).thenReturn(getObject());
		
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
