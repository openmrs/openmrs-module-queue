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

import java.util.List;

import org.openmrs.Auditable;
import org.openmrs.OpenmrsObject;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.utils.QueueEntrySearchCriteria;

public interface QueueEntryDao<Q extends OpenmrsObject & Auditable> extends BaseQueueDao<Q> {
	
	/**
	 * @return {@link List} of queue entries that match the given %{@link QueueEntrySearchCriteria}
	 */
	List<QueueEntry> getQueueEntries(@NotNull QueueEntrySearchCriteria searchCriteria);
	
	/**
	 * @return {@link Long} of the number of queue entries that match the given
	 *         %{@link QueueEntrySearchCriteria}
	 */
	Long getCountOfQueueEntries(@NotNull QueueEntrySearchCriteria searchCriteria);
	
}
