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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.queue.api.RoomProviderMapService;
import org.openmrs.module.queue.api.dao.RoomProviderMapDao;
import org.openmrs.module.queue.api.search.RoomProviderMapSearchCriteria;
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
	
	@Transactional(readOnly = true)
	@Override
	public Optional<RoomProviderMap> getRoomProviderMapByUuid(String uuid) {
		return dao.get(uuid);
	}
	
	@Transactional(readOnly = true)
	@Override
	public Optional<RoomProviderMap> getRoomProviderMapById(int id) {
		return dao.get(id);
	}
	
	@Override
	public RoomProviderMap createRoomProviderMap(RoomProviderMap roomProviderMap) {
		if (roomProviderMap.getId() == null) {
			RoomProviderMapSearchCriteria criteria = new RoomProviderMapSearchCriteria();
			criteria.setProviders(Collections.singletonList(roomProviderMap.getProvider()));
			criteria.setQueueRooms(Collections.singletonList(roomProviderMap.getQueueRoom()));
			for (RoomProviderMap existingAssignedRoom : getRoomProviderMaps(criteria)) {
				voidRoomProviderMap(existingAssignedRoom, "Api call");
			}
		}
		return dao.createOrUpdate(roomProviderMap);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<RoomProviderMap> getAllRoomProviderMaps() {
		return getRoomProviderMaps(new RoomProviderMapSearchCriteria());
	}
	
	@Transactional(readOnly = true)
	@Override
	public List<RoomProviderMap> getRoomProviderMaps(RoomProviderMapSearchCriteria searchCriteria) {
		return dao.getRoomProviderMaps(searchCriteria);
	}
	
	@Override
	public void voidRoomProviderMap(RoomProviderMap roomProviderMap, String voidReason) {
		roomProviderMap.setVoided(true);
		roomProviderMap.setDateVoided(new Date());
		roomProviderMap.setVoidReason(voidReason);
		roomProviderMap.setVoidedBy(Context.getAuthenticatedUser());
		dao.createOrUpdate(roomProviderMap);
	}
	
	@Override
	public void purgeRoomProviderMap(RoomProviderMap roomProviderMap) throws APIException {
		dao.delete(roomProviderMap);
	}
}
