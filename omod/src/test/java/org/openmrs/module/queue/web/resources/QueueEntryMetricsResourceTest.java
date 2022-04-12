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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.VisitQueueEntryService;
import org.openmrs.module.queue.web.resources.custom.response.GenericSingleObjectResult;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, RestUtil.class })
public class QueueEntryMetricsResourceTest {
	
	private static final String STATUS = "Waiting";
	
	private static final String SERVICE = "triage";
	
	private QueueEntryMetricsResource resource;
	
	@Mock
	private VisitQueueEntryService visitQueueEntryService;
	
	@Before
	public void prepareMocks() {
		PowerMockito.mockStatic(RestUtil.class);
		PowerMockito.mockStatic(Context.class);
		
		resource = new QueueEntryMetricsResource();
		//By pass authentication
		when(Context.isAuthenticated()).thenReturn(true);
		when(Context.getService(VisitQueueEntryService.class)).thenReturn(visitQueueEntryService);
	}
	
	@Test
	public void shouldReturnQueueEntryMetricsByStatus() {
		RequestContext requestContext = mock(RequestContext.class);
		
		when(visitQueueEntryService.getVisitQueueEntriesCountByStatus(STATUS)).thenReturn(0L);
		when(requestContext.getParameter("status")).thenReturn(STATUS);
		
		GenericSingleObjectResult result = (GenericSingleObjectResult) resource.doSearch(requestContext);
		
		assertThat(result, notNullValue());
		assertThat(result.getPropValues(), hasSize(2));
		assertTrue(result.getPropValues().stream().anyMatch((propValue -> propValue.getValue().equals(0L))));
	}
	
	@Test
	public void shouldReturnQueueEntryMetricsByService() {
		RequestContext requestContext = mock(RequestContext.class);
		
		when(visitQueueEntryService.getVisitQueueEntriesCountByStatus(SERVICE)).thenReturn(0L);
		when(requestContext.getParameter("service")).thenReturn(SERVICE);
		
		GenericSingleObjectResult result = (GenericSingleObjectResult) resource.doSearch(requestContext);
		
		assertThat(result, notNullValue());
		assertThat(result.getPropValues(), hasSize(2));
		assertTrue(result.getPropValues().stream().anyMatch((propValue -> propValue.getValue().equals(0L))));
	}
	
}
