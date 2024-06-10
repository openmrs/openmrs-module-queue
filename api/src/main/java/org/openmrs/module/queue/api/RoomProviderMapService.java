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
import org.openmrs.module.queue.api.search.RoomProviderMapSearchCriteria;
import org.openmrs.module.queue.model.RoomProviderMap;
import org.openmrs.module.queue.utils.PrivilegeConstants;

public interface RoomProviderMapService {
	
	/**
	 * Gets a room provider map by uuid.
	 *
	 * @param uuid the uuid of the room provider map to be returned.
	 * @return {@link org.openmrs.module.queue.model.RoomProviderMap}
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUE_ROOMS })
	Optional<RoomProviderMap> getRoomProviderMapByUuid(@NotNull String uuid);
	
	/**
	 * Gets a room provider map by id.
	 *
	 * @param id the id of the room provider map to retrieve.
	 * @return {@link org.openmrs.module.queue.model.RoomProviderMap}
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUE_ROOMS })
	Optional<RoomProviderMap> getRoomProviderMapById(@NotNull int id);
	
	/**
	 * Saves a room provider map
	 *
	 * @param roomProviderMap the room provider map to be saved
	 * @return saved {@link org.openmrs.module.queue.model.RoomProviderMap}
	 */
	@Authorized({ PrivilegeConstants.MANAGE_QUEUE_ROOMS })
	RoomProviderMap saveRoomProviderMap(@NotNull RoomProviderMap roomProviderMap);
	
	/**
	 * Gets a List of all Room Provider Maps.
	 *
	 * @return {@link List} of all room provider maps
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUE_ROOMS })
	List<RoomProviderMap> getAllRoomProviderMaps();
	
	/**
	 * Gets a List of all Room Provider Maps that match the given RoomProviderMapSearchCriteria.
	 *
	 * @return {@link List} of room provider maps that match the given
	 *         {@link org.openmrs.module.queue.api.search.RoomProviderMapSearchCriteria}
	 */
	@Authorized({ PrivilegeConstants.GET_QUEUE_ROOMS })
	List<RoomProviderMap> getRoomProviderMaps(RoomProviderMapSearchCriteria searchCriteria);
	
	/**
	 * Voids a room provider map
	 *
	 * @param roomProviderMap the room provider map to void
	 * @param voidReason the reason for voiding the room provider map
	 */
	@Authorized({ PrivilegeConstants.MANAGE_QUEUE_ROOMS })
	void voidRoomProviderMap(@NotNull RoomProviderMap roomProviderMap, String voidReason);
	
	/**
	 * Completely remove a room provider map from the database
	 *
	 * @param roomProviderMap room provider map to be deleted
	 * @throws org.openmrs.api.APIException
	 */
	@Authorized({ PrivilegeConstants.PURGE_QUEUE_ROOMS })
	void purgeRoomProviderMap(@NotNull RoomProviderMap roomProviderMap) throws APIException;
	
}
