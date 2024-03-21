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
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.utils.QueueUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Utility class for static methods useful within the Queue module
 */
@Slf4j
@Component
public class QueueEntrySearchCriteriaParser {
	
	public static final String SEARCH_PARAM_QUEUE = "queue";
	
	public static final String SEARCH_PARAM_LOCATION = "location";
	
	public static final String SEARCH_PARAM_SERVICE = "service";
	
	public static final String SEARCH_PARAM_PATIENT = "patient";
	
	public static final String SEARCH_PARAM_VISIT = "visit";
	
	public static final String SEARCH_PARAM_HAS_VISIT = "hasVisit";
	
	public static final String SEARCH_PARAM_PRIORITY = "priority";
	
	public static final String SEARCH_PARAM_STATUS = "status";
	
	public static final String SEARCH_PARAM_LOCATION_WAITING_FOR = "locationWaitingFor";
	
	public static final String SEARCH_PARAM_PROVIDER_WAITING_FOR = "providerWaitingFor";
	
	public static final String SEARCH_PARAM_QUEUE_COMING_FROM = "queueComingFrom";
	
	public static final String SEARCH_PARAM_STARTED_ON_OR_AFTER = "startedOnOrAfter";
	
	public static final String SEARCH_PARAM_STARTED_ON_OR_BEFORE = "startedOnOrBefore";
	
	public static final String SEARCH_PARAM_STARTED_ON = "startedOn";
	
	public static final String SEARCH_PARAM_IS_ENDED = "isEnded";
	
	public static final String SEARCH_PARAM_ENDED_ON_OR_AFTER = "endedOnOrAfter";
	
	public static final String SEARCH_PARAM_ENDED_ON_OR_BEFORE = "endedOnOrBefore";
	
	public static final String SEARCH_PARAM_ENDED_ON = "endedOn";
	
	public static final String SEARCH_PARAM_INCLUDE_VOIDED = "includedVoided";
	
	public static final List<String> SEARCH_PARAMETERS = Arrays.asList(SEARCH_PARAM_QUEUE, SEARCH_PARAM_LOCATION,
	    SEARCH_PARAM_SERVICE, SEARCH_PARAM_PATIENT, SEARCH_PARAM_VISIT, SEARCH_PARAM_HAS_VISIT, SEARCH_PARAM_PRIORITY,
	    SEARCH_PARAM_STATUS, SEARCH_PARAM_LOCATION_WAITING_FOR, SEARCH_PARAM_PROVIDER_WAITING_FOR,
	    SEARCH_PARAM_QUEUE_COMING_FROM, SEARCH_PARAM_STARTED_ON_OR_AFTER, SEARCH_PARAM_STARTED_ON_OR_BEFORE,
	    SEARCH_PARAM_IS_ENDED, SEARCH_PARAM_ENDED_ON_OR_AFTER, SEARCH_PARAM_ENDED_ON_OR_BEFORE, SEARCH_PARAM_INCLUDE_VOIDED);
	
	private final QueueServicesWrapper services;
	
