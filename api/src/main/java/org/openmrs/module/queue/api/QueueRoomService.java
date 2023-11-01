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

import org.openmrs.api.APIException;
import org.openmrs.module.queue.api.search.QueueRoomSearchCriteria;
import org.openmrs.module.queue.model.QueueRoom;

public interface QueueRoomService {
	
	Optional<QueueRoom> getQueueRoomByUuid(@NotNull String uuid);
	
	Optional<QueueRoom> getQueueRoomById(@NotNull int id);
	
	List<QueueRoom> getAllQueueRooms();
	
	QueueRoom saveQueueRoom(@NotNull QueueRoom queueRoom);
	
	List<QueueRoom> getQueueRooms(QueueRoomSearchCriteria searchCriteria);
	
	void retireQueueRoom(@NotNull QueueRoom queueRoom, String voidReason);
	
	void purgeQueueRoom(@NotNull QueueRoom queueRoom) throws APIException;
}
