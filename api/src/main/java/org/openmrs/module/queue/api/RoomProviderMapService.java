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
import org.openmrs.module.queue.api.search.RoomProviderMapSearchCriteria;
import org.openmrs.module.queue.model.RoomProviderMap;

public interface RoomProviderMapService {
	
	Optional<RoomProviderMap> getRoomProviderMapByUuid(@NotNull String uuid);
	
	Optional<RoomProviderMap> getRoomProviderMapById(@NotNull int id);
	
	RoomProviderMap saveRoomProviderMap(@NotNull RoomProviderMap roomProviderMap);
	
	List<RoomProviderMap> getAllRoomProviderMaps();
	
	List<RoomProviderMap> getRoomProviderMaps(RoomProviderMapSearchCriteria searchCriteria);
	
	void voidRoomProviderMap(@NotNull RoomProviderMap roomProviderMap, String voidReason);
	
	void purgeRoomProviderMap(@NotNull RoomProviderMap roomProviderMap) throws APIException;
	
}
