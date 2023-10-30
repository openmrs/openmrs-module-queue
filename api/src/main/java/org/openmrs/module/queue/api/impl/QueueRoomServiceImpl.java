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

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.queue.api.QueueRoomService;
import org.openmrs.module.queue.api.dao.QueueRoomDao;
import org.openmrs.module.queue.api.search.QueueRoomSearchCriteria;
import org.openmrs.module.queue.model.QueueRoom;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Transactional
@Setter(AccessLevel.MODULE)
public class QueueRoomServiceImpl extends BaseOpenmrsService implements QueueRoomService {
	
	private QueueRoomDao dao;
	
	public void setDao(QueueRoomDao dao) {
		this.dao = dao;
	}
	
	@Override
	@Transactional(readOnly = true)
	public Optional<QueueRoom> getQueueRoomByUuid(String uuid) {
		return dao.get(uuid);
	}
	
	@Override
	@Transactional(readOnly = true)
	public Optional<QueueRoom> getQueueRoomById(int id) {
		return dao.get(id);
	}
	
	@Override
	public QueueRoom createQueueRoom(QueueRoom queueRoom) {
		return dao.createOrUpdate(queueRoom);
	}

	@Override
	@Transactional(readOnly = true)
	public List<QueueRoom> getAllQueueRooms() {
		return getQueueRooms(new QueueRoomSearchCriteria());
	}

	@Override
	@Transactional(readOnly = true)
	public List<QueueRoom> getQueueRooms(QueueRoomSearchCriteria searchCriteria) {
		return dao.getQueueRooms(searchCriteria);
	}
	
	@Override
	public void retireQueueRoom(@NotNull QueueRoom queueRoom, String retireReason) {
		queueRoom.setRetired(true);
		queueRoom.setDateRetired(new Date());
		queueRoom.setRetireReason(retireReason);
		queueRoom.setRetiredBy(Context.getAuthenticatedUser());
		dao.createOrUpdate(queueRoom);
	}
	
	@Override
	public void purgeQueueRoom(QueueRoom queueRoom) throws APIException {
		dao.delete(queueRoom);
	}
}
