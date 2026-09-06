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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.openmrs.module.queue.web.QueueEntryMetricRestController.AVERAGE_OPEN_WAIT_TIME;
import static org.openmrs.module.queue.web.QueueEntryMetricRestController.AVERAGE_WAIT_TIME;
import static org.openmrs.module.queue.web.QueueEntryMetricRestController.COUNT;
import static org.openmrs.module.queue.web.QueueEntryMetricRestController.COUNTS_BY_STATUS;
import static org.openmrs.module.queue.web.QueueEntryMetricRestController.GROUP_BY;
import static org.openmrs.module.queue.web.QueueEntryMetricRestController.LONGEST_OPEN_WAIT;
import static org.openmrs.module.queue.web.QueueEntryMetricRestController.QUEUE;
import static org.openmrs.module.queue.web.QueueEntryMetricRestController.QUEUES;
import static org.openmrs.module.queue.web.QueueEntryMetricRestController.WAIT_STATUS;
import static org.openmrs.module.queue.web.resources.parser.QueueEntrySearchCriteriaParser.SEARCH_PARAM_LOCATION;
import static org.openmrs.module.queue.web.resources.parser.QueueEntrySearchCriteriaParser.SEARCH_PARAM_QUEUE;
import static org.openmrs.module.queue.web.resources.parser.QueueEntrySearchCriteriaParser.SEARCH_PARAM_SERVICE;
import static org.openmrs.module.queue.web.resources.parser.QueueEntrySearchCriteriaParser.SEARCH_PARAM_STATUS;

import javax.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.QueueRoomService;
import org.openmrs.module.queue.api.QueueService;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.RoomProviderMapService;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.api.search.QueueSearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.web.resources.parser.QueueEntrySearchCriteriaParser;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RestUtil;

@ExtendWith(MockitoExtension.class)
public class QueueEntryMetricRestControllerTest {
	
	private QueueEntryMetricRestController controller;
	
	@Mock
	private QueueService queueService;
	
	@Mock
	private QueueEntryService queueEntryService;
	
	@Mock
	private QueueRoomService queueRoomService;
	
	@Mock
	private RoomProviderMapService roomProviderMapService;
	
	@Mock
	private ConceptService conceptService;
	
	@Mock
	private LocationService locationService;
	
	@Mock
	private PatientService patientService;
	
	@Mock
	private QueueServicesWrapper queueServicesWrapper;
	
	private MockedStatic<RestUtil> restUtil;
	
	private MockedStatic<Context> context;
	
	private MockedStatic<ConversionUtil> conversionUtil;
	
	HttpServletRequest request;
	
	Map<String, String[]> parameterMap;
	
	ArgumentCaptor<QueueEntrySearchCriteria> queueEntryArgumentCaptor;
	
	@BeforeEach
	public void prepareMocks() {
		restUtil = mockStatic(RestUtil.class);
		context = mockStatic(Context.class);
		// The controller adds representations of the queue and of the longest-waiting entry to the
		// response. The conversion needs the REST framework, and this test does not have it.
		// The mock returns its input unchanged. The assertions examine only the metric values.
		conversionUtil = mockStatic(ConversionUtil.class, withSettings().lenient());
		conversionUtil.when(() -> ConversionUtil.convertToRepresentation(any(), any())).thenAnswer(i -> i.getArgument(0));
		lenient().when(queueServicesWrapper.getQueueService()).thenReturn(queueService);
		lenient().when(queueServicesWrapper.getQueueEntryService()).thenReturn(queueEntryService);
		lenient().when(queueServicesWrapper.getQueueRoomService()).thenReturn(queueRoomService);
		lenient().when(queueServicesWrapper.getRoomProviderMapService()).thenReturn(roomProviderMapService);
		lenient().when(queueServicesWrapper.getConceptService()).thenReturn(conceptService);
		lenient().when(queueServicesWrapper.getLocationService()).thenReturn(locationService);
		lenient().when(queueServicesWrapper.getPatientService()).thenReturn(patientService);
		
		//By pass authentication
		context.when(Context::isAuthenticated).thenReturn(true);
		
		QueueEntrySearchCriteriaParser searchCriteriaParser = new QueueEntrySearchCriteriaParser(queueServicesWrapper);
		context.when(() -> Context.getRegisteredComponents(QueueEntrySearchCriteriaParser.class))
		        .thenReturn(Collections.singletonList(searchCriteriaParser));
		
		context.when(() -> Context.getRegisteredComponents(QueueServicesWrapper.class))
		        .thenReturn(Collections.singletonList(queueServicesWrapper));
		
		controller = new QueueEntryMetricRestController(searchCriteriaParser, queueServicesWrapper);
		
		request = mock(HttpServletRequest.class);
		parameterMap = new HashMap<>();
		when(request.getParameterMap()).thenReturn(parameterMap);
		queueEntryArgumentCaptor = ArgumentCaptor.forClass(QueueEntrySearchCriteria.class);
		lenient().when(queueEntryService.getCountOfQueueEntries(any())).thenReturn(50L);
	}
	