	@Autowired
	public QueueEntrySearchCriteriaParser(QueueServicesWrapper services) {
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
	public QueueEntrySearchCriteria constructFromRequest(Map<String, String[]> parameterMap) {
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		if (parameterMap == null) {
			return criteria;
		}
		for (String parameterName : parameterMap.keySet()) {
			switch (parameterName) {
				case SEARCH_PARAM_QUEUE: {
					criteria.setQueues(services.getQueues(parameterMap.get(SEARCH_PARAM_QUEUE)));
					break;
				}
				case SEARCH_PARAM_LOCATION: {
					criteria.setLocations(services.getLocations(parameterMap.get(SEARCH_PARAM_LOCATION)));
					break;
				}
				case SEARCH_PARAM_SERVICE: {
					criteria.setServices(services.getConcepts(parameterMap.get(SEARCH_PARAM_SERVICE)));
					break;
				}
				case SEARCH_PARAM_PATIENT: {
					criteria.setPatient(services.getPatient(parameterMap.get(SEARCH_PARAM_PATIENT)[0]));
					break;
				}
				case SEARCH_PARAM_VISIT: {
					criteria.setVisit(services.getVisit(parameterMap.get(SEARCH_PARAM_VISIT)[0]));
					break;
				}
				case SEARCH_PARAM_HAS_VISIT: {
					criteria.setHasVisit(parseBoolean(parameterMap.get(SEARCH_PARAM_HAS_VISIT)[0]));
					break;
				}
				case SEARCH_PARAM_PRIORITY: {
					criteria.setPriorities(services.getConcepts(parameterMap.get(SEARCH_PARAM_PRIORITY)));
					break;
				}
				case SEARCH_PARAM_STATUS: {
					criteria.setStatuses(services.getConcepts(parameterMap.get(SEARCH_PARAM_STATUS)));
					break;
				}
				case SEARCH_PARAM_LOCATION_WAITING_FOR: {
					List<Location> l = services.getLocations(parameterMap.get(SEARCH_PARAM_LOCATION_WAITING_FOR));
					criteria.setLocationsWaitingFor(l);
					break;
				}
				case SEARCH_PARAM_PROVIDER_WAITING_FOR: {
					List<Provider> l = services.getProviders(parameterMap.get(SEARCH_PARAM_PROVIDER_WAITING_FOR));
					criteria.setProvidersWaitingFor(l);
					break;
				}
				case SEARCH_PARAM_QUEUE_COMING_FROM: {
					criteria.setQueuesComingFrom(services.getQueues(parameterMap.get(SEARCH_PARAM_QUEUE_COMING_FROM)));
					break;
				}
				case SEARCH_PARAM_STARTED_ON_OR_AFTER: {
					String date = parameterMap.get(SEARCH_PARAM_STARTED_ON_OR_AFTER)[0];
					criteria.setStartedOnOrAfter(QueueUtils.parseDate(date));
					break;
				}
				case SEARCH_PARAM_STARTED_ON_OR_BEFORE: {
					String date = parameterMap.get(SEARCH_PARAM_STARTED_ON_OR_BEFORE)[0];
					criteria.setStartedOnOrBefore(QueueUtils.parseDate(date));
					break;
				}
				case SEARCH_PARAM_STARTED_ON: {
					String date = parameterMap.get(SEARCH_PARAM_STARTED_ON)[0];
					criteria.setStartedOn(QueueUtils.parseDate(date));
					break;
				}
				case SEARCH_PARAM_ENDED_ON_OR_AFTER: {
					String date = parameterMap.get(SEARCH_PARAM_ENDED_ON_OR_AFTER)[0];
					criteria.setEndedOnOrAfter(QueueUtils.parseDate(date));
					break;
				}
				case SEARCH_PARAM_ENDED_ON_OR_BEFORE: {
					String date = parameterMap.get(SEARCH_PARAM_ENDED_ON_OR_BEFORE)[0];
					criteria.setEndedOnOrBefore(QueueUtils.parseDate(date));
					break;
				}
				case SEARCH_PARAM_ENDED_ON: {
					String date = parameterMap.get(SEARCH_PARAM_ENDED_ON)[0];
					criteria.setEndedOn(QueueUtils.parseDate(date));
					break;
				}
				case SEARCH_PARAM_IS_ENDED: {
					criteria.setIsEnded(parseBoolean(parameterMap.get(SEARCH_PARAM_IS_ENDED)[0]));
					break;
				}
				case SEARCH_PARAM_INCLUDE_VOIDED: {
					criteria.setIncludedVoided(Boolean.parseBoolean(parameterMap.get(SEARCH_PARAM_INCLUDE_VOIDED)[0]));
					break;
				}
				default: {
					log.debug("Unhandled search parameter found: " + parameterName);
				}
			}
		}
		return criteria;
	}
	
	private Boolean parseBoolean(String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		return Boolean.parseBoolean(value);
	}
}
