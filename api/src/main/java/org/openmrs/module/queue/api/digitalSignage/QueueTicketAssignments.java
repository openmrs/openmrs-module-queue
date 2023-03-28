package org.openmrs.module.queue.api.digitalSignage;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import java.util.Iterator;

/**
 * A utility class for updating details of active queue tickets
 */
public class QueueTicketAssignments {

    /**
     * The object has:
     * service point/room name as key for ease of search and update
     * and object with status and ticket number
     */
    private static ObjectNode ACTIVE_QUEUE_TICKETS = JsonNodeFactory.instance.objectNode();

    /**
     * We want to control access to the ACTIVE_QUEUE_TICKETS so that requests are queued
     * @param servicePointName
     * @param ticketNumber
     * @param status
     */
    synchronized public static void updateTicketAssignment(String servicePointName, String ticketNumber, String status) {
        if (StringUtils.isNotBlank(servicePointName) && StringUtils.isNotBlank(ticketNumber) && StringUtils.isNotBlank(status)) {
            if (ACTIVE_QUEUE_TICKETS.has(servicePointName)) { // check if there is an existing ticket assignment
                //update the object with new information
                ObjectNode tNode = (ObjectNode) ACTIVE_QUEUE_TICKETS.get(servicePointName);
                tNode.put("status", status);
                tNode.put("ticketNumber", ticketNumber);
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

            }
        }
    }

    public static ObjectNode getActiveTicketAssignments() {
        return ACTIVE_QUEUE_TICKETS;
    }
}
