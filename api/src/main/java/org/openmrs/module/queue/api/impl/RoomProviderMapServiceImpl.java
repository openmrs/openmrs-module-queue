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

import java.util.Date;
import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.Provider;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.queue.api.RoomProviderMapService;
import org.openmrs.module.queue.api.dao.RoomProviderMapDao;
import org.openmrs.module.queue.model.QueueRoom;
import org.openmrs.module.queue.model.RoomProviderMap;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
@Setter(AccessLevel.MODULE)
public class RoomProviderMapServiceImpl extends BaseOpenmrsService implements RoomProviderMapService {
	
	private RoomProviderMapDao dao;
	
	public void setDao(RoomProviderMapDao dao) {
		this.dao = dao;
	}
	
	@Override
	public Optional<RoomProviderMap> getRoomProviderMapByUuid(String uuid) {
		return this.dao.get(uuid);
	}
	
	@Override
	public Optional<RoomProviderMap> getRoomProviderMapById(int id) {
		return this.dao.get(id);
	}
	
	@Override
	public RoomProviderMap createRoomProviderMap(RoomProviderMap roomProviderMap) {
		if (roomProviderMap.getId() == null) {
			List<RoomProviderMap> existingAssignedRooms = getRoomProvider(roomProviderMap.getProvider(),
			    roomProviderMap.getQueueRoom());
			existingAssignedRooms.forEach(roomProviderMap1 -> voidRoomProviderMap(roomProviderMap1.getUuid(), "Api call"));
			
			return this.dao.createOrUpdate(roomProviderMap);
		}
		roomProviderMap.setDateChanged(new Date());
		return this.dao.createOrUpdate(roomProviderMap);
	}
	
	@Override
	public List<RoomProviderMap> getRoomProvider(Provider provider, QueueRoom queueRoom) {
		return this.dao.getRoomProvider(provider, queueRoom);
	}
	
	@Override
	public void voidRoomProviderMap(String roomProviderMapUuid, String voidReason) {
		this.dao.get(roomProviderMapUuid).ifPresent(obj -> {
			obj.setVoided(true);
			obj.setDateVoided(new Date());
			obj.setVoidReason(voidReason);
			obj.setVoidedBy(Context.getAuthenticatedUser());
			this.dao.createOrUpdate(obj);
		});
	}
	
	@Override
	public void purgeRoomProviderMap(RoomProviderMap roomProviderMap) throws APIException {
		this.dao.delete(roomProviderMap);
	}
}
