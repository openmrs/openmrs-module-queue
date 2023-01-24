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
import org.openmrs.module.queue.model.VisitQueueEntry;

public interface VisitQueueEntryService {
	
	/**
	 * Gets a visit queue entry given UUID.
	 *
	 * @param uuid uuid of the visit queue entry to be returned.
	 * @return {@link org.openmrs.module.queue.model.VisitQueueEntry}
	 */
	Optional<VisitQueueEntry> getVisitQueueEntryByUuid(@NotNull String uuid);
	
	/**
	 * Saves a visit queue entry record
	 *
	 * @param visitQueueEntry the visit queue entry to be saved
	 * @return saved {@link org.openmrs.module.queue.model.VisitQueueEntry}
	 */
	VisitQueueEntry createVisitQueueEntry(@NotNull VisitQueueEntry visitQueueEntry);
	
	/**
	 * Finds all {@link VisitQueueEntry} in the database
	 *
	 * @return {@link Collection} of all visit queue entries in the db
	 */
	Collection<VisitQueueEntry> findAllVisitQueueEntries();
	
	/**
	 * Gets only active queue entries (If endDate is null & not voided)
	 * 
	 * @return {@link Collection} of queue entries
	 */
	Collection<VisitQueueEntry> getActiveVisitQueueEntries();
	
	/**
	 * Find visitQueueEntries by service and status i.e. Find patients waiting for(status) certain
	 * service
	 *
	 * @param status concept name for queueEntry status
	 * @param service concept name for queue service
	 * @param locationUuid location uuid for the queue location
	 * @return {@link Collection} visitQueueEntries matching specified parameters
	 */
	Collection<VisitQueueEntry> findVisitQueueEntries(String status, String service, String locationUuid);
	
	/**
	 * Voids a visit queue entry record
	 *
	 * @param visitQueueEntryUuid uuid of the queue entry to be voided
	 * @param voidReason the reason for voiding the queue entry
	 */
	void voidVisitQueueEntry(@NotNull String visitQueueEntryUuid, String voidReason);
	
	/**
	 * Completely remove a visit queue entry from the database
	 *
	 * @param visitQueueEntry visit queue entry to be deleted
	 * @throws org.openmrs.api.APIException
	 */
	void purgeQueueEntry(@NotNull VisitQueueEntry visitQueueEntry) throws APIException;
	
	/**
	 * Gets count of visit queue entries filtered by status
	 *
	 * @param status concept status name
	 * @return {@link Long} count of visit queue entries
	 */
	Long getVisitQueueEntriesCountByStatus(@NotNull String status);
	
	/**
	 * Gets count of visit queue entries filtered by service
	 *
	 * @param service concept service name
	 * @return {@link Long} count of visit queue entries
	 */
	Long getVisitQueueEntriesCountByService(@NotNull String service);
	
	/**
	 * Gets count of visit queue entries filtered by location
	 *
	 * @param locationUuid location uuid
	 * @return {@link Long} count of visit queue entries
	 */
	Long getVisitQueueEntriesCountByLocation(@NotNull String locationUuid);
	
	/**
	 * Gets count of visit queue entries filtered by location, service and status
	 *
	 * @param service concept service name
	 * @param status concept status name
	 * @param locaitonUuid location uuid
	 * @return {@link Long} count of visit queue entries
	 */
	Long getVisitQueueEntriesCountByLocationStatusAndService(@NotNull String status, String service, String locationUUid);
	
}
