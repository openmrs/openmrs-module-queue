/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.sort;

import org.openmrs.module.queue.model.QueueEntry;

/**
 * Implementations of this interface provide specific algorithms to generate a sort weight for a
 * given QueueEntry. This sort weight is not required to be unique across queue entries, but is
 * intended to be used by Queue services as the first criteria used when returning an ordered List
 * of QueueEntries, with startedAt, dateCreated, and primary key as subsequent sort criteria. Thus,
 * a typical implementation only needs to return a sort weight that would result in entries being
 * ordered ahead of entries that had an earlier startedAt. Implementations that wish to return
 * QueueEntries based on their priority property should implement this interface to generate a sort
 * weight that is appropriate for the given priority. QueueEntries will typically be ordered based
 * on sort weight descending, so higher priorities should result in a higher returned sort weight
 */
public interface SortWeightGenerator {
	
	/**
	 * Generates a sort weight that will be set on the given QueueEntry when it is saved
	 */
	Double generateSortWeight(QueueEntry queueEntry);
}
