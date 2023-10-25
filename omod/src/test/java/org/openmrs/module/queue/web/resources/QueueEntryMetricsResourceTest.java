/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.web.resources;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import javax.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
import org.openmrs.module.queue.web.resources.custom.response.GenericSingleObjectResult;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, RestUtil.class })
public class QueueEntryMetricsResourceTest {
	
	private static final String STATUS = "Waiting";
	
	private static final String SERVICE = "Triage";
	
	private QueueEntryMetricsResource resource;
	
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
	
	@Before
	public void prepareMocks() {
		mockStatic(RestUtil.class);
		mockStatic(Context.class);
		when(queueServicesWrapper.getQueueService()).thenReturn(queueService);
		when(queueServicesWrapper.getQueueEntryService()).thenReturn(queueEntryService);
		when(queueServicesWrapper.getQueueRoomService()).thenReturn(queueRoomService);
		when(queueServicesWrapper.getRoomProviderMapService()).thenReturn(roomProviderMapService);
		when(queueServicesWrapper.getConceptService()).thenReturn(conceptService);
		when(queueServicesWrapper.getLocationService()).thenReturn(locationService);
		when(queueServicesWrapper.getPatientService()).thenReturn(patientService);
		
		//By pass authentication
		when(Context.isAuthenticated()).thenReturn(true);
		when(Context.getRegisteredComponents(QueueServicesWrapper.class))
		        .thenReturn(Collections.singletonList(queueServicesWrapper));
		
		resource = new QueueEntryMetricsResource();
	}
	
	@Test
	public void shouldReturnQueueEntryMetricsByStatus() {
		Map<String, String[]> parameterMap = new HashMap<>();
		parameterMap.put("status", new String[] { STATUS });
		
		RequestContext requestContext = mock(RequestContext.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(requestContext.getRequest()).thenReturn(request);
		when(request.getParameterMap()).thenReturn(parameterMap);
		
		Concept statusConcept = new Concept();
		when(queueServicesWrapper.getConcept(STATUS)).thenReturn(statusConcept);
		when(queueEntryService.getCountOfQueueEntries(any())).thenReturn(50L);
		
		GenericSingleObjectResult result = (GenericSingleObjectResult) resource.doSearch(requestContext);
		
		assertThat(result, notNullValue());
		assertThat(result.getPropValues(), hasSize(2));
		assertThat(result.getPropValues().get(0).getProperty(), equalTo("metric"));
		assertThat(result.getPropValues().get(0).getValue(), equalTo(STATUS));
		assertThat(result.getPropValues().get(1).getProperty(), equalTo("count"));
		assertThat(result.getPropValues().get(1).getValue(), equalTo(50L));
	}
	
	@Test
	public void shouldReturnQueueEntryMetricsByService() {
		Map<String, String[]> parameterMap = new HashMap<>();
		parameterMap.put("service", new String[] { SERVICE });
		
		RequestContext requestContext = mock(RequestContext.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(requestContext.getRequest()).thenReturn(request);
		when(request.getParameterMap()).thenReturn(parameterMap);
		
		Concept serviceConcept = new Concept();
		when(queueServicesWrapper.getConcept(SERVICE)).thenReturn(serviceConcept);
		when(queueEntryService.getCountOfQueueEntries(any())).thenReturn(50L);
		
		GenericSingleObjectResult result = (GenericSingleObjectResult) resource.doSearch(requestContext);
		
		assertThat(result, notNullValue());
		assertThat(result.getPropValues(), hasSize(2));
		assertThat(result.getPropValues().get(0).getProperty(), equalTo("metric"));
		assertThat(result.getPropValues().get(0).getValue(), equalTo(SERVICE));
		assertThat(result.getPropValues().get(1).getProperty(), equalTo("count"));
		assertThat(result.getPropValues().get(1).getValue(), equalTo(50L));
	}
	
}
