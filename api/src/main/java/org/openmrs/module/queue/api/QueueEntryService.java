/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api;

import javax.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

import org.openmrs.Location;
import org.openmrs.Visit;
import org.openmrs.VisitAttributeType;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.api.sort.SortWeightGenerator;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.QueueEntryTransition;
import org.openmrs.module.queue.utils.PrivilegeConstants;

public interface QueueEntryService extends OpenmrsService {
	
	/**
	 * Gets a queue entry given uuid.
	 *
	 * @param uuid uuid of the queue entry to be returned.
	 * @return {@link org.openmrs.module.queue.model.QueueEntry}
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUE_ENTRIES })
	Optional<QueueEntry> getQueueEntryByUuid(@NotNull String uuid);
	
	/**
	 * Gets a queue entry by id.
	 *
	 * @param id queueEntryId - the id of the queue entry to retrieve.
	 * @return {@link org.openmrs.module.queue.model.QueueEntry}
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUE_ENTRIES })
	Optional<QueueEntry> getQueueEntryById(@NotNull Integer id);
	
	/**
	 * Saves a queue entry
	 *
	 * @param queueEntry the queue entry to be saved
	 * @return saved {@link org.openmrs.module.queue.model.QueueEntry}
	 */
	@Authorized({ PrivilegeConstants.MANAGE_QUEUE_ENTRIES })
	QueueEntry saveQueueEntry(@NotNull QueueEntry queueEntry);
	
	/**
	 * Transitions a queue entry by ending one queue entry and creating a new queue entry that starts at
	 * that time
	 * 
	 * @param queueEntryTransition the queueEntryTransition
	 * @return the new QueueEntry that is created
	 */
	@Authorized({ PrivilegeConstants.MANAGE_QUEUE_ENTRIES })
	QueueEntry transitionQueueEntry(@NotNull QueueEntryTransition queueEntryTransition);
	
	/**
	 * Undos a transition to the input queue entry by voiding it and making its previous queue entry
	 * active by setting the previous entry's end time to null.
	 * 
	 * @see QueueEntryService#getPreviousQueueEntry(QueueEntry)
	 * @param queueEntry the queue entry to undo transition to. Must be active
	 * @return the previous queue entry, re-activated
	 * @throws IllegalArgumentException if the previous queue entry does not exist
	 * @throws IllegalStateException if multiple previous entries are identified
	 */
	@Authorized({ PrivilegeConstants.MANAGE_QUEUE_ENTRIES })
	QueueEntry undoTransition(@NotNull QueueEntry queueEntry);
	
	/**
	 * Voids a queue entry
	 *
	 * @param queueEntry the queue entry to be voided
	 * @param voidReason the reason for voiding the queue entry
	 */
	@Authorized({ PrivilegeConstants.MANAGE_QUEUE_ENTRIES })
	void voidQueueEntry(@NotNull QueueEntry queueEntry, String voidReason);
	
	/**
	 * Completely remove a queue entry from the database
	 *
	 * @param queueEntry queue entry to be deleted
	 * @throws org.openmrs.api.APIException
	 */
	@Authorized({ PrivilegeConstants.PURGE_QUEUE_ENTRIES })
	void purgeQueueEntry(@NotNull QueueEntry queueEntry) throws APIException;
	
	/**
	 * @return {@link List} of queue entries that match the given %{@link QueueEntrySearchCriteria}
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUE_ENTRIES })
	List<QueueEntry> getQueueEntries(@NotNull QueueEntrySearchCriteria searchCriteria);
	
	/**
	 * @return {@link Long} count of queue entries that match the given
	 *         %{@link QueueEntrySearchCriteria}
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUE_ENTRIES })
	Long getCountOfQueueEntries(@NotNull QueueEntrySearchCriteria searchCriteria);
	
	/**
	 * @param location
	 * @param queue
	 * @return VisitQueueNumber - used to identify patients in the queue instead of using patient name
	 */
	String generateVisitQueueNumber(@NotNull Location location, @NotNull Queue queue, @NotNull Visit visit,
	        @NotNull VisitAttributeType visitAttributeType);
	
	/**
	 * Closes all active queue entries
	 */
	void closeActiveQueueEntries();
	
	/**
	 * @return the instance of SortWeightGenerator that is configured via global property, or null if
	 *         none configured
	 */
	SortWeightGenerator getSortWeightGenerator();
	
	/**
	 * Allows explicitly setting the sortWeightGenerator Typical usage would involve configuring the
	 * sortWeightGenerator via global property but this method exists to enable setting programmatically
	 * if necessary
	 * 
	 * @param sortWeightGenerator the SortWeightGenerator to set
	 */
	void setSortWeightGenerator(SortWeightGenerator sortWeightGenerator);
	
	/**
	 * Given a specified queue entry Q, return its previous queue entry P, where P has same patient and
	 * visit as Q, and P.endedAt time is same as Q.startedAt time, and P.queue is same as
	 * Q.queueComingFrom
	 * 
	 * @param queueEntry
	 * @return the previous queue entry, null otherwise.
	 * @throws IllegalStateException if multiple previous queue entries are identified
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUE_ENTRIES })
	QueueEntry getPreviousQueueEntry(@NotNull QueueEntry queueEntry);
}
