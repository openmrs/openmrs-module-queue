/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.model.Queue;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

public class QueueValidatorTest {
	
	private QueueValidator validator;
	
	private Queue queue;
	
	private Errors errors;
	
	private Concept allowedService;
	
	@Mock
	private QueueServicesWrapper queueServices;
	
	@Mock
	private LocationService locationService;
	
	@Before
	public void setup() {
		MockitoAnnotations.openMocks(this);
		validator = new QueueValidator();
		queue = new Queue();
		errors = new BindException(queue, queue.getClass().getName());
		allowedService = new Concept();
		queue.setName("Triage Queue");
		queue.setService(allowedService);
		when(queueServices.getAllowedServices()).thenReturn(Collections.singletonList(allowedService));
	}
	
	@Test
	public void shouldRejectLocationWhenLocationIsNull() {
		try (MockedStatic<Context> context = mockStatic(Context.class)) {
			context.when(() -> Context.getRegisteredComponents(QueueServicesWrapper.class))
			        .thenReturn(Collections.singletonList(queueServices));
			
			queue.setLocation(null);
			validator.validate(queue, errors);
			
			assertThat(errors.getFieldErrorCount("location"), equalTo(2));
			assertThat(errors.getFieldError("location").getCode(), equalTo("queue.location.null"));
		}
	}
	
	@Test
	public void shouldRejectLocationWhenQueueLocationTagDoesNotExist() {
		Location location = mock(Location.class);
		when(location.getTags()).thenReturn(Collections.emptySet());
		queue.setLocation(location);
		
		try (MockedStatic<Context> context = mockStatic(Context.class)) {
			context.when(Context::getLocationService).thenReturn(locationService);
			context.when(() -> Context.getRegisteredComponents(QueueServicesWrapper.class))
			        .thenReturn(Collections.singletonList(queueServices));
			when(locationService.getLocationTagByName("Queue Location")).thenReturn(null);
			
			validator.validate(queue, errors);
			
			assertThat(errors.getFieldErrorCount("location"), equalTo(1));
			assertThat(errors.getFieldError("location").getCode(), equalTo("queue.location.invalid"));
		}
	}
	
	@Test
	public void shouldRejectLocationWhenLocationIsNotTaggedAsQueueLocation() {
		Location location = mock(Location.class);
		LocationTag queueLocationTag = new LocationTag();
		when(location.getTags()).thenReturn(Collections.emptySet());
		queue.setLocation(location);
		
		try (MockedStatic<Context> context = mockStatic(Context.class)) {
			context.when(Context::getLocationService).thenReturn(locationService);
			context.when(() -> Context.getRegisteredComponents(QueueServicesWrapper.class))
			        .thenReturn(Collections.singletonList(queueServices));
			when(locationService.getLocationTagByName("Queue Location")).thenReturn(queueLocationTag);
			
			validator.validate(queue, errors);
			
			assertThat(errors.getFieldErrorCount("location"), equalTo(1));
			assertThat(errors.getFieldError("location").getCode(), equalTo("queue.location.invalid"));
		}
	}
	
	@Test
	public void shouldPassWhenLocationIsTaggedAsQueueLocation() {
		Location location = mock(Location.class);
		LocationTag queueLocationTag = new LocationTag();
		when(location.getTags()).thenReturn(Collections.singleton(queueLocationTag));
		queue.setLocation(location);
		
		try (MockedStatic<Context> context = mockStatic(Context.class)) {
			context.when(Context::getLocationService).thenReturn(locationService);
			context.when(() -> Context.getRegisteredComponents(QueueServicesWrapper.class))
			        .thenReturn(Collections.singletonList(queueServices));
			when(locationService.getLocationTagByName("Queue Location")).thenReturn(queueLocationTag);
			
			validator.validate(queue, errors);
			
			assertThat(errors.getFieldError("location"), equalTo(null));
			assertThat(errors.getErrorCount(), equalTo(0));
		}
	}
}
