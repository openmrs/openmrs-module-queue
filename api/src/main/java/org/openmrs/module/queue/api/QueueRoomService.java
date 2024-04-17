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
import org.openmrs.module.queue.api.search.QueueRoomSearchCriteria;
import org.openmrs.module.queue.model.QueueRoom;
import org.openmrs.module.queue.utils.PrivilegeConstants;

public interface QueueRoomService {
	
	/**
	 * Gets a queue room given a uuid.
	 *
	 * @param uuid the uuid of the queue room to be returned.
	 * @return {@link org.openmrs.module.queue.model.QueueRoom}
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUE_ROOMS })
	Optional<QueueRoom> getQueueRoomByUuid(@NotNull String uuid);
	
	/**
	 * Gets a queue room by id.
	 *
	 * @param id the id of the queue room to retrieve.
	 * @return {@link org.openmrs.module.queue.model.QueueRoom}
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUE_ROOMS })
	Optional<QueueRoom> getQueueRoomById(@NotNull int id);
	
	/**
	 * Gets a List of all Queue Rooms.
	 *
	 * @return {@link List} of all queue rooms
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUE_ROOMS })
	List<QueueRoom> getAllQueueRooms();
	
	/**
	 * Saves a queue room
	 *
	 * @param queueRoom the queue room to be saved
	 * @return saved {@link org.openmrs.module.queue.model.QueueRoom}
	 */
	@Authorized({ PrivilegeConstants.MANAGE_QUEUE_ROOMS })
	QueueRoom saveQueueRoom(@NotNull QueueRoom queueRoom);
	
	/**
	 * Gets a List of all Queue Rooms that match the given QueueRoomSearchCriteria.
	 *
	 * @return {@link List} of queue rooms that match the given
	 *         {@link org.openmrs.module.queue.api.search.QueueRoomSearchCriteria}
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUE_ROOMS })
	List<QueueRoom> getQueueRooms(QueueRoomSearchCriteria searchCriteria);
	
	/**
	 * Retires a queue room.
	 *
	 * @param queueRoom the queue room to retire
	 * @param retireReason the reason for retiring the queue room
	 */
	@Authorized({ PrivilegeConstants.MANAGE_QUEUE_ROOMS })
	void retireQueueRoom(@NotNull QueueRoom queueRoom, String retireReason);
	
	/**
	 * Completely remove a queue room from the database
	 *
	 * @param queueRoom queue room to be deleted
	 * @throws org.openmrs.api.APIException
	 */
	@Authorized({ PrivilegeConstants.PURGE_QUEUE_ROOMS })
	void purgeQueueRoom(@NotNull QueueRoom queueRoom) throws APIException;
}
