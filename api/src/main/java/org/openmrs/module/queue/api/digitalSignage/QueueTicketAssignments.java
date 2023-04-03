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
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import javax.swing.*;

/**
 * A utility class for updating details of active queue tickets
 */
public class QueueTicketAssignments {
	
	/**
	 * The object has: service point/room name as key for ease of search and update and object with
	 * status and ticket number
	 */
	private static ObjectNode ACTIVE_QUEUE_TICKETS = JsonNodeFactory.instance.objectNode();
	
	/**
	 * We want to control access to the ACTIVE_QUEUE_TICKETS so that requests are queued
	 * 
	 * @param servicePointName
	 * @param ticketNumber
	 * @param status
	 */
	synchronized public static ObjectNode updateTicketAssignment(String servicePointName, String ticketNumber,
	        String status) {
		if (StringUtils.isNotBlank(servicePointName) && StringUtils.isNotBlank(ticketNumber)
		        && StringUtils.isNotBlank(status)) {
			if (ACTIVE_QUEUE_TICKETS.has(servicePointName)) { // check if there is an existing ticket assignment
				//update the object with new information
				ObjectNode tNode = (ObjectNode) ACTIVE_QUEUE_TICKETS.get(servicePointName);
				tNode.put("status", status);
				tNode.put("ticketNumber", ticketNumber);
				ACTIVE_QUEUE_TICKETS.put(servicePointName, tNode);
			} else {
				// remove the ticket number from any assignment
				
				Iterator<String> keys = ACTIVE_QUEUE_TICKETS.getFieldNames();
				
				while (keys.hasNext()) { // check if the ticket number has active assignment to any of the service rooms
					String key = keys.next();
					ObjectNode obj = (ObjectNode) ACTIVE_QUEUE_TICKETS.get(key);
					if (obj.has(ticketNumber)) {
						// remove the object
						ACTIVE_QUEUE_TICKETS.remove(key);
						break;
					}
				}
				
				// add the new assignment
				ObjectNode ticketAssignment = JsonNodeFactory.instance.objectNode();
				ticketAssignment.put("status", status);
				ticketAssignment.put("ticketNumber", ticketNumber);
				
				ACTIVE_QUEUE_TICKETS.put(servicePointName, ticketAssignment);
				System.out.println("Object: " + ACTIVE_QUEUE_TICKETS.toString());
				
			}
			return ACTIVE_QUEUE_TICKETS;
		}
		ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
		objectNode.put("Error :", "One of the required fields is empty");
		return objectNode;
	}
	
	public static ObjectNode getActiveTicketAssignments() {
		return ACTIVE_QUEUE_TICKETS;
	}
	
	/**
	 * Extracts the request body and return it as string
	 *
	 * @param reader
	 * @return
	 */
	public static String fetchRequestBody(BufferedReader reader) {
		String requestBodyJsonStr = "";
		try {
			
			BufferedReader br = new BufferedReader(reader);
			String output = "";
			while ((output = reader.readLine()) != null) {
				requestBodyJsonStr += output;
			}
		}
		catch (IOException e) {
			
			System.out.println("IOException: " + e.getMessage());
			
		}
		return requestBodyJsonStr;
	}
}
