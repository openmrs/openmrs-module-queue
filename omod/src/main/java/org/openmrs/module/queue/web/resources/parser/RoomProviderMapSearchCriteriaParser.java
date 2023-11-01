/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.web.resources.parser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.search.RoomProviderMapSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Utility class for static methods useful within the Queue module
 */
@Slf4j
@Component
public class RoomProviderMapSearchCriteriaParser {
	
	public static final String SEARCH_PARAM_QUEUE_ROOMS = "queueRoom";
	
	public static final String SEARCH_PARAM_PROVIDER = "provider";
	
	public static final List<String> SEARCH_PARAMETERS = Arrays.asList(SEARCH_PARAM_QUEUE_ROOMS, SEARCH_PARAM_PROVIDER);
	
	private final QueueServicesWrapper services;
	
	@Autowired
	public RoomProviderMapSearchCriteriaParser(QueueServicesWrapper services) {
		this.services = services;
	}
	
	/**
	 * @param parameterMap a Map from parameter name to array of parameter values
	 * @return true if the parameterMap contains at least one search parameter as a key
	 */
	public boolean hasSearchParameter(Map<String, String[]> parameterMap) {
		for (String parameterName : parameterMap.keySet()) {
			if (SEARCH_PARAMETERS.contains(parameterName)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param parameterMap a Map from parameter name to array of parameter values
	 * @return QueueEntrySearchCriteria that is configured based on the parameters in the request
	 */
	public RoomProviderMapSearchCriteria constructFromRequest(Map<String, String[]> parameterMap) {
		RoomProviderMapSearchCriteria criteria = new RoomProviderMapSearchCriteria();
		if (parameterMap == null) {
			return criteria;
		}
		for (String parameterName : parameterMap.keySet()) {
			switch (parameterName) {
				case SEARCH_PARAM_QUEUE_ROOMS: {
					criteria.setQueueRooms(services.getQueueRooms(parameterMap.get(SEARCH_PARAM_QUEUE_ROOMS)));
					break;
				}
				case SEARCH_PARAM_PROVIDER: {
					criteria.setProviders(services.getProviders(parameterMap.get(SEARCH_PARAM_PROVIDER)));
					break;
				}
				default: {
					log.debug("Unhandled search parameter found: " + parameterName);
				}
			}
		}
		return criteria;
	}
}
