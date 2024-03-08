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

import org.openmrs.Concept;
import org.openmrs.api.APIException;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.QueueEntryTransition;
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
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/queue-entry-transition")
public class QueueEntryTransitionRestController extends BaseRestController {
	
	public static final String QUEUE_ENTRY_TO_TRANSITION = "queueEntryToTransition";
	
	public static final String TRANSITION_DATE = "transitionDate";
	
	public static final String NEW_QUEUE = "newQueue";
	
	public static final String NEW_STATUS = "newStatus";
	
	public static final String NEW_PRIORITY = "newPriority";
	
	public static final String NEW_PRIORITY_COMMENT = "newPriorityComment";
	
	private final QueueServicesWrapper services;
	
	@Autowired
	public QueueEntryTransitionRestController(QueueServicesWrapper services) {
		this.services = services;
	}
	
	@RequestMapping(method = { RequestMethod.PUT, RequestMethod.POST })
	@ResponseBody
	public Object transitionQueueEntry(@RequestBody Map<String, String> body) {
		QueueEntryTransition transition = new QueueEntryTransition();
		
		// Queue Entry to Transition
		String queueEntryUuid = body.get(QUEUE_ENTRY_TO_TRANSITION);
		QueueEntry queueEntry = services.getQueueEntryService().getQueueEntryByUuid(queueEntryUuid)
		        .orElseThrow(() -> new APIException(QUEUE_ENTRY_TO_TRANSITION + " is a required parameter"));
		transition.setQueueEntryToTransition(queueEntry);
		
		// Transition Date
		Date transitionDate = new Date();
		if (body.containsKey(TRANSITION_DATE)) {
			transitionDate = (Date) ConversionUtil.convert(body.get(TRANSITION_DATE), Date.class);
		}
		if (transitionDate == null) {
			throw new APIException("Invalid transition date specified: " + body.get(TRANSITION_DATE));
		}
		transition.setTransitionDate(transitionDate);
		
		// Queue
		if (body.containsKey(NEW_QUEUE)) {
			Optional<Queue> queueOptional = services.getQueueService().getQueueByUuid(body.get(NEW_QUEUE));
			if (!queueOptional.isPresent()) {
				throw new APIException("Invalid queue specified: " + body.get(NEW_QUEUE));
			}
			transition.setNewQueue(queueOptional.get());
		}
		
		// Status
		if (body.containsKey(NEW_STATUS)) {
			Concept concept = services.getConcept(body.get(NEW_STATUS));
			if (concept == null) {
				throw new APIException("Invalid status specified: " + body.get(NEW_STATUS));
			}
			transition.setNewStatus(concept);
		}
		
		// Priority
		if (body.containsKey(NEW_PRIORITY)) {
			Concept concept = services.getConcept(body.get(NEW_PRIORITY));
			if (concept == null) {
				throw new APIException("Invalid priority specified: " + body.get(NEW_PRIORITY));
			}
			transition.setNewPriority(concept);
		}
		
		transition.setNewPriorityComment(body.get(NEW_PRIORITY_COMMENT));
		
		// Execute transition
		QueueEntry newQueueEntry = services.getQueueEntryService().transitionQueueEntry(transition);
		return ConversionUtil.convertToRepresentation(newQueueEntry, Representation.REF);
	}
}
