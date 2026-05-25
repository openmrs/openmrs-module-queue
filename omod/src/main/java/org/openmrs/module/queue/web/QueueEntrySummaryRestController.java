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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.web.resources.parser.QueueEntrySearchCriteriaParser;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Lightweight, flat-shape, server-paginated read endpoint for the Service Queues table. Returns
 * only the fields the queue table renders; avoids the deep nested encounter/obs/diagnosis graph
 * that the default {@code QueueEntryResource} representation pulls in. Three-layer batch-load
 * pattern mirrors openmrs-module-emrapi PR #250.
 */
@Controller
public class QueueEntrySummaryRestController extends BaseRestController {
	
	private static final String SUMMARY_REPRESENTATION = "uuid,patient:(uuid,display),queue:(uuid,display),"
	        + "status:(uuid,display),priority:(uuid,display),priorityComment,startedAt,visit:(uuid,startDatetime)";
	
	private final QueueServicesWrapper services;
	
	private final QueueEntrySearchCriteriaParser parser;
	
	@Autowired
	public QueueEntrySummaryRestController(QueueServicesWrapper services, QueueEntrySearchCriteriaParser parser) {
		this.services = services;
		this.parser = parser;
	}
	
	@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/queue-entry-summary", method = RequestMethod.GET)
	public ResponseEntity<?> getQueueEntrySummaries(HttpServletRequest request, HttpServletResponse response) {
		@SuppressWarnings("unchecked")
		Map<String, String[]> parameterMap = request.getParameterMap();
		QueueEntrySearchCriteria criteria = parser.constructFromRequest(parameterMap);
		RequestContext context = RestUtil.getRequestContext(request, response, Representation.DEFAULT);
		
		QueueEntryService queueEntryService = services.getQueueEntryService();
		List<QueueEntry> entries = queueEntryService.getQueueEntries(criteria, context.getStartIndex(), context.getLimit());
		Map<QueueEntry, String> previousUuids = queueEntryService.getPreviousQueueEntryUuids(entries);
		
		CustomRepresentation entryRep = new CustomRepresentation(SUMMARY_REPRESENTATION);
		List<SimpleObject> results = new ArrayList<>(entries.size());
		for (QueueEntry entry : entries) {
			SimpleObject row = (SimpleObject) ConversionUtil.convertToRepresentation(entry, entryRep);
			SimpleObject visit = (SimpleObject) row.remove("visit");
			row.add("visitUuid", visit == null ? null : visit.get("uuid"));
			row.add("visitStartDatetime", visit == null ? null : visit.get("startDatetime"));
			row.add("previousQueueEntryUuid", previousUuids.get(entry));
			results.add(row);
		}
		
		boolean totalCountRequested = Boolean.valueOf(context.getParameter("totalCount"));
		boolean hasMore;
		AlreadyPaged<SimpleObject> paged;
		if (totalCountRequested) {
			Long totalCount = queueEntryService.getCountOfQueueEntries(criteria);
			hasMore = context.getStartIndex() + entries.size() < totalCount;
			paged = new AlreadyPaged<>(context, results, hasMore, totalCount);
		} else {
			hasMore = entries.size() == context.getLimit();
			paged = new AlreadyPaged<>(context, results, hasMore);
		}
		return new ResponseEntity<>(paged, HttpStatus.OK);
	}
}
