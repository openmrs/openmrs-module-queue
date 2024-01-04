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
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.VisitService;
import org.openmrs.module.queue.QueueModuleConstants;
import org.openmrs.module.queue.model.Queue;

@RunWith(MockitoJUnitRunner.class)
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
	
	@Before
	public void setupMocks() {
		MockitoAnnotations.openMocks(this);
		wrapper = new QueueServicesWrapper(queueService, queueEntryService, queueRoomService, roomProviderMapService,
		        administrationService, conceptService, locationService, patientService, visitService, providerService);
		conceptSet1 = new Concept();
		conceptSet1.addSetMember(new Concept());
		conceptSet1.addSetMember(new Concept());
		conceptSet2 = new Concept();
		conceptSet2.addSetMember(new Concept());
		when(conceptService.getConceptByUuid(conceptSet1.getUuid())).thenReturn(conceptSet1);
		queue = new Queue();
	}
	
	@Test(expected = IllegalStateException.class)
	public void getAllowedServices_shouldThrowErrorIfNoGpConfigured() {
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_SERVICE)).thenReturn(null);
		wrapper.getAllowedServices();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void getAllowedServices_shouldThrowErrorIfInvalidGpConfigured() {
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_SERVICE)).thenReturn("invalid");
		wrapper.getAllowedServices();
	}
	
	@Test
	public void getAllowedServices_shouldSucceedIfValidGpConfigured() {
		String conceptSetUuid = conceptSet1.getUuid();
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_SERVICE)).thenReturn(conceptSetUuid);
		List<Concept> services = wrapper.getAllowedServices();
		assertThat(services.size(), equalTo(2));
	}
	
	@Test(expected = IllegalStateException.class)
	public void getAllowedPriorities_shouldThrowErrorIfNoGpConfigured() {
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_PRIORITY)).thenReturn(null);
		wrapper.getAllowedPriorities(queue);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void getAllowedPriorities_shouldThrowErrorIfInvalidGpConfigured() {
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_PRIORITY)).thenReturn("invalid");
		wrapper.getAllowedPriorities(queue);
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
	
	@Test(expected = IllegalStateException.class)
	public void getAllowedStatuses_shouldThrowErrorIfNoGpConfigured() {
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_STATUS)).thenReturn(null);
		wrapper.getAllowedStatuses(queue);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void getAllowedStatuses_shouldThrowErrorIfInvalidGpConfigured() {
		when(administrationService.getGlobalProperty(QueueModuleConstants.QUEUE_STATUS)).thenReturn("invalid");
		wrapper.getAllowedStatuses(queue);
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
}
