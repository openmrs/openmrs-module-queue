/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.web;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.api.APIException;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.QueueEntryTransition;
import org.openmrs.module.queue.web.dto.QueueEntryTransitionRequest;
import org.openmrs.module.queue.web.dto.UndoQueueEntryTransitionRequest;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * The main controller that exposes additional end points for order entry
 */
@Controller
public class QueueEntryTransitionRestController extends BaseRestController {
	
	private final QueueServicesWrapper services;
	
	@Autowired
	public QueueEntryTransitionRestController(QueueServicesWrapper services) {
		this.services = services;
	}
	
	@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/queue-entry/transition", method = { RequestMethod.PUT,
	        RequestMethod.POST })
	@ResponseBody
	public Object transitionQueueEntry(@RequestBody Map<String, Object> rawBody) {
		QueueEntryTransitionRequest body = new ObjectMapper().convertValue(rawBody, QueueEntryTransitionRequest.class);
		QueueEntryTransition transition = new QueueEntryTransition();
		
		// Queue Entry to Transition
		String queueEntryUuid = body.getQueueEntryToTransition();
		QueueEntry queueEntry = services.getQueueEntryService().getQueueEntryByUuid(queueEntryUuid)
		        .orElseThrow(() -> new APIException("queueEntryToTransition not specified or found"));
		transition.setQueueEntryToTransition(queueEntry);
		
		// Transition Date
		Date transitionDate = new Date();
		if (body.getTransitionDate() != null) {
			transitionDate = (Date) ConversionUtil.convert(body.getTransitionDate(), Date.class);
		}
		if (transitionDate == null) {
			throw new APIException("Invalid transition date specified: " + body.getTransitionDate());
		}
		transition.setTransitionDate(transitionDate);
		
		// Queue
		if (body.getNewQueue() != null) {
			Optional<Queue> queueOptional = services.getQueueService().getQueueByUuid(body.getNewQueue());
			if (!queueOptional.isPresent()) {
				throw new APIException("Invalid queue specified: " + body.getNewQueue());
			}
			transition.setNewQueue(queueOptional.get());
		}
		
		// Status
		if (body.getNewStatus() != null) {
			Concept concept = services.getConcept(body.getNewStatus());
			if (concept == null) {
				throw new APIException("Invalid status specified: " + body.getNewStatus());
			}
			transition.setNewStatus(concept);
		}
		
		// Priority
		if (body.getNewPriority() != null) {
			Concept concept = services.getConcept(body.getNewPriority());
			if (concept == null) {
				throw new APIException("Invalid priority specified: " + body.getNewPriority());
			}
			transition.setNewPriority(concept);
		}
		
		transition.setNewPriorityComment(body.getNewPriorityComment());
		
		// Location waiting for
		if (rawBody.containsKey(QueueEntryTransitionRequest.NEW_LOCATION_WAITING_FOR_FIELD)) {
			if (body.getNewLocationWaitingFor() == null) {
				transition.setNewLocationWaitingFor(null);
			} else {
				Location location = services.getLocationService().getLocationByUuid(body.getNewLocationWaitingFor());
				if (location == null) {
					throw new APIException("Invalid locationWaitingFor specified: " + body.getNewLocationWaitingFor());
				}
				transition.setNewLocationWaitingFor(location);
			}
		} else {
			transition.setNewLocationWaitingFor(queueEntry.getLocationWaitingFor());
		}
		
		// Provider waiting for
		if (rawBody.containsKey(QueueEntryTransitionRequest.NEW_PROVIDER_WAITING_FOR_FIELD)) {
			if (body.getNewProviderWaitingFor() == null) {
				transition.setNewProviderWaitingFor(null);
			} else {
				Provider provider = services.getProviderService().getProviderByUuid(body.getNewLocationWaitingFor());
				if (provider == null) {
					throw new APIException("Invalid providerWaitingFor specified: " + body.getNewProviderWaitingFor());
				}
				transition.setNewProviderWaitingFor(provider);
			}
		} else {
			transition.setNewProviderWaitingFor(queueEntry.getProviderWaitingFor());
		}
		
		// Execute transition
		QueueEntry newQueueEntry = services.getQueueEntryService().transitionQueueEntry(transition);
		return ConversionUtil.convertToRepresentation(newQueueEntry, Representation.REF);
	}
	
	@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/queue-entry/transition", method = RequestMethod.DELETE)
	@ResponseBody
	public Object undoTransition(@RequestBody UndoQueueEntryTransitionRequest body) {
		QueueEntryService qes = services.getQueueEntryService();
		Optional<QueueEntry> queueEntry = qes.getQueueEntryByUuid(body.getQueueEntry());
		if (queueEntry.isPresent()) {
			QueueEntry unEndedQueueEntry = services.getQueueEntryService().undoTransition(queueEntry.get());
			return ConversionUtil.convertToRepresentation(unEndedQueueEntry, Representation.REF);
		} else {
			throw new APIException("Invalid queue entry");
		}
	}
}
