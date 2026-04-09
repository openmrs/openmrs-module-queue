/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.web.resources.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.api.LocationService;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.web.dto.QueueEntryMetricsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class QueueEntryMetricsControllerTest {
	
	@Mock
	private LocationService locationService;
	
	@Mock
	private QueueEntryService queueEntryService;
	
	private QueueEntryMetricsController controller;
	
	private static final String LOCATION_UUID = "location-uuid-1234";
	
	private static final String PATIENT_UUID = "patient-uuid-abcd";
	
	private static final String ENTRY_UUID = "entry-uuid-efgh";
	
	private Location mockLocation;
	
	private QueueEntry mockEntry;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		controller = new QueueEntryMetricsController(locationService, queueEntryService);
		
		mockLocation = new Location();
		mockLocation.setUuid(LOCATION_UUID);
		mockLocation.setName("Outpatient Clinic");
		when(locationService.getLocationByUuid(LOCATION_UUID)).thenReturn(mockLocation);
		
		Patient patient = new Patient();
		patient.setUuid(PATIENT_UUID);
		PersonName name = new PersonName("John", null, "Doe");
		name.setPreferred(true);
		patient.addName(name);
		patient.setGender("M");
		PatientIdentifier identifier = new PatientIdentifier();
		identifier.setIdentifier("OM-10045");
		identifier.setPreferred(true);
		patient.addIdentifier(identifier);
		
		Queue queue = new Queue();
		queue.setUuid("queue-uuid-1111");
		queue.setName("Triage");
		queue.setLocation(mockLocation);
		
		Concept status = new Concept();
		status.setUuid("status-uuid-5678");
		ConceptName statusName = new ConceptName();
		statusName.setName("Waiting");
		statusName.setLocale(Locale.ENGLISH);
		status.addName(statusName);
		
		Concept priority = new Concept();
		priority.setUuid("priority-uuid-9999");
		ConceptName priorityName = new ConceptName();
		priorityName.setName("Normal");
		priorityName.setLocale(Locale.ENGLISH);
		priority.addName(priorityName);
		
		mockEntry = new QueueEntry();
		mockEntry.setUuid(ENTRY_UUID);
		mockEntry.setPatient(patient);
		mockEntry.setQueue(queue);
		mockEntry.setStatus(status);
		mockEntry.setPriority(priority);
		mockEntry.setStartedAt(new Date(System.currentTimeMillis() - 30 * 60 * 1000L));
		mockEntry.setSortWeight(0.0);
	}
	
	@Test
	public void getQueueMetrics_shouldReturn400WhenLocationUuidIsNull() {
		ResponseEntity<List<QueueEntryMetricsResponse>> response = controller.getQueueMetrics(null, null, null);
		assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
	}
	
	@Test
	public void getQueueMetrics_shouldReturn400WhenLocationUuidIsBlank() {
		ResponseEntity<List<QueueEntryMetricsResponse>> response = controller.getQueueMetrics("   ", null, null);
		assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
	}
	
	@Test
	public void getQueueMetrics_shouldReturn404WhenLocationNotFound() {
		when(locationService.getLocationByUuid("unknown-uuid")).thenReturn(null);
		ResponseEntity<List<QueueEntryMetricsResponse>> response = controller.getQueueMetrics("unknown-uuid", null, null);
		assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
	}
	
	@Test
	public void getQueueMetrics_shouldReturnEmptyListWhenNoEntries() {
		when(queueEntryService.getQueueEntries(any(QueueEntrySearchCriteria.class))).thenReturn(Collections.emptyList());
		ResponseEntity<List<QueueEntryMetricsResponse>> response = controller.getQueueMetrics(LOCATION_UUID, null, null);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody(), is(empty()));
	}
	
	@Test
	public void getQueueMetrics_shouldReturnFlatDtoWithCorrectFields() {
		when(queueEntryService.getQueueEntries(any(QueueEntrySearchCriteria.class)))
		        .thenReturn(Collections.singletonList(mockEntry));
		
		ResponseEntity<List<QueueEntryMetricsResponse>> response = controller.getQueueMetrics(LOCATION_UUID, null, null);
		
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody(), hasSize(1));
		QueueEntryMetricsResponse dto = response.getBody().get(0);
		assertThat(dto.getUuid(), is(ENTRY_UUID));
		assertThat(dto.getPatientUuid(), is(PATIENT_UUID));
		assertThat(dto.getPatientName(), is("John Doe"));
		assertThat(dto.getPatientIdentifier(), is("OM-10045"));
		assertThat(dto.getPatientGender(), is("M"));
		assertThat(dto.getQueueName(), is("Triage"));
		assertThat(dto.getLocationUuid(), is(LOCATION_UUID));
		assertThat(dto.getLocationName(), is("Outpatient Clinic"));
		assertThat(dto.getStatusDisplay(), is("Waiting"));
		assertThat(dto.getPriorityDisplay(), is("Normal"));
		assertThat(dto.getWaitTimeMinutes(), is(greaterThanOrEqualTo(29L)));
		assertThat(dto.getWaitTimeMinutes(), is(lessThanOrEqualTo(31L)));
	}
	
	@Test
	public void getQueueMetrics_shouldReturnMultipleEntries() {
		QueueEntry second = new QueueEntry();
		second.setUuid("entry-uuid-second");
		second.setStartedAt(new Date());
		when(queueEntryService.getQueueEntries(any(QueueEntrySearchCriteria.class)))
		        .thenReturn(Arrays.asList(mockEntry, second));
		ResponseEntity<List<QueueEntryMetricsResponse>> response = controller.getQueueMetrics(LOCATION_UUID, null, null);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody(), hasSize(2));
	}
	
	@Test
	public void getQueueMetrics_shouldHandleNullPatientGracefully() {
		mockEntry.setPatient(null);
		when(queueEntryService.getQueueEntries(any(QueueEntrySearchCriteria.class)))
		        .thenReturn(Collections.singletonList(mockEntry));
		ResponseEntity<List<QueueEntryMetricsResponse>> response = controller.getQueueMetrics(LOCATION_UUID, null, null);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody().get(0).getPatientUuid(), is(nullValue()));
	}
}
