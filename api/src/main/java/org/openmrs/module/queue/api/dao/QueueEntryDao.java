/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.dao;

import javax.validation.constraints.NotNull;

import java.util.Date;
import java.util.List;

import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;

public interface QueueEntryDao extends BaseQueueDao<QueueEntry> {
	
	/**
	 * @return {@link List} of queue entries that match the given %{@link QueueEntrySearchCriteria}
	 */
	List<QueueEntry> getQueueEntries(@NotNull QueueEntrySearchCriteria searchCriteria);
	
	/**
	 * @return {@link Long} of the number of queue entries that match the given
	 *         %{@link QueueEntrySearchCriteria}
	 */
	Long getCountOfQueueEntries(@NotNull QueueEntrySearchCriteria searchCriteria);
	
	List<QueueEntry> getOverlappingQueueEntries(@NotNull QueueEntrySearchCriteria searchCriteria);
	
	/**
	 * Flushes the current session to ensure pending changes are persisted to the database.
	 */
	void flushSession();
	
	/**
	 * Updates the queue entry only if it hasn't been modified since it was loaded. This provides
	 * optimistic locking to prevent concurrent modifications.
	 *
	 * @param queueEntry the queue entry to update
	 * @param expectedDateChanged the dateChanged value that was present when the entry was loaded
	 * @return true if the update succeeded, false if the entry was modified by another transaction
	 */
	boolean updateIfUnmodified(QueueEntry queueEntry, Date expectedDateChanged);
}
