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

import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.queue.api.search.QueueSearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.utils.PrivilegeConstants;

/**
 * This interface defines methods for Queue objects
 */
public interface QueueService extends OpenmrsService {
	
	/**
	 * Gets a queue given UUID.
	 *
	 * @param uuid uuid of the queue to be returned.
	 * @return {@link org.openmrs.module.queue.model.Queue}
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUES })
	Optional<Queue> getQueueByUuid(@NotNull String uuid);
	
	/**
	 * Gets a queue by id.
	 *
	 * @param id queueId - the id of the queue to retrieve.
	 * @return {@link org.openmrs.module.queue.model.Queue}
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUES })
	Optional<Queue> getQueueById(@NotNull Integer id);
	
	/**
	 * Saves a queue. This is here mainly for backwards-compatibility, it delegates to saveQueue(Queue)
	 *
	 * @param queue the queue to be saved
	 * @return saved {@link org.openmrs.module.queue.model.Queue}
	 */
	@Authorized({ PrivilegeConstants.ADD_QUEUES, PrivilegeConstants.EDIT_QUEUES })
	Queue createQueue(@NotNull Queue queue);
	
	/**
	 * Saves a queue
	 *
	 * @param queue the queue to be saved
	 * @return saved {@link org.openmrs.module.queue.model.Queue}
	 */
	@Authorized({ PrivilegeConstants.ADD_QUEUES, PrivilegeConstants.EDIT_QUEUES })
	Queue saveQueue(@NotNull Queue queue);
	
	/**
	 * @return all queues
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUES })
	List<Queue> getAllQueues();
	
	/**
	 * @return {@link List} of queues that match the given %{@link QueueSearchCriteria}
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUES })
	List<Queue> getQueues(@NotNull QueueSearchCriteria searchCriteria);
	
	/**
	 * Voids a queue
	 *
	 * @param queue the queue to retire
	 * @param retireReason the reason for voiding the queue
	 */
	@Authorized({ PrivilegeConstants.DELETE_QUEUES })
	void retireQueue(@NotNull Queue queue, String retireReason);
	
	/**
	 * Completely remove a queue from the database
	 *
	 * @param queue queue to be deleted
	 * @throws APIException <strong>Should</strong> delete the given queue from the database
	 */
	@Authorized({ PrivilegeConstants.PURGE_QUEUES })
	void purgeQueue(@NotNull Queue queue) throws APIException;
}
