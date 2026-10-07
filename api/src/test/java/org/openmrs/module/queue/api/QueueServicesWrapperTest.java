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
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openmrs.Concept;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.VisitService;
import org.openmrs.module.queue.QueueModuleConstants;
import org.openmrs.module.queue.model.Queue;

@ExtendWith(MockitoExtension.class)
public class QueueServicesWrapperTest {
	
	QueueServicesWrapper wrapper;
	
	@Mock
	private QueueService queueService;
	
	@Mock
	private QueueEntryService queueEntryService;
	
	@Mock
	private QueueRoomService queueRoomService;
	
	@Mock
	private RoomProviderMapService roomProviderMapService;
	
	@Mock
	private AdministrationService administrationService;
	
	@Mock
	private ConceptService conceptService;
	
	@Mock
	private LocationService locationService;
	
	@Mock
	private PatientService patientService;
	
	@Mock
	private VisitService visitService;
	
	@Mock
	private ProviderService providerService;
	
	private Queue queue;
	
	private Concept conceptSet1;
	
	private Concept conceptSet2;
	
	@BeforeEach
	public void setupMocks() {
		MockitoAnnotations.openMocks(this);
		wrapper = new QueueServicesWrapper(queueService, queueEntryService, queueRoomService, roomProviderMapService,
		        administrationService, conceptService, locationService, patientService, visitService, providerService);
		conceptSet1 = new Concept();
		conceptSet1.addSetMember(new Concept());
		conceptSet1.addSetMember(new Concept());
		conceptSet2 = new Concept();
		conceptSet2.addSetMember(new Concept());
		lenient().when(conceptService.getConceptByUuid(conceptSet1.getUuid())).thenReturn(conceptSet1);
		queue = new Queue();
	}
	
	@Test
	public void getAllowedServices_shouldThrowErrorIfNoGpConfigured() {
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_SERVICE)).thenReturn(null);
		assertThrows(IllegalStateException.class, () -> wrapper.getAllowedServices());
	}
	
	@Test
	public void getAllowedServices_shouldThrowErrorIfInvalidGpConfigured() {
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_SERVICE)).thenReturn("invalid");
		assertThrows(IllegalArgumentException.class, () -> wrapper.getAllowedServices());
	}
	
	@Test
	public void getAllowedServices_shouldSucceedIfValidGpConfigured() {
		String conceptSetUuid = conceptSet1.getUuid();
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_SERVICE)).thenReturn(conceptSetUuid);
		List<Concept> services = wrapper.getAllowedServices();
		assertThat(services.size(), equalTo(2));
	}
	
	@Test
	public void getAllowedPriorities_shouldThrowErrorIfNoGpConfigured() {
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_PRIORITY)).thenReturn(null);
		assertThrows(IllegalStateException.class, () -> wrapper.getAllowedPriorities(queue));
	}
	
	@Test
	public void getAllowedPriorities_shouldThrowErrorIfInvalidGpConfigured() {
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_PRIORITY)).thenReturn("invalid");
		assertThrows(IllegalArgumentException.class, () -> wrapper.getAllowedPriorities(queue));
	}
	
	@Test
	public void getAllowedPriorities_shouldSucceedIfValidGpConfigured() {
		String conceptSetUuid = conceptSet1.getUuid();
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_PRIORITY)).thenReturn(conceptSetUuid);
		List<Concept> priorities = wrapper.getAllowedPriorities(queue);
		assertThat(priorities.size(), equalTo(2));
	}
	
	@Test
	public void getAllowedPriorities_shouldSucceedIfConceptConfiguredOnQueue() {
		queue.setPriorityConceptSet(conceptSet2);
		List<Concept> priorities = wrapper.getAllowedPriorities(queue);
		assertThat(priorities.size(), equalTo(1));
	}
	
	@Test
	public void getAllowedStatuses_shouldThrowErrorIfNoGpConfigured() {
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_STATUS)).thenReturn(null);
		assertThrows(IllegalStateException.class, () -> wrapper.getAllowedStatuses(queue));
	}
	
	@Test
	public void getAllowedStatuses_shouldThrowErrorIfInvalidGpConfigured() {
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_STATUS)).thenReturn("invalid");
		assertThrows(IllegalArgumentException.class, () -> wrapper.getAllowedStatuses(queue));
	}
	
	@Test
	public void getAllowedStatuses_shouldSucceedIfValidGpConfigured() {
		String conceptSetUuid = conceptSet1.getUuid();
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_STATUS)).thenReturn(conceptSetUuid);
		List<Concept> statuses = wrapper.getAllowedStatuses(queue);
		assertThat(statuses.size(), equalTo(2));
	}
	
	@Test
	public void getAllowedStatuses_shouldSucceedIfConceptConfiguredOnQueue() {
		queue.setStatusConceptSet(conceptSet2);
		List<Concept> statuses = wrapper.getAllowedStatuses(queue);
		assertThat(statuses.size(), equalTo(1));
	}
	
	@Test
	public void getQueue_shouldGetQueueByUuid() {
		when(queueService.getQueueByUuid(queue.getUuid())).thenReturn(Optional.of(queue));
		assertThat(wrapper.getQueue(queue.getUuid()), equalTo(queue));
	}
	
	@Test
	public void getQueue_shouldGetQueueByName() {
		queue.setName("Triage");
		when(queueService.getQueueByUuid("Triage")).thenReturn(Optional.empty());
		when(queueService.getAllQueues()).thenReturn(Collections.singletonList(queue));
		assertThat(wrapper.getQueue("Triage"), equalTo(queue));
		assertThat(wrapper.getQueue("triage"), equalTo(queue));
	}
	
	@Test
	public void getQueue_shouldThrowErrorIfNameIsAmbiguous() {
		queue.setName("Triage");
		Queue otherQueue = new Queue();
		otherQueue.setName("Triage");
		when(queueService.getQueueByUuid("Triage")).thenReturn(Optional.empty());
		when(queueService.getAllQueues()).thenReturn(Arrays.asList(queue, otherQueue));
		assertThrows(IllegalArgumentException.class, () -> wrapper.getQueue("Triage"));
	}
	
	@Test
	public void getQueue_shouldThrowErrorIfQueueRefDoesNotResolve() {
		when(queueService.getQueueByUuid("unknown")).thenReturn(Optional.empty());
		when(queueService.getAllQueues()).thenReturn(Collections.emptyList());
		assertThrows(IllegalArgumentException.class, () -> wrapper.getQueue("unknown"));
	}
}
