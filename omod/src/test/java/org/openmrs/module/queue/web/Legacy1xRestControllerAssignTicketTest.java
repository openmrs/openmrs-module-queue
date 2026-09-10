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
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

import javax.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class Legacy1xRestControllerAssignTicketTest {
	
	private Legacy1xRestController controller;
	
	@Mock
	private QueueServicesWrapper services;
	
	@BeforeEach
	public void setUp() {
		controller = new Legacy1xRestController(services);
	}
	
	private HttpServletRequest mockRequestWithBody(String body) throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
		return request;
	}
	
	@Test
	public void assignTicket_shouldReturn400WhenBodyIsEmpty() throws Exception {
		HttpServletRequest request = mockRequestWithBody("");
		ResponseEntity<?> response = (ResponseEntity<?>) controller.assignTicketToServicePoint(request);
		assertThat(response.getStatusCode(), equalTo(BAD_REQUEST));
	}
	
	@Test
	public void assignTicket_shouldReturn400WhenBodyIsInvalidJson() throws Exception {
		HttpServletRequest request = mockRequestWithBody("not-json{{{");
		ResponseEntity<?> response = (ResponseEntity<?>) controller.assignTicketToServicePoint(request);
		assertThat(response.getStatusCode(), equalTo(BAD_REQUEST));
	}
	
	@Test
	public void assignTicket_shouldReturnOkWhenTicketNumberIsMissing() throws Exception {
		// When ticketNumber is absent, the endpoint intentionally skips assignment and returns 200
		HttpServletRequest request = mockRequestWithBody("{\"servicePointName\":\"Room1\",\"status\":\"pending\"}");
		ResponseEntity<?> response = (ResponseEntity<?>) controller.assignTicketToServicePoint(request);
		assertThat(response.getStatusCode(), equalTo(OK));
	}
	
	@Test
	public void assignTicket_shouldReturn400WhenServicePointNameIsMissing() throws Exception {
		HttpServletRequest request = mockRequestWithBody(
		    "{\"ticketNumber\":\"T001\",\"status\":\"pending\"}");
		ResponseEntity<?> response = (ResponseEntity<?>) controller.assignTicketToServicePoint(request);
		assertThat(response.getStatusCode(), equalTo(BAD_REQUEST));
	}
	
	@Test
	public void assignTicket_shouldReturn400WhenStatusIsNull() throws Exception {
		HttpServletRequest request = mockRequestWithBody(
		    "{\"servicePointName\":\"Room1\",\"ticketNumber\":\"T001\",\"status\":null}");
		ResponseEntity<?> response = (ResponseEntity<?>) controller.assignTicketToServicePoint(request);
		assertThat(response.getStatusCode(), equalTo(BAD_REQUEST));
	}
	
	@Test
	public void assignTicket_shouldReturn400WhenServicePointNameIsNonStringType() throws Exception {
		HttpServletRequest request = mockRequestWithBody(
		    "{\"servicePointName\":123,\"ticketNumber\":\"T001\",\"status\":\"pending\"}");
		ResponseEntity<?> response = (ResponseEntity<?>) controller.assignTicketToServicePoint(request);
		assertThat(response.getStatusCode(), equalTo(BAD_REQUEST));
	}
	
	@Test
	public void assignTicket_shouldReturn400WhenStatusIsEmpty() throws Exception {
		HttpServletRequest request = mockRequestWithBody(
		    "{\"servicePointName\":\"Room1\",\"ticketNumber\":\"T001\",\"status\":\"\"}");
		ResponseEntity<?> response = (ResponseEntity<?>) controller.assignTicketToServicePoint(request);
		assertThat(response.getStatusCode(), equalTo(BAD_REQUEST));
	}
	
	@Test
	public void assignTicket_shouldReturn200WhenAllFieldsAreValid() throws Exception {
		HttpServletRequest request = mockRequestWithBody(
		    "{\"servicePointName\":\"Room1\",\"ticketNumber\":\"T001\",\"status\":\"pending\"}");
		ResponseEntity<?> response = (ResponseEntity<?>) controller.assignTicketToServicePoint(request);
		assertThat(response.getStatusCode(), equalTo(OK));
	}
}
