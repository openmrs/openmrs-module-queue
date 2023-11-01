/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.search;

import java.io.Serializable;
import java.util.Collection;

import lombok.Data;
import org.openmrs.Provider;
import org.openmrs.module.queue.model.QueueRoom;

/**
 * Bean definition used for retrieving RoomProviderMaps that meet specific criteria All properties
 * should be considered to further limit results (ANDed together) Any property that is null will not
 * limit by the related property Any Collection that is empty will return only those results for
 * which the related property is null Any Boolean property without a default value will not limit if
 * the property is null For example, to not limit by priority, set the priority property to null To
 * limit to only those entries whose priority is null, set the priority property to an empty
 * collection
 */
@Data
public class RoomProviderMapSearchCriteria implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Collection<QueueRoom> queueRooms;
	
	private Collection<Provider> providers;
	
	private boolean includeVoided = false;
}
