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

import java.util.Collection;

import org.openmrs.Auditable;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.ConceptNameType;
import org.openmrs.module.queue.model.VisitQueueEntry;

public interface VisitQueueEntryDao<Q extends OpenmrsObject & Auditable> extends BaseQueueDao<Q> {
	
	/**
	 * Finds {@link VisitQueueEntry} by conceptStatus and conceptService.
	 *
	 * @param conceptStatus conceptName for queueEntry conceptStatus concept.
	 * @param conceptService conceptName for queue conceptService concept.
	 * @return {@link Collection} of visitQueueEntries matching specified parameters.
	 */
	Collection<VisitQueueEntry> findVisitQueueEntriesByConceptStatusAndConceptService(String conceptStatus,
	        String conceptService, ConceptNameType conceptNameType, boolean localePreferred);
}
