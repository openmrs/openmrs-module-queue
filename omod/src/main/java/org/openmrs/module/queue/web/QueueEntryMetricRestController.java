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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.api.search.QueueSearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.utils.QueueUtils;
import org.openmrs.module.queue.web.resources.parser.QueueEntrySearchCriteriaParser;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
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
	
	public static final String METRIC = "metric";
	
	public static final String COUNT = "count";
	
	public static final String AVERAGE_WAIT_TIME = "averageWaitTime";
	
	public static final String AVERAGE_OPEN_WAIT_TIME = "averageOpenWaitTime";
	
	public static final String LONGEST_OPEN_WAIT = "longestOpenWait";
	
	public static final String COUNTS_BY_STATUS = "countsByStatus";
	
	public static final String GROUP_BY = "groupBy";
	
	public static final String WAIT_STATUS = "waitStatus";
	
	public static final String QUEUE = "queue";
	
	public static final String QUEUES = "queues";
	
	// Narrower than REF, which for a queue entry also carries the queue, status, visit and priority,
	// each a lazy load, for every queue reported on
	private static final String LONGEST_OPEN_WAIT_REP = "uuid,display,startedAt,patient:(uuid,display)";
	
	// Wider than REF, which carries neither the location and service that label a row nor the retired
	// flag, but narrower than DEFAULT, which also carries the allowed priorities and statuses
	private static final String QUEUE_REP = "uuid,display,name,description,retired,"
	        + "location:(uuid,display),service:(uuid,display)";
	
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
		SimpleObject ret = new SimpleObject();
		
		String[] metricArray = parameters.get(METRIC);
		List<String> metrics = (metricArray == null ? new ArrayList<>() : Arrays.asList(metricArray));
		
		QueueEntrySearchCriteria criteria = searchCriteriaParser.constructFromRequest(parameters);
		
		String[] groupByArray = parameters.get(GROUP_BY);
		boolean groupByQueue = groupByArray != null && Arrays.asList(groupByArray).contains(QUEUE);
		
		// If we only want count, then use the ore efficient service to get counts
		if (!groupByQueue && metrics.size() == 1 && metrics.get(0).equals(COUNT)) {
			ret.add(COUNT, services.getQueueEntryService().getCountOfQueueEntries(criteria).intValue());
		} else {
			List<QueueEntry> queueEntries = services.getQueueEntryService().getQueueEntries(criteria);
			// One instant for every duration, so the per-queue figures and the totals cannot disagree
			Date asOf = new Date();
			String[] waitStatusArray = parameters.get(WAIT_STATUS);
			List<Concept> waitStatuses = null;
			if (waitStatusArray != null) {
				waitStatuses = new ArrayList<>(services.getConcepts(waitStatusArray));
				// A blank status ref resolves to a null element
				waitStatuses.removeIf(c -> c == null);
			}
			addMetrics(ret, queueEntries, metrics, asOf, waitStatuses);
			if (groupByQueue) {
				ret.add(QUEUES, getMetricsPerQueue(queueEntries, criteria, metrics, asOf, waitStatuses));
			}
		}
		
		return ret;
	}
	
	// Adds the requested metrics, or all of them if none were requested
	private void addMetrics(SimpleObject target, List<QueueEntry> queueEntries, List<String> metrics, Date asOf,
	        List<Concept> waitStatuses) {
		if (metrics.isEmpty() || metrics.contains(COUNT)) {
			target.add(COUNT, queueEntries.size());
		}
		if (metrics.isEmpty() || metrics.contains(AVERAGE_WAIT_TIME)) {
			target.add(AVERAGE_WAIT_TIME, QueueUtils.computeAverageWaitTimeInMinutes(queueEntries));
		}
		// Unlike the two above, the remaining metrics are only reported when asked for, so that a caller
		// that names no metric keeps receiving exactly what it received before they existed.
		// An entry's startedAt is reset when it is called in to be seen, so the two open wait metrics are
		// measured over only the statuses the caller counts as waiting, where it names any.
		List<QueueEntry> waitingEntries = filterByStatus(queueEntries, waitStatuses);
		if (metrics.contains(AVERAGE_OPEN_WAIT_TIME)) {
			target.add(AVERAGE_OPEN_WAIT_TIME, QueueUtils.computeAverageOpenWaitTimeInMinutes(waitingEntries, asOf));
		}
		if (metrics.contains(LONGEST_OPEN_WAIT)) {
			target.add(LONGEST_OPEN_WAIT, getLongestOpenWait(waitingEntries, asOf));
		}
		if (metrics.contains(COUNTS_BY_STATUS)) {
			target.add(COUNTS_BY_STATUS, getCountsByStatus(queueEntries));
		}
	}
	
	private List<QueueEntry> filterByStatus(List<QueueEntry> queueEntries, List<Concept> statuses) {
		if (statuses == null || statuses.isEmpty()) {
			return queueEntries;
		}
		List<QueueEntry> ret = new ArrayList<>();
		for (QueueEntry queueEntry : queueEntries) {
			if (statuses.contains(queueEntry.getStatus())) {
				ret.add(queueEntry);
			}
		}
		return ret;
	}
	
	private SimpleObject getLongestOpenWait(List<QueueEntry> queueEntries, Date asOf) {
		QueueEntry longestWaiting = QueueUtils.findLongestOpenWait(queueEntries);
		if (longestWaiting == null) {
			return null;
		}
		SimpleObject ret = new SimpleObject();
		ret.add("minutes", QueueUtils.computeOpenWaitTimeInMinutes(longestWaiting, asOf));
		ret.add("queueEntry",
		    ConversionUtil.convertToRepresentation(longestWaiting, new CustomRepresentation(LONGEST_OPEN_WAIT_REP)));
		return ret;
	}
	
	// Keyed by status concept rather than by particular named statuses, as which statuses matter is a
	// matter of configuration in the calling application rather than something this module fixes
	private Map<String, Integer> getCountsByStatus(List<QueueEntry> queueEntries) {
		Map<String, Integer> ret = new LinkedHashMap<>();
		for (QueueEntry queueEntry : queueEntries) {
			Concept status = queueEntry.getStatus();
			if (status != null) {
				ret.merge(status.getUuid(), 1, Integer::sum);
			}
		}
		return ret;
	}
	
	// Seeded from the queues so that a queue nobody is in still reports a row of zeroes, then extended
	// by the entries so that everything counted in the totals is also counted in a row: the queue
	// search excludes retired queues, while the queue entry search has no such filter.
	private List<SimpleObject> getMetricsPerQueue(List<QueueEntry> queueEntries, QueueEntrySearchCriteria criteria,
	        List<String> metrics, Date asOf, List<Concept> waitStatuses) {
		Map<Queue, List<QueueEntry>> entriesByQueue = new LinkedHashMap<>();
		for (Queue queue : getQueuesToReport(criteria)) {
			entriesByQueue.put(queue, new ArrayList<>());
		}
		for (QueueEntry queueEntry : queueEntries) {
			entriesByQueue.computeIfAbsent(queueEntry.getQueue(), q -> new ArrayList<>()).add(queueEntry);
		}
		
		List<SimpleObject> ret = new ArrayList<>();
		for (Map.Entry<Queue, List<QueueEntry>> e : entriesByQueue.entrySet()) {
			SimpleObject queueMetrics = new SimpleObject();
			queueMetrics.add(QUEUE, ConversionUtil.convertToRepresentation(e.getKey(), new CustomRepresentation(QUEUE_REP)));
			addMetrics(queueMetrics, e.getValue(), metrics, asOf, waitStatuses);
			ret.add(queueMetrics);
		}
		return ret;
	}
	
	// Queues are limited by those criteria they share with the queue entry search, and are sorted by
	// name so that repeating the same request returns the rows in the same order
	private List<Queue> getQueuesToReport(QueueEntrySearchCriteria criteria) {
		List<Queue> queues;
		if (criteria.getQueues() != null) {
			queues = new ArrayList<>(criteria.getQueues());
			// A blank queue ref resolves to a null element
			queues.removeIf(q -> q == null);
		} else {
			QueueSearchCriteria queueSearchCriteria = new QueueSearchCriteria();
			queueSearchCriteria.setLocations(criteria.getLocations());
			queueSearchCriteria.setServices(criteria.getServices());
			queues = new ArrayList<>(services.getQueueService().getQueues(queueSearchCriteria));
		}
		queues.sort(Comparator.comparing(Queue::getName));
		return queues;
	}
	
	@Override
	public String getNamespace() {
		return "v1/queue-entry-metric";
	}
}
