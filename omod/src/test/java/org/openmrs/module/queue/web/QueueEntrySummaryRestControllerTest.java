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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.web.resources.parser.QueueEntrySearchCriteriaParser;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class QueueEntrySummaryRestControllerTest {
	
	private QueueEntrySummaryRestController controller;
	
	@Mock
	private QueueServicesWrapper queueServicesWrapper;
	
	@Mock
	private QueueEntryService queueEntryService;
	
	@Mock
	private QueueEntrySearchCriteriaParser parser;
	
	private MockedStatic<RestUtil> restUtil;
	
	private MockedStatic<Context> context;
	
	private MockedStatic<ConversionUtil> conversionUtil;
	
	private HttpServletRequest request;
	
	private HttpServletResponse response;
	
	private RequestContext requestContext;
	
	@BeforeEach
	public void prepareMocks() {
		restUtil = mockStatic(RestUtil.class);
		context = mockStatic(Context.class);
		conversionUtil = mockStatic(ConversionUtil.class);
		
		lenient().when(queueServicesWrapper.getQueueEntryService()).thenReturn(queueEntryService);
		context.when(Context::isAuthenticated).thenReturn(true);
		
		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
		Map<String, String[]> parameterMap = new HashMap<>();
		when(request.getParameterMap()).thenReturn(parameterMap);
		when(parser.constructFromRequest(parameterMap)).thenReturn(new QueueEntrySearchCriteria());
		
		requestContext = mock(RequestContext.class);
		lenient().when(requestContext.getStartIndex()).thenReturn(0);
		lenient().when(requestContext.getLimit()).thenReturn(50);
		lenient().when(requestContext.getRequest()).thenReturn(request);
		restUtil.when(() -> RestUtil.getRequestContext(any(), any(), any())).thenReturn(requestContext);
		
		controller = new QueueEntrySummaryRestController(queueServicesWrapper, parser);
	}
	
	@AfterEach
	public void cleanup() {
		restUtil.close();
		context.close();
		conversionUtil.close();
	}
	
	@Test
	public void getQueueEntrySummaries_shouldReturnPaginatedSummariesWithIdentifier() {
		QueueEntry entry1 = new QueueEntry();
		QueueEntry entry2 = new QueueEntry();
		when(queueEntryService.getQueueEntries(any(), anyInt(), anyInt())).thenReturn(Arrays.asList(entry1, entry2));
		Map<QueueEntry, String> previousUuids = new HashMap<>();
		previousUuids.put(entry2, "prev-uuid");
		when(queueEntryService.getPreviousQueueEntryUuids(any(Collection.class))).thenReturn(previousUuids);
		
		conversionUtil.when(() -> ConversionUtil.convertToRepresentation(any(QueueEntry.class), any()))
		        .thenAnswer(invocation -> {
			        SimpleObject row = new SimpleObject();
			        row.add("uuid", "qe-uuid");
			        SimpleObject patient = new SimpleObject();
			        patient.add("uuid", "patient-uuid");
			        patient.add("display", "100GEJ - John Doe");
			        patient.add("patientIdentifier", "100GEJ");
			        row.add("patient", patient);
			        SimpleObject visit = new SimpleObject();
			        visit.add("uuid", "visit-uuid");
			        visit.add("startDatetime", "2026-05-25T08:55:00.000+0000");
			        row.add("visit", visit);
			        return row;
		        });
		
		ResponseEntity<?> resp = controller.getQueueEntrySummaries(request, response);
		
		AlreadyPaged<?> paged = (AlreadyPaged<?>) resp.getBody();
		assertThat(paged, notNullValue());
		List<?> results = paged.getPageOfResults();
		assertThat(results, hasSize(2));
		
		SimpleObject firstRow = (SimpleObject) results.get(0);
		assertThat(firstRow.get("uuid"), is("qe-uuid"));
		assertThat(firstRow.get("visitUuid"), is("visit-uuid"));
		assertThat(firstRow.get("visitStartDatetime"), is("2026-05-25T08:55:00.000+0000"));
		assertThat(firstRow.get("visit"), is(nullValue()));
		assertThat(firstRow.get("previousQueueEntryUuid"), is(nullValue()));
		SimpleObject firstPatient = (SimpleObject) firstRow.get("patient");
		assertThat(firstPatient.get("identifier"), is("100GEJ"));
		assertThat(firstPatient.get("patientIdentifier"), is(nullValue()));
		assertThat(firstPatient.get("display"), is("100GEJ - John Doe"));
		
		SimpleObject secondRow = (SimpleObject) results.get(1);
		assertThat(secondRow.get("previousQueueEntryUuid"), is("prev-uuid"));
	}
	
	@Test
	public void getQueueEntrySummaries_shouldHandleTotalCountRequest() {
		when(queueEntryService.getQueueEntries(any(), anyInt(), anyInt()))
		        .thenReturn(Collections.singletonList(new QueueEntry()));
		when(queueEntryService.getPreviousQueueEntryUuids(any(Collection.class))).thenReturn(Collections.emptyMap());
		when(queueEntryService.getCountOfQueueEntries(any())).thenReturn(137L);
		when(requestContext.getParameter("totalCount")).thenReturn("true");
		conversionUtil.when(() -> ConversionUtil.convertToRepresentation(any(QueueEntry.class), any()))
		        .thenReturn(new SimpleObject());
		
		ResponseEntity<?> resp = controller.getQueueEntrySummaries(request, response);
		
		AlreadyPaged<?> paged = (AlreadyPaged<?>) resp.getBody();
		assertThat(paged.getTotalCount(), equalTo(137L));
		assertThat(paged.hasMoreResults(), is(true));
		verify(queueEntryService).getCountOfQueueEntries(any());
	}
	
	@Test
	public void getQueueEntrySummaries_shouldReturnEmptyListWhenNoResults() {
		when(queueEntryService.getQueueEntries(any(), anyInt(), anyInt())).thenReturn(Collections.emptyList());
		when(queueEntryService.getPreviousQueueEntryUuids(any(Collection.class))).thenReturn(Collections.emptyMap());
		
		ResponseEntity<?> resp = controller.getQueueEntrySummaries(request, response);
		
		AlreadyPaged<?> paged = (AlreadyPaged<?>) resp.getBody();
		assertThat(paged.getPageOfResults(), is(empty()));
		assertThat(paged.hasMoreResults(), is(false));
		conversionUtil.verify(() -> ConversionUtil.convertToRepresentation(any(QueueEntry.class), any()), never());
	}
}
