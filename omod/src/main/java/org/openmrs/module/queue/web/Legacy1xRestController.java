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

import static org.openmrs.module.queue.web.QueueEntryMetricRestController.AVERAGE_WAIT_TIME;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Location;
import org.openmrs.Visit;
import org.openmrs.VisitAttributeType;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.digitalSignage.QueueTicketAssignments;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.web.resources.QueueEntryResource;
import org.openmrs.module.queue.web.resources.QueueRoomResource;
import org.openmrs.module.queue.web.resources.RoomProviderMapResource;
import org.openmrs.module.queue.web.resources.parser.QueueEntrySearchCriteriaParser;
import org.openmrs.module.queue.web.resources.parser.QueueRoomSearchCriteriaParser;
import org.openmrs.module.queue.web.resources.parser.RoomProviderMapSearchCriteriaParser;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.Converter;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * REST controller that exposes REST endpoints that are compatible with version 1.x of this module
 */
@Controller
@Slf4j
public class Legacy1xRestController extends BaseRestController {
	
	@Autowired
	QueueServicesWrapper services;
	
	@Autowired
	QueueEntrySearchCriteriaParser queueEntrySearchCriteriaParser;
	
	@Autowired
	QueueRoomSearchCriteriaParser queueRoomSearchCriteriaParser;
	
	@Autowired
	RoomProviderMapSearchCriteriaParser roomProviderMapSearchCriteriaParser;
	
	@Autowired
	private QueueEntryMetricRestController queueEntryMetricRestController;
	
	private final QueueEntryResource queueEntryResource;
	
	private final QueueRoomResource queueRoomResource;
	
	private final RoomProviderMapResource roomProviderMapResource;
	
	public Legacy1xRestController() {
		queueEntryResource = new QueueEntryResource(services, queueEntrySearchCriteriaParser);
		queueRoomResource = new QueueRoomResource(services, queueRoomSearchCriteriaParser);
		roomProviderMapResource = new RoomProviderMapResource(services, roomProviderMapSearchCriteriaParser);
	}
	
	@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/visit-queue-entry", method = GET)
	@ResponseBody
	@SuppressWarnings("unchecked")
	public Object getVisitQueueEntries(HttpServletRequest request, HttpServletResponse response) throws Exception {
		SimpleObject result = new SimpleObject();
		List<SimpleObject> visitQueueEntries = new ArrayList<>();
		result.add("results", visitQueueEntries);
		RequestContext requestContext = RestUtil.getRequestContext(request, response, Representation.REF);
		Map<String, String[]> parameters = new HashMap<String, String[]>(requestContext.getRequest().getParameterMap());
		// The queueEntryResource does not limit to active by default, but the legacy resource does
		if (!parameters.containsKey("isEnded")) {
			parameters.put("isEnded", new String[] { "false" });
		}
		QueueEntrySearchCriteria criteria = queueEntryResource.getSearchCriteriaParser().constructFromRequest(parameters);
		List<QueueEntry> queueEntryList = services.getQueueEntryService().getQueueEntries(criteria);
		PageableResult pageableResult = new NeedsPaging<>(queueEntryList, requestContext);
		Map<String, Object> searchResult = pageableResult.toSimpleObject(queueEntryResource);
		List<Map<String, Object>> queueEntries = (List<Map<String, Object>>) PropertyUtils.getProperty(searchResult,
		    "results");
		for (Map<String, Object> queueEntry : queueEntries) {
			SimpleObject visitQueueEntry = new SimpleObject();
			visitQueueEntry.add("visit", queueEntry.get("visit"));
			visitQueueEntry.add("queueEntry", queueEntry);
			visitQueueEntries.add(visitQueueEntry);
		}
		return result;
	}
	
