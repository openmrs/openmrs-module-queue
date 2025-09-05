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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.module.queue.web.resources.parser.QueueEntrySearchCriteriaParser.SEARCH_PARAM_LOCATION;
import static org.openmrs.module.queue.web.resources.parser.QueueEntrySearchCriteriaParser.SEARCH_PARAM_SERVICE;

import javax.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.module.queue.api.QueueService;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.search.QueueSearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.web.resources.parser.QueueSearchCriteriaParser;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;

@ExtendWith(MockitoExtension.class)
public class QueueResourceTest extends BaseQueueResourceTest<Queue, QueueResource> {
	
	private static final String QUEUE_UUID = "6hje567a-fca0-11e5-9e59-08002719a7";
	
	private static final String QUEUE_NAME = "queue name";
	
	private static final String LOCATION_UUID = "kra567a-fca0-11e5-9e59-08002719a9";
	
	@Mock
	private QueueService queueService;
	
	private Queue queue;
	
	@Mock
	private QueueServicesWrapper queueServicesWrapper;
	
	private QueueResource resource;
	
	RequestContext requestContext;
	
	HttpServletRequest request;
	
	Map<String, String[]> parameterMap;
	
	ArgumentCaptor<QueueSearchCriteria> queueSearchCriteriaCaptor;
	
	@BeforeEach
	public void setup() {
		this.cleanup();
		queue = new Queue();
		queue.setUuid(QUEUE_UUID);
		queue.setName(QUEUE_NAME);
		
		this.prepareMocks();
		lenient().when(queueServicesWrapper.getQueueService()).thenReturn(queueService);
		
		QueueSearchCriteriaParser parser = new QueueSearchCriteriaParser(queueServicesWrapper);
		resource = new QueueResource(queueServicesWrapper, parser);
		this.setResource(resource);
		this.setObject(queue);
		
		requestContext = mock(RequestContext.class);
		request = mock(HttpServletRequest.class);
		lenient().when(requestContext.getRequest()).thenReturn(request);
		parameterMap = new HashMap<>();
		lenient().when(request.getParameterMap()).thenReturn(parameterMap);
		queueSearchCriteriaCaptor = ArgumentCaptor.forClass(QueueSearchCriteria.class);
	}
	
	@Test
	public void shouldGetQueueService() {
		assertThat(queueService, notNullValue());
	}
	
	@Test
	public void shouldReturnDefaultRepresentation() {
		verifyDefaultRepresentation("name", "description", "uuid");
	}
	
	@Test
	public void shouldReturnFullRepresentation() {
		verifyFullRepresentation("name", "location", "service", "display", "description", "uuid", "auditInfo");
	}
	
	@Test
	public void shouldReturnNullForCustomRepresentation() {
		CustomRepresentation customRepresentation = new CustomRepresentation("custom-representation");
		assertThat(getResource().getRepresentationDescription(customRepresentation), is(nullValue()));
	}
	
	@Test
	public void shouldNOTReturnNullForDefaultRepresentation() {
		assertThat(getResource().getRepresentationDescription(new DefaultRepresentation()), is(notNullValue()));
	}
	
	@Test
	public void shouldNOTReturnNullForFullRepresentation() {
		assertThat(getResource().getRepresentationDescription(new FullRepresentation()), is(notNullValue()));
	}
	
	@Test
	public void shouldNOTReturnNullForRefRepresentation() {
		assertThat(getResource().getRepresentationDescription(new RefRepresentation()), is(notNullValue()));
	}
	
	@Test
	public void shouldGetResourceByUniqueUuid() {
		when(queueService.getQueueByUuid(QUEUE_UUID)).thenReturn(Optional.of(queue));
		
		Queue result = getResource().getByUniqueId(QUEUE_UUID);
		assertThat(result, notNullValue());
		assertThat(result.getUuid(), is(QUEUE_UUID));
		assertThat(result.getName(), is(QUEUE_NAME));
	}
	
	@Test
	public void shouldCreateNewResource() {
		when(queueService.saveQueue(getObject())).thenReturn(getObject());
		
		Queue newlyCreatedObject = getResource().save(getObject());
		assertThat(newlyCreatedObject, notNullValue());
		assertThat(newlyCreatedObject.getUuid(), is(QUEUE_UUID));
		assertThat(newlyCreatedObject.getName(), is(QUEUE_NAME));
	}
	
	@Test
	public void shouldInstantiateNewDelegate() {
		assertThat(getResource().newDelegate(), notNullValue());
	}
	
	@Test
	public void verifyResourceVersion() {
		assertThat(getResource().getResourceVersion(), is("2.3"));
	}
	
	@Test
	public void shouldSearchQueueEntriesByLocation() {
		List<Location> vals = Arrays.asList(new Location(), new Location());
		String[] refs = new String[] { "ref1", "ref2" };
		parameterMap.put(SEARCH_PARAM_LOCATION, refs);
		when(queueServicesWrapper.getLocations(refs)).thenReturn(vals);
		resource.doSearch(requestContext);
		verify(queueService).getQueues(queueSearchCriteriaCaptor.capture());
		QueueSearchCriteria criteria = queueSearchCriteriaCaptor.getValue();
		assertThat(criteria.getLocations(), Matchers.hasSize(2));
		assertThat(criteria.getLocations(), containsInAnyOrder(vals.get(0), vals.get(1)));
	}
	
	@Test
	public void shouldSearchQueueEntriesByService() {
		List<Concept> vals = Arrays.asList(new Concept(), new Concept());
		String[] refs = new String[] { "ref1", "ref2" };
		parameterMap.put(SEARCH_PARAM_SERVICE, refs);
		when(queueServicesWrapper.getConcepts(refs)).thenReturn(vals);
		resource.doSearch(requestContext);
		verify(queueService).getQueues(queueSearchCriteriaCaptor.capture());
		QueueSearchCriteria criteria = queueSearchCriteriaCaptor.getValue();
		assertThat(criteria.getServices(), Matchers.hasSize(2));
		assertThat(criteria.getServices(), containsInAnyOrder(vals.get(0), vals.get(1)));
	}
	
	@Test
	public void shouldGetAllQueues() {
		Queue queueMock = mock(Queue.class);
		when(queueService.getAllQueues()).thenReturn(Collections.singletonList(queueMock));
		Collection<Queue> result = queueService.getAllQueues();
		assertThat(result, hasSize(1));
		assertThat(result, hasItem(queueMock));
	}
}
