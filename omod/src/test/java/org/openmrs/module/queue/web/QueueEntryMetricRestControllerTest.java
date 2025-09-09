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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.module.queue.web.QueueEntryMetricRestController.COUNT;
import static org.openmrs.module.queue.web.resources.parser.QueueEntrySearchCriteriaParser.SEARCH_PARAM_STATUS;

import javax.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
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
import org.openmrs.module.queue.web.resources.parser.QueueEntrySearchCriteriaParser;
import org.openmrs.module.webservices.rest.SimpleObject;
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
	
	HttpServletRequest request;
	
	Map<String, String[]> parameterMap;
	
	ArgumentCaptor<QueueEntrySearchCriteria> queueEntryArgumentCaptor;
	
	@BeforeEach
	public void prepareMocks() {
		restUtil = mockStatic(RestUtil.class);
		context = mockStatic(Context.class);
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
		when(queueEntryService.getCountOfQueueEntries(any())).thenReturn(50L);
	}
	
	@AfterEach
	public void cleanup() {
		restUtil.close();
		context.close();
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
}
