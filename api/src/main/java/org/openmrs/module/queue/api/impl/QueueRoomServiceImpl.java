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

import java.util.Collection;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.Concept;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.queue.api.QueueRoomService;
import org.openmrs.module.queue.api.dao.QueueRoomDao;
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
	public Collection<QueueRoom> getQueueRoomsByQueue(Concept concept) {
		return null;
	}
	
}