	@AfterEach
	public void cleanup() {
		restUtil.close();
		context.close();
		conversionUtil.close();
	}
	
	@Test
	public void shouldRetrieveCountOfQueueEntriesByStatus() {
		List<Concept> vals = Arrays.asList(new Concept(), new Concept());
		String[] refs = new String[] { "ref1", "ref2" };
		parameterMap.put(SEARCH_PARAM_STATUS, refs);
		parameterMap.put(QueueEntryMetricRestController.METRIC, new String[] { COUNT });
		when(queueServicesWrapper.getConcepts(refs)).thenReturn(vals);
		SimpleObject result = (SimpleObject) controller.handleRequest(request);
		assertThat(result.get(COUNT), equalTo(50));
		verify(queueEntryService).getCountOfQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getStatuses(), hasSize(2));
		assertThat(criteria.getStatuses(), containsInAnyOrder(vals.get(0), vals.get(1)));
	}
	
	@Test
	public void shouldReportOnlyTheOriginalMetricsWhenNoneAreNamed() {
		when(queueEntryService.getQueueEntries(any())).thenReturn(Collections.emptyList());
		
		SimpleObject result = (SimpleObject) controller.handleRequest(request);
		
		// The metrics added since are reported only on request, so that a caller naming none keeps
		// receiving exactly what it received before they existed
		assertThat(result.keySet(), containsInAnyOrder(COUNT, AVERAGE_WAIT_TIME));
	}
	
	@Test
	public void shouldStillGroupByQueueWhenOnlyTheCountIsAskedFor() {
		Queue triage = queue("Triage");
		when(queueService.getQueues(any())).thenReturn(Collections.singletonList(triage));
		when(queueEntryService.getQueueEntries(any()))
		        .thenReturn(Collections.singletonList(entry(triage, minutesAgo(10), null)));
		parameterMap.put(GROUP_BY, new String[] { QUEUE });
		parameterMap.put(QueueEntryMetricRestController.METRIC, new String[] { COUNT });
		
		SimpleObject result = (SimpleObject) controller.handleRequest(request);
		
		// Counting alone is otherwise served by a cheaper query that cannot break the total down
		List<SimpleObject> queues = (List<SimpleObject>) result.get(QUEUES);
		assertThat(queues, hasSize(1));
		assertThat(queues.get(0).get(COUNT), equalTo(1));
	}
	
	@Test
	public void shouldReturnMetricsForEachQueueWhenGroupingByQueue() {
		Queue triage = queue("Triage");
		Queue pharmacy = queue("Pharmacy");
		when(queueService.getQueues(any())).thenReturn(Arrays.asList(triage, pharmacy));
		when(queueEntryService.getQueueEntries(any()))
		        .thenReturn(Arrays.asList(entry(triage, minutesAgo(30), null), entry(triage, minutesAgo(10), null)));
		parameterMap.put(GROUP_BY, new String[] { QUEUE });
		parameterMap.put(QueueEntryMetricRestController.METRIC,
		    new String[] { COUNT, AVERAGE_OPEN_WAIT_TIME, LONGEST_OPEN_WAIT });
		
		SimpleObject result = (SimpleObject) controller.handleRequest(request);
		
		assertThat(result.get(COUNT), equalTo(2));
		List<SimpleObject> queues = (List<SimpleObject>) result.get(QUEUES);
		assertThat(queues, hasSize(2));
		// Rows come back sorted by queue name, whatever order the queue search returned them in
		assertThat(((Queue) queues.get(0).get(QUEUE)).getName(), equalTo("Pharmacy"));
		assertThat(((Queue) queues.get(1).get(QUEUE)).getName(), equalTo("Triage"));
		assertThat(queues.get(1).get(COUNT), equalTo(2));
		// A queue that nobody is in still gets a row, rather than dropping out of the list entirely
		assertThat(queues.get(0).get(COUNT), equalTo(0));
		assertThat(queues.get(0).get(AVERAGE_OPEN_WAIT_TIME), is(nullValue()));
		assertThat(queues.get(0).get(LONGEST_OPEN_WAIT), is(nullValue()));
	}
	
	@Test
	public void shouldReportOnlyTheQueuesAskedForWhenTheRequestNamesThem() {
		Queue triage = queue("Triage");
		String[] refs = new String[] { "triage-uuid" };
		when(queueServicesWrapper.getQueues(refs)).thenReturn(Collections.singletonList(triage));
		when(queueEntryService.getQueueEntries(any()))
		        .thenReturn(Collections.singletonList(entry(triage, minutesAgo(10), null)));
		parameterMap.put(SEARCH_PARAM_QUEUE, refs);
		parameterMap.put(GROUP_BY, new String[] { QUEUE });
		parameterMap.put(QueueEntryMetricRestController.METRIC, new String[] { COUNT });
		
		SimpleObject result = (SimpleObject) controller.handleRequest(request);
		
		List<SimpleObject> queues = (List<SimpleObject>) result.get(QUEUES);
		assertThat(queues, hasSize(1));
		assertThat(((Queue) queues.get(0).get(QUEUE)).getName(), equalTo("Triage"));
		assertThat(queues.get(0).get(COUNT), equalTo(1));
		// The request has already named the queues, so there is nothing for the queue search to add
		verify(queueService, never()).getQueues(any(QueueSearchCriteria.class));
	}
	
	@Test
	public void shouldIgnoreAnUnknownGroupByValue() {
		parameterMap.put(GROUP_BY, new String[] { "Queue" });
		parameterMap.put(QueueEntryMetricRestController.METRIC, new String[] { COUNT });
		
		SimpleObject result = (SimpleObject) controller.handleRequest(request);
		
		// An unrecognized groupBy value keeps the flat response and the count fast path
		assertThat(result.keySet(), containsInAnyOrder(COUNT));
		assertThat(result.get(COUNT), equalTo(50));
		verify(queueEntryService).getCountOfQueueEntries(any());
	}
	
	@Test
	public void shouldGiveARowToAQueueTheQueueSearchDoesNotReturn() {
		Queue triage = queue("Triage");
		// A retired queue is the real case: the queue search excludes those, the queue entry search does
		// not, so its entries would count towards the total while belonging to no row
		Queue missingFromQueueSearch = queue("Retired triage");
		when(queueService.getQueues(any())).thenReturn(Collections.singletonList(triage));
		when(queueEntryService.getQueueEntries(any())).thenReturn(
		    Arrays.asList(entry(triage, minutesAgo(30), null), entry(missingFromQueueSearch, minutesAgo(10), null)));
		parameterMap.put(GROUP_BY, new String[] { QUEUE });
		parameterMap.put(QueueEntryMetricRestController.METRIC, new String[] { COUNT });
		
		SimpleObject result = (SimpleObject) controller.handleRequest(request);
		
		List<SimpleObject> queues = (List<SimpleObject>) result.get(QUEUES);
		assertThat(queues, hasSize(2));
		assertThat(result.get(COUNT), equalTo(2));
		assertThat(queues.get(0).get(COUNT), equalTo(1));
		assertThat(queues.get(1).get(COUNT), equalTo(1));
	}
	
	@Test
	public void shouldMeasureOpenWaitsSeparatelyFromCompletedOnes() {
		Queue triage = queue("Triage");
		when(queueEntryService.getQueueEntries(any())).thenReturn(Arrays.asList(
		    // Still waiting, for 40 and 20 minutes so far, averaging 30
		    entry(triage, minutesAgo(40), null), entry(triage, minutesAgo(20), null),
		    // Already seen after a 10 minute wait, so it counts towards the completed average only
		    entry(triage, minutesAgo(70), minutesAgo(60))));
		parameterMap.put(QueueEntryMetricRestController.METRIC,
		    new String[] { AVERAGE_WAIT_TIME, AVERAGE_OPEN_WAIT_TIME, LONGEST_OPEN_WAIT });
		
		SimpleObject result = (SimpleObject) controller.handleRequest(request);
		
		assertThat((Double) result.get(AVERAGE_WAIT_TIME), equalTo(10.0));
		assertThat((Double) result.get(AVERAGE_OPEN_WAIT_TIME), equalTo(30.0));
		SimpleObject longestOpenWait = (SimpleObject) result.get(LONGEST_OPEN_WAIT);
		assertThat(longestOpenWait, is(notNullValue()));
		assertThat((Long) longestOpenWait.get("minutes"), equalTo(40L));
	}
	
	@Test
	public void shouldCountEntriesByStatusConcept() {
		Queue triage = queue("Triage");
		Concept waiting = concept("waiting-uuid");
		Concept inService = concept("in-service-uuid");
		QueueEntry first = entry(triage, minutesAgo(10), null);
		first.setStatus(waiting);
		QueueEntry second = entry(triage, minutesAgo(20), null);
		second.setStatus(waiting);
		QueueEntry third = entry(triage, minutesAgo(5), null);
		third.setStatus(inService);
		when(queueEntryService.getQueueEntries(any())).thenReturn(Arrays.asList(first, second, third));
		parameterMap.put(QueueEntryMetricRestController.METRIC, new String[] { COUNTS_BY_STATUS });
		
		SimpleObject result = (SimpleObject) controller.handleRequest(request);
		
		Map<String, Integer> countsByStatus = (Map<String, Integer>) result.get(COUNTS_BY_STATUS);
		assertThat(countsByStatus.get("waiting-uuid"), equalTo(2));
		assertThat(countsByStatus.get("in-service-uuid"), equalTo(1));
	}
	
	@Test
	public void shouldMeasureOpenWaitsOverOnlyTheStatusesNamedAsWaiting() {
		Queue triage = queue("Triage");
		Concept waiting = concept("waiting-uuid");
		Concept inService = concept("in-service-uuid");
		QueueEntry stillWaiting = entry(triage, minutesAgo(40), null);
		stillWaiting.setStatus(waiting);
		// startedAt is reset when an entry is called in, so this one's 5 minutes are time in service
		QueueEntry beingSeen = entry(triage, minutesAgo(5), null);
		beingSeen.setStatus(inService);
		String[] refs = new String[] { "waiting-uuid" };
		when(queueServicesWrapper.getConcepts(refs)).thenReturn(Collections.singletonList(waiting));
		when(queueEntryService.getQueueEntries(any())).thenReturn(Arrays.asList(stillWaiting, beingSeen));
		when(queueService.getQueues(any())).thenReturn(Collections.singletonList(triage));
		parameterMap.put(WAIT_STATUS, refs);
		parameterMap.put(GROUP_BY, new String[] { QUEUE });
		parameterMap.put(QueueEntryMetricRestController.METRIC,
		    new String[] { COUNT, AVERAGE_OPEN_WAIT_TIME, LONGEST_OPEN_WAIT });
		
		SimpleObject result = (SimpleObject) controller.handleRequest(request);
		
		// The entry being seen is still counted, it is only left out of the two wait measurements
		assertThat(result.get(COUNT), equalTo(2));
		assertThat((Double) result.get(AVERAGE_OPEN_WAIT_TIME), equalTo(40.0));
		assertThat((Long) ((SimpleObject) result.get(LONGEST_OPEN_WAIT)).get("minutes"), equalTo(40L));
		// The per-queue rows are measured the same way as the totals
		SimpleObject triageRow = ((List<SimpleObject>) result.get(QUEUES)).get(0);
		assertThat(triageRow.get(COUNT), equalTo(2));
		assertThat((Double) triageRow.get(AVERAGE_OPEN_WAIT_TIME), equalTo(40.0));
		assertThat((Long) ((SimpleObject) triageRow.get(LONGEST_OPEN_WAIT)).get("minutes"), equalTo(40L));
	}
	
	@Test
	public void shouldLimitTheQueueSearchToTheRequestedLocationAndService() {
		Location clinic = new Location();
		Concept service = concept("service-uuid");
		String[] locationRefs = new String[] { "clinic-uuid" };
		String[] serviceRefs = new String[] { "service-uuid" };
		when(queueServicesWrapper.getLocations(locationRefs)).thenReturn(Collections.singletonList(clinic));
		when(queueServicesWrapper.getConcepts(serviceRefs)).thenReturn(Collections.singletonList(service));
		when(queueService.getQueues(any())).thenReturn(Collections.emptyList());
		when(queueEntryService.getQueueEntries(any())).thenReturn(Collections.emptyList());
		parameterMap.put(SEARCH_PARAM_LOCATION, locationRefs);
		parameterMap.put(SEARCH_PARAM_SERVICE, serviceRefs);
		parameterMap.put(GROUP_BY, new String[] { QUEUE });
		parameterMap.put(QueueEntryMetricRestController.METRIC, new String[] { COUNT });
		
		controller.handleRequest(request);
		
		// The rows have to be drawn from the same queues the entries were counted over
		ArgumentCaptor<QueueSearchCriteria> captor = ArgumentCaptor.forClass(QueueSearchCriteria.class);
		verify(queueService).getQueues(captor.capture());
		assertThat(captor.getValue().getLocations(), containsInAnyOrder(clinic));
		assertThat(captor.getValue().getServices(), containsInAnyOrder(service));
	}
	
	@Test
	public void shouldSkipABlankQueueRef() {
		Queue triage = queue("Triage");
		String[] refs = new String[] { "triage-uuid", "" };
		// The wrapper resolves a blank ref to a null element
		when(queueServicesWrapper.getQueues(refs)).thenReturn(Arrays.asList(triage, null));
		when(queueEntryService.getQueueEntries(any()))
		        .thenReturn(Collections.singletonList(entry(triage, minutesAgo(10), null)));
		parameterMap.put(SEARCH_PARAM_QUEUE, refs);
		parameterMap.put(GROUP_BY, new String[] { QUEUE });
		parameterMap.put(QueueEntryMetricRestController.METRIC, new String[] { COUNT });
		
		SimpleObject result = (SimpleObject) controller.handleRequest(request);
		
		List<SimpleObject> queues = (List<SimpleObject>) result.get(QUEUES);
		assertThat(queues, hasSize(1));
		assertThat(((Queue) queues.get(0).get(QUEUE)).getName(), equalTo("Triage"));
		assertThat(queues.get(0).get(COUNT), equalTo(1));
	}
	
	@Test
	public void shouldSkipABlankWaitStatusRef() {
		Queue triage = queue("Triage");
		Concept waiting = concept("waiting-uuid");
		QueueEntry stillWaiting = entry(triage, minutesAgo(40), null);
		stillWaiting.setStatus(waiting);
		String[] refs = new String[] { "" };
		// The wrapper resolves a blank ref to a null element
		when(queueServicesWrapper.getConcepts(refs)).thenReturn(Collections.singletonList((Concept) null));
		when(queueEntryService.getQueueEntries(any())).thenReturn(Collections.singletonList(stillWaiting));
		parameterMap.put(WAIT_STATUS, refs);
		parameterMap.put(QueueEntryMetricRestController.METRIC, new String[] { AVERAGE_OPEN_WAIT_TIME, LONGEST_OPEN_WAIT });
		
		SimpleObject result = (SimpleObject) controller.handleRequest(request);
		
		// Naming no usable status leaves the wait metrics measured over every entry
		assertThat((Double) result.get(AVERAGE_OPEN_WAIT_TIME), equalTo(40.0));
		assertThat((Long) ((SimpleObject) result.get(LONGEST_OPEN_WAIT)).get("minutes"), equalTo(40L));
	}
	
	private Queue queue(String name) {
		Queue queue = new Queue();
		queue.setName(name);
		return queue;
	}
	
	private Concept concept(String uuid) {
		Concept concept = new Concept();
		concept.setUuid(uuid);
		return concept;
	}
	
	private QueueEntry entry(Queue queue, Date startedAt, Date endedAt) {
		QueueEntry queueEntry = new QueueEntry();
		queueEntry.setQueue(queue);
		queueEntry.setStartedAt(startedAt);
		queueEntry.setEndedAt(endedAt);
		return queueEntry;
	}
	
	private Date minutesAgo(int minutes) {
		return new Date(System.currentTimeMillis() - (minutes * 60L * 1000L));
	}
}
