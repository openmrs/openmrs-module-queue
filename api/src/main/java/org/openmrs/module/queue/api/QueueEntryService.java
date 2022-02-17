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

import java.util.Collection;
import java.util.Optional;

import org.openmrs.api.APIException;
import org.openmrs.module.queue.model.QueueEntry;

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
	 * @param queueEntryUuid uuid of the queue entry to be voided
	 * @param voidReason the reason for voiding the queue entry
	 */
	void voidQueueEntry(@NotNull String queueEntryUuid, String voidReason);
	
	/**
	 * Completely remove a queue entry from the database
	 *
	 * @param queueEntry queue entry to be deleted
	 * @throws org.openmrs.api.APIException
	 */
	void purgeQueueEntry(@NotNull QueueEntry queueEntry) throws APIException;
	
	/**
	 * Search for queue entries by status
	 *
	 * @param status queue entry status
	 * @param includeVoided include/exclude voided queue entries
	 * @return {@link java.util.Collection} of queue entries with the specified statuses
	 */
	Collection<QueueEntry> searchQueueEntries(@NotNull String status, boolean includeVoided);
	
	/**
	 * Gets count of queue entries by status
	 *
	 * @param status the queue entry status
	 * @return {@link java.lang.Long} count of queue entries by specified status
	 */
	Long getQueueEntriesCountByStatus(@NotNull String status);
}
