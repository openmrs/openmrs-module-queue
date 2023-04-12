/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.impl;

import javax.validation.constraints.NotNull;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.Location;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.queue.api.QueueRoomService;
import org.openmrs.module.queue.api.dao.QueueRoomDao;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueRoom;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
@Setter(AccessLevel.MODULE)
public class QueueRoomServiceImpl extends BaseOpenmrsService implements QueueRoomService {
	
	private QueueRoomDao dao;
	
	public void setDao(QueueRoomDao dao) {
		this.dao = dao;
	}
	
	@Override
	public Optional<QueueRoom> getQueueRoomByUuid(String uuid) {
		return this.dao.get(uuid);
	}
	
	@Override
	public Optional<QueueRoom> getQueueRoomById(int id) {
		return this.dao.get(id);
	}
	
	@Override
	public QueueRoom createQueueRoom(QueueRoom queueRoom) {
		return this.dao.createOrUpdate(queueRoom);
	}
	
	@Override
	public List<QueueRoom> getQueueRoomsByServiceAndLocation(Queue queue, Location location) {
		return this.dao.getQueueRoomsByServiceAndLocation(queue, location);
	}
	
	@Override
	public void voidQueueRoom(@NotNull String queueRoomUuid, String voidReason) {
		this.dao.get(queueRoomUuid).ifPresent(queueRoom -> {
			queueRoom.setRetired(true);
			queueRoom.setDateRetired(new Date());
			queueRoom.setRetireReason(voidReason);
			queueRoom.setRetiredBy(Context.getAuthenticatedUser());
			this.dao.createOrUpdate(queueRoom);
		});
	}
	
	@Override
	public void purgeQueueRoom(QueueRoom queueRoom) throws APIException {
		this.dao.delete(queueRoom);
	}
}
