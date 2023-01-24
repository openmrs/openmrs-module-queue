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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Location;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueService;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class QueueResourceTest extends BaseQueueResourceTest<Queue, QueueResource> {
	
	private static final String QUEUE_UUID = "6hje567a-fca0-11e5-9e59-08002719a7";
	
	private static final String QUEUE_NAME = "queue name";
	
	private static final String LOCATION_UUID = "kra567a-fca0-11e5-9e59-08002719a9";
	
	@Mock
	private QueueService queueService;
	
	@Mock
	private LocationService locationService;
	
	private Queue queue;
	
	@Before
	public void setup() {
		queue = new Queue();
		queue.setUuid(QUEUE_UUID);
		queue.setName(QUEUE_NAME);
		
		this.prepareMocks();
		when(Context.getService(QueueService.class)).thenReturn(queueService);
		
		this.setResource(new QueueResource());
		this.setObject(queue);
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
		when(queueService.createQueue(getObject())).thenReturn(getObject());
		
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
	@SuppressWarnings("unchecked")
	public void shouldFindQueuesByLocation() {
		Queue queue = mock(Queue.class);
		Location location = mock(Location.class);
		RequestContext context = mock(RequestContext.class);
		
		when(Context.getLocationService()).thenReturn(locationService);
		when(locationService.getLocationByUuid(LOCATION_UUID)).thenReturn(location);
		when(queue.getLocation()).thenReturn(location);
		when(location.getUuid()).thenReturn(LOCATION_UUID);
		when(context.getParameter("location")).thenReturn(LOCATION_UUID);
		when(queueService.getAllQueuesByLocation(LOCATION_UUID)).thenReturn(Collections.singletonList(queue));
		
		NeedsPaging<Queue> result = (NeedsPaging<Queue>) getResource().doSearch(context);
		
		assertThat(result, notNullValue());
		assertThat(result.getTotalCount(), is(1L));
		result.getPageOfResults().forEach(q -> assertThat(q.getLocation().getUuid(), is(LOCATION_UUID)));
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
