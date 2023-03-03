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
import java.util.List;
import java.util.Optional;

import org.openmrs.Concept;
import org.openmrs.api.APIException;
import org.openmrs.module.queue.model.Queue;

/**
 * This interface defines methods for Queue objects
 */
public interface QueueService {
	
	/**
	 * Gets a queue given UUID.
	 *
	 * @param uuid uuid of the queue to be returned.
	 * @return {@link org.openmrs.module.queue.model.Queue}
	 */
	Optional<Queue> getQueueByUuid(@NotNull String uuid);
	
	/**
	 * Gets a queue by id.
	 *
	 * @param id queueId - the id of the queue to retrieve.
	 * @return {@link org.openmrs.module.queue.model.Queue}
	 */
	Optional<Queue> getQueueById(@NotNull Integer id);
	
	/**
	 * Saves a queue
	 *
	 * @param queue the queue to be saved
	 * @return saved {@link org.openmrs.module.queue.model.Queue}
	 */
	Queue createQueue(@NotNull Queue queue);
	
	/**
	 * Gets all queues related to a specified location.
	 *
	 * @param locationUuid UUID of the location being queried.
	 * @return {@link java.util.List} of {@link org.openmrs.module.queue.model.Queue}
	 */
	List<Queue> getAllQueuesByLocation(@NotNull String locationUuid);
	
	/**
	 * Gets all queues
	 *
	 * @return all queues
	 */
	Collection<Queue> getAllQueues();
	
	/**
	 * Voids a queue
	 *
	 * @param queueUuid uuid of the queue to be voided
	 * @param voidReason the reason for voiding the queue
	 */
	void voidQueue(@NotNull String queueUuid, String voidReason);
	
	/**
	 * Completely remove a queue from the database
	 *
	 * @param queue queue to be deleted
	 * @throws APIException <strong>Should</strong> delete the given queue from the database
	 */
	void purgeQueue(@NotNull Queue queue) throws APIException;
	
	/**
	 * Returns average weight time for patients in a queue
	 * 
	 * @param queue
	 * @return
	 */
	Double getQueueAverageWaitTime(@NotNull Queue queue, Concept status);
}