	@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/visit-queue-entry", method = POST)
	@ResponseBody
	public Object postVisitQueueEntry(HttpServletRequest request, HttpServletResponse response,
	        @RequestBody SimpleObject post) {
		RequestContext requestContext = RestUtil.getRequestContext(request, response);
		SimpleObject queueEntry = new SimpleObject();
		Map<String, Object> postedQueueEntry = post.get("queueEntry");
		if (postedQueueEntry != null) {
			for (String key : postedQueueEntry.keySet()) {
				Object value = postedQueueEntry.get(key);
				// Handle complex objects by extracting UUID if present
				Object extractedValue = extractUuidFromComplexObject(value);
				queueEntry.add(key, extractedValue);
				log.debug("Extracted {}: {} -> {}", key, value, extractedValue);
			}
		} else {
			log.warn("postedQueueEntry is null in postVisitQueueEntry");
		}
		// Extract UUID from visit object if it's a complex object
		Object visit = extractUuidFromComplexObject(post.get("visit"));
		if (visit != null) {
			queueEntry.add("visit", visit);
		}
		log.debug("Final queueEntry SimpleObject: {}", queueEntry);
		Object created = queueEntryResource.create(queueEntry, requestContext);
		return RestUtil.created(response, created);
	}
	
	/**
	 * Extracts UUID from complex objects that have a uuid field This handles cases where the frontend
	 * sends complex objects like: {"uuid": "some-uuid", "display": "some-display"} instead of just
	 * "some-uuid"
	 */
	private Object extractUuidFromComplexObject(Object value) {
		if (value instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) value;
			if (map.containsKey("uuid")) {
				return map.get("uuid");
			}
		}
		return value;
	}
	
	@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/queueroom", method = GET)
	@ResponseBody
	public Object getQueueRooms(HttpServletRequest request, HttpServletResponse response) {
		RequestContext requestContext = RestUtil.getRequestContext(request, response, Representation.REF);
		return queueRoomResource.search(requestContext);
	}
	
	@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/roomprovidermap", method = GET)
	@ResponseBody
	public Object getRoomProviderMaps(HttpServletRequest request, HttpServletResponse response) {
		RequestContext requestContext = RestUtil.getRequestContext(request, response, Representation.REF);
		return roomProviderMapResource.search(requestContext);
	}
	
	@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/roomprovidermap", method = POST)
	@ResponseBody
	public Object postRoomProviderMap(HttpServletRequest request, HttpServletResponse response,
	        @RequestBody SimpleObject post) {
		RequestContext requestContext = RestUtil.getRequestContext(request, response);
		// Handle complex objects by extracting UUID if present
		SimpleObject processedPost = new SimpleObject();
		for (String key : post.keySet()) {
			Object value = post.get(key);
			value = extractUuidFromComplexObject(value);
			processedPost.add(key, value);
		}
		Object created = roomProviderMapResource.create(processedPost, requestContext);
		return RestUtil.created(response, created);
	}
	
	@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/roomprovidermap/{uuid}", method = POST)
	@ResponseBody
	public Object updateRoomProviderMap(@PathVariable("uuid") String uuid, @RequestBody SimpleObject post,
	        HttpServletRequest request, HttpServletResponse response) throws ResponseException {
		RequestContext context = RestUtil.getRequestContext(request, response);
		if (post.get("deleted") != null && "false".equals(post.get("deleted")) && post.size() == 1) {
			return RestUtil.updated(response, roomProviderMapResource.undelete(uuid, context));
		}
		// Handle complex objects by extracting UUID if present
		SimpleObject processedPost = new SimpleObject();
		for (String key : post.keySet()) {
			Object value = post.get(key);
			value = extractUuidFromComplexObject(value);
			processedPost.add(key, value);
		}
		return RestUtil.updated(response, roomProviderMapResource.update(uuid, processedPost, context));
	}
	
	@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/queue-entry-metrics", method = { GET, POST })
	@ResponseBody
	public Object getQueueEntryMetrics(HttpServletRequest request) {
		return queueEntryMetricRestController.handleRequest(request);
	}
	
	@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/queue-metrics", method = GET)
	@ResponseBody
	public Object getQueueMetrics(HttpServletRequest request) {
		SimpleObject results = (SimpleObject) getQueueEntryMetrics(request);
		String queueUuid = request.getParameter("queue");
		String queueName = (StringUtils.isNotBlank(queueUuid) ? services.getQueue(queueUuid).getName() : "");
		return new GenericSingleObjectResult(Arrays.asList(new PropValue("queue", queueName),
		    new PropValue("averageWaitTime", results.get(AVERAGE_WAIT_TIME))));
	}
	
	@RequestMapping(method = POST, value = "/rest/" + RestConstants.VERSION_1 + "/queueutil/assignticket")
	@ResponseBody
	public Object assignTicketToServicePoint(HttpServletRequest request) throws Exception {
		String requestBody = QueueTicketAssignments.fetchRequestBody(request.getReader());
		if (requestBody != null) {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode actualObj = mapper.readTree(requestBody);
			
			if (!actualObj.has("ticketNumber")) {
				String msg = "No ticketNumber passed, skipping ticket assignment";
				return new ResponseEntity<Object>(msg, new HttpHeaders(), HttpStatus.OK);
			}
			
			String servicePointName = actualObj.get("servicePointName").textValue();
			String ticketNumber = actualObj.get("ticketNumber").textValue();
			String status = actualObj.get("status").textValue();
			
			if (servicePointName.isEmpty() || ticketNumber.isEmpty() || status.isEmpty()) {
				return new ResponseEntity<Object>("One of the required fields is empty", new HttpHeaders(), BAD_REQUEST);
			}
			
			QueueTicketAssignments.updateTicketAssignment(servicePointName, ticketNumber, status);
			return new ResponseEntity<Object>("Ticket successfully assigned!", new HttpHeaders(), HttpStatus.OK);
		}
		return new ResponseEntity<Object>("The request could not be interpreted", new HttpHeaders(), BAD_REQUEST);
	}
	
	@RequestMapping(method = GET, value = "/rest/" + RestConstants.VERSION_1 + "/queueutil/active-tickets")
	public Object getActiveTickets() {
		return new ResponseEntity<>(QueueTicketAssignments.getActiveTicketAssignments(), new HttpHeaders(), HttpStatus.OK);
	}
	
	@RequestMapping(method = { GET, POST }, value = "/rest/" + RestConstants.VERSION_1 + "/queue-entry-number")
	public Object generateQueueEntryNumber(HttpServletRequest request) {
		String serviceType = "";
		String visitQueueNumber = "";
		String vatUuid = request.getParameter("visitAttributeType");
		if (StringUtils.isNotEmpty(vatUuid)) {
			VisitAttributeType vat = services.getVisitService().getVisitAttributeTypeByUuid(vatUuid);
			if (vat != null) {
				Location l = services.getLocation(request.getParameter("location"));
				Visit v = services.getVisit(request.getParameter("visit"));
				Queue q = services.getQueue(request.getParameter("queue"));
				serviceType = q.getName();
				visitQueueNumber = services.getQueueEntryService().generateVisitQueueNumber(l, q, v, vat);
			}
		}
		return new GenericSingleObjectResult(Arrays.asList(new PropValue("serviceType", serviceType),
		    new PropValue("visitQueueNumber", visitQueueNumber)));
	}
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class GenericSingleObjectResult implements PageableResult {
		
		private List<PropValue> propValues;
		
		@Override
		public SimpleObject toSimpleObject(Converter<?> converter) throws ResponseException {
			SimpleObject ret = new SimpleObject();
			this.propValues.forEach(propValue -> ret.add(propValue.getProperty(), propValue.getValue()));
			return ret;
		}
		
		public void add(@NotNull String property, @NotNull Object value) {
			if (propValues == null) {
				this.propValues = new ArrayList<>();
			}
			this.propValues.add(new PropValue(property, value));
		}
	}
	
	@Data
	@AllArgsConstructor
	public static class PropValue implements Serializable {
		
		private static final long serialVersionUID = 45L;
		
		private String property;
		
		private Object value;
	}
}
