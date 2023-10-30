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
import org.openmrs.api.APIException;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.utils.QueueEntrySearchCriteria;

public interface QueueEntryService {
	
	/**
	 * Gets a queue entry given uuid.
	 *
	 * @param uuid uuid of the queue entry to be returned.
	 * @return {@link org.openmrs.module.queue.model.QueueEntry}
	 */
	Optional<QueueEntry> getQueueEntryByUuid(@NotNull String uuid);
	
	/**
	 * Gets a queue entry by id.
	 *
	 * @param id queueEntryId - the id of the queue entry to retrieve.
	 * @return {@link org.openmrs.module.queue.model.QueueEntry}
	 */
	Optional<QueueEntry> getQueueEntryById(@NotNull Integer id);
	
	/**
	 * Saves a queue entry
	 *
	 * @param queueEntry the queue entry to be saved
	 * @return saved {@link org.openmrs.module.queue.model.QueueEntry}
	 */
	QueueEntry createQueueEntry(@NotNull QueueEntry queueEntry);
	
	/**
	 * Voids a queue entry
	 *
	 * @param queueEntry the queue entry to be voided
	 * @param voidReason the reason for voiding the queue entry
	 */
	void voidQueueEntry(@NotNull QueueEntry queueEntry, String voidReason);
	
	/**
	 * Completely remove a queue entry from the database
	 *
	 * @param queueEntry queue entry to be deleted
	 * @throws org.openmrs.api.APIException
	 */
	void purgeQueueEntry(@NotNull QueueEntry queueEntry) throws APIException;
	
	/**
	 * @return {@link List} of queue entries that match the given %{@link QueueEntrySearchCriteria}
	 */
	List<QueueEntry> getQueueEntries(@NotNull QueueEntrySearchCriteria searchCriteria);
	
	/**
	 * @return {@link Long} count of queue entries that match the given
	 *         %{@link QueueEntrySearchCriteria}
	 */
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
	
}
