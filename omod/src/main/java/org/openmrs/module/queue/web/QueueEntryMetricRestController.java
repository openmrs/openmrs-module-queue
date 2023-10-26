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

import java.util.Map;

import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.utils.QueueEntrySearchCriteria;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * The main controller that exposes additional end points for order entry
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/queue-entry-metric")
public class QueueEntryMetricRestController extends BaseRestController {
	
	public static final String COUNT = "count";
	
	private final QueueEntrySearchCriteriaParser searchCriteriaParser;
	
	private final QueueServicesWrapper services;
	
	@Autowired
	public QueueEntryMetricRestController(QueueEntrySearchCriteriaParser searchCriteriaParser,
	    QueueServicesWrapper services) {
		this.searchCriteriaParser = searchCriteriaParser;
		this.services = services;
	}
	
	@RequestMapping(method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	@SuppressWarnings("unchecked")
	public Object handleRequest(HttpServletRequest request) {
		Map<String, String[]> parameters = request.getParameterMap();
		Long count = 0L;
		if (searchCriteriaParser.hasSearchParameter(parameters)) {
			QueueEntrySearchCriteria criteria = searchCriteriaParser.constructFromRequest(parameters);
			count = services.getQueueEntryService().getCountOfQueueEntries(criteria);
		}
		SimpleObject ret = new SimpleObject();
		ret.add(COUNT, count);
		return ret;
	}
	
	@Override
	public String getNamespace() {
		return "v1/queue-entry-metric";
	}
}
