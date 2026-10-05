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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.LocationService;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.web.dto.QueueEntryMetricsResponse;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/queue/metrics")
public class QueueEntryMetricsController {
	
	private final LocationService locationService;
	
	private final QueueEntryService queueEntryService;
	
	@Autowired
	public QueueEntryMetricsController(LocationService locationService, QueueEntryService queueEntryService) {
		this.locationService = locationService;
		this.queueEntryService = queueEntryService;
	}
	
	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<List<QueueEntryMetricsResponse>> getQueueMetrics(
	        @RequestParam(value = "location", required = false) String locationUuid,
	        @RequestParam(value = "status", required = false) String statusUuid,
	        @RequestParam(value = "queue", required = false) String queueUuid) {
		
		if (locationUuid == null || locationUuid.trim().isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
		
		Location location = locationService.getLocationByUuid(locationUuid);
		if (location == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		criteria.setLocations(Collections.singletonList(location));
		criteria.setIsEnded(false);
		
		List<QueueEntry> entries = queueEntryService.getQueueEntries(criteria);
		
		List<QueueEntryMetricsResponse> result = entries.stream().map(this::toMetricsResponse).collect(Collectors.toList());
		
		return ResponseEntity.ok(result);
	}
	
	private QueueEntryMetricsResponse toMetricsResponse(QueueEntry entry) {
		QueueEntryMetricsResponse dto = new QueueEntryMetricsResponse();
		dto.setUuid(entry.getUuid());
		
		Patient patient = entry.getPatient();
		if (patient != null) {
			dto.setPatientUuid(patient.getUuid());
			dto.setPatientName(patient.getPersonName() != null ? patient.getPersonName().getFullName() : null);
			PatientIdentifier preferred = patient.getPatientIdentifier();
			dto.setPatientIdentifier(preferred != null ? preferred.getIdentifier() : null);
			dto.setPatientAge(patient.getAge());
			dto.setPatientGender(patient.getGender());
		}
		
		if (entry.getQueue() != null) {
			dto.setQueueUuid(entry.getQueue().getUuid());
			dto.setQueueName(entry.getQueue().getName());
			if (entry.getQueue().getLocation() != null) {
				dto.setLocationUuid(entry.getQueue().getLocation().getUuid());
				dto.setLocationName(entry.getQueue().getLocation().getName());
			}
		}
		
		if (entry.getStatus() != null) {
			dto.setStatusUuid(entry.getStatus().getUuid());
			dto.setStatusDisplay(getFirstConceptName(entry.getStatus()));
		}
		
		if (entry.getPriority() != null) {
			dto.setPriorityUuid(entry.getPriority().getUuid());
			dto.setPriorityDisplay(getFirstConceptName(entry.getPriority()));
		}
		
		dto.setStartedAt(entry.getStartedAt());
		if (entry.getStartedAt() != null) {
			long diffMs = new Date().getTime() - entry.getStartedAt().getTime();
			dto.setWaitTimeMinutes(TimeUnit.MILLISECONDS.toMinutes(diffMs));
		}
		
		dto.setSortWeight(entry.getSortWeight());
		return dto;
	}
	
	private String getFirstConceptName(Concept concept) {
		if (concept == null || concept.getNames() == null || concept.getNames().isEmpty()) {
			return null;
		}
		ConceptName first = concept.getNames().iterator().next();
		return first != null ? first.getName() : null;
	}
}
