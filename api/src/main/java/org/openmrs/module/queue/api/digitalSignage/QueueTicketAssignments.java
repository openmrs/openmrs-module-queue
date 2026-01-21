/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.digitalSignage;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * A utility class for updating details of active queue tickets
 */
public class QueueTicketAssignments {
	
	/**
	 * The object has: service point/room name as key for ease of search and update and object with
	 * status, ticket number, and location
	 */
	private static final Map<String, TicketAssignment> ACTIVE_QUEUE_TICKETS = new HashMap<>();
	
	/**
	 * We want to control access to the ACTIVE_QUEUE_TICKETS so that requests are queued
	 *
	 * @param servicePointName
	 * @param ticketNumber
	 * @param status
	 * @param locationUuid - optional location UUID to associate with the ticket
	 */
	synchronized public static void updateTicketAssignment(String servicePointName, String ticketNumber, String status,
	        String locationUuid) {
		
		if (StringUtils.isNotBlank(servicePointName) && StringUtils.isNotBlank(ticketNumber)
		        && StringUtils.isNotBlank(status)) {
			
			// Remove the ticket number from any assignment
			ACTIVE_QUEUE_TICKETS.entrySet().removeIf(entry -> ticketNumber.equals(entry.getValue().getTicketNumber()));
			
			// If ticket is completed, no need to reassign
			if ("completed".equalsIgnoreCase(status)) {
				return;
			}
			
			// Assign ticket to a room if the room already exists
			if (ACTIVE_QUEUE_TICKETS.containsKey(servicePointName)) {
				TicketAssignment tAssignment = ACTIVE_QUEUE_TICKETS.get(servicePointName);
				tAssignment.setStatus(status);
				tAssignment.setTicketNumber(ticketNumber);
				tAssignment.setLocationUuid(locationUuid);
				ACTIVE_QUEUE_TICKETS.put(servicePointName, tAssignment);
			} else {
				// Else create a new assignment
				TicketAssignment ticketAssignment = new TicketAssignment(status, ticketNumber, locationUuid);
				ACTIVE_QUEUE_TICKETS.put(servicePointName, ticketAssignment);
			}
		}
	}
	
	/**
	 * Overloaded method for backward compatibility - calls main method with null location
	 */
	synchronized public static void updateTicketAssignment(String servicePointName, String ticketNumber, String status) {
		updateTicketAssignment(servicePointName, ticketNumber, status, null);
	}
	
	public static Map<String, TicketAssignment> getActiveTicketAssignments() {
		return ACTIVE_QUEUE_TICKETS;
	}
	
	/**
	 * Get active ticket assignments filtered by location
	 *
	 * @param locationUuid - the location UUID to filter by
	 * @return Map of filtered ticket assignments for the specified location
	 */
	public static Map<String, TicketAssignment> getActiveTicketAssignmentsByLocation(String locationUuid) {
		if (StringUtils.isBlank(locationUuid)) {
			return ACTIVE_QUEUE_TICKETS;
		}
		
		return ACTIVE_QUEUE_TICKETS.entrySet().stream().filter(entry -> {
			String ticketLocation = entry.getValue().getLocationUuid();
			return ticketLocation != null && ticketLocation.equals(locationUuid);
		}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
	
	/**
	 * Extracts the request body and returns it as a string
	 *
	 * @param reader
	 * @return
	 */
	public static String fetchRequestBody(BufferedReader reader) {
		StringBuilder requestBodyJsonStr = new StringBuilder();
		try {
			String output;
			while ((output = reader.readLine()) != null) {
				requestBodyJsonStr.append(output);
			}
		}
		catch (IOException e) {
			System.out.println("IOException: " + e.getMessage());
		}
		return requestBodyJsonStr.toString();
	}
	
	public static class TicketAssignment {
		
		private String status;
		
		private String ticketNumber;
		
		private String locationUuid;
		
		public TicketAssignment(String status, String ticketNumber, String locationUuid) {
			this.status = status;
			this.ticketNumber = ticketNumber;
			this.locationUuid = locationUuid;
		}
		
		// Backward compatible constructor
		public TicketAssignment(String status, String ticketNumber) {
			this(status, ticketNumber, null);
		}
		
		public String getStatus() {
			return status;
		}
		
		public void setStatus(String status) {
			this.status = status;
		}
		
		public String getTicketNumber() {
			return ticketNumber;
		}
		
		public void setTicketNumber(String ticketNumber) {
			this.ticketNumber = ticketNumber;
		}
		
		public String getLocationUuid() {
			return locationUuid;
		}
		
		public void setLocationUuid(String locationUuid) {
			this.locationUuid = locationUuid;
		}
	}
}
