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

import java.util.Collection;

import org.openmrs.Auditable;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.ConceptNameType;
import org.openmrs.module.queue.model.QueueEntry;

public interface QueueEntryDao<Q extends OpenmrsObject & Auditable> extends BaseQueueDao<Q> {
	
	/**
	 * Searches queue entries by conceptStatus
	 *
	 * @param conceptStatus the queueEntry conceptStatus
	 * @param includeVoided Include/exclude voided queue entries
	 * @return {@link java.util.Collection} of queue entries with the specified conceptStatus
	 */
	Collection<QueueEntry> SearchQueueEntriesByConceptStatus(@NotNull String conceptStatus, ConceptNameType conceptNameType,
	        boolean localePreferred, boolean includeVoided);
	
	/**
	 * Gets count of queue entries by given status
	 *
	 * @param conceptStatus the queue entry status
	 * @param conceptNameType the conceptNameType e.g. FULLY_SPECIFIED
	 * @param localePreferred locale preferred either true or false
	 * @return {@link java.lang.Long} count of queue entries by status
	 */
	Long getQueueEntriesCountByConceptStatus(@NotNull String conceptStatus, ConceptNameType conceptNameType,
	        boolean localePreferred);
	
	/**
	 * Get active queue entry by patient uuid
	 *
	 * @param patientUuid
	 */
	Collection<QueueEntry> getActiveQueueEntryByPatientUuid(@NotNull String patientUuid);
}
