/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.web.resources;

import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.utils.QueueEntrySearchCriteria;
import org.openmrs.module.queue.utils.QueueUtils;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

/**
 * REST resource for Queue Entries
 */
@Slf4j
@SuppressWarnings("unused")
@Resource(name = RestConstants.VERSION_1 + "/queue-entry", supportedClass = QueueEntry.class, supportedOpenmrsVersions = {
        "2.3 - 9.*" })
public class QueueEntryResource extends DelegatingCrudResource<QueueEntry> {
	
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
	
	public static final String SEARCH_PARAM_IS_ENDED = "isEnded";
	
	public static final String SEARCH_PARAM_ENDED_ON_OR_AFTER = "endedOnOrAfter";
	
	public static final String SEARCH_PARAM_ENDED_ON_OR_BEFORE = "endedOnOrBefore";
	
	public static final String SEARCH_PARAM_INCLUDE_VOIDED = "includedVoided";
	
	private final QueueServicesWrapper services;
	
	public QueueEntryResource() {
		this.services = Context.getRegisteredComponents(QueueServicesWrapper.class).get(0);
	}
	
	@Override
	public QueueEntry getByUniqueId(@NotNull String uuid) {
		Optional<QueueEntry> queueEntryOptional = services.getQueueEntryService().getQueueEntryByUuid(uuid);
		if (!queueEntryOptional.isPresent()) {
			throw new ObjectNotFoundException("Could not find visit queue entry with uuid " + uuid);
		}
		return queueEntryOptional.get();
	}
	
	@Override
	protected void delete(QueueEntry qe, String reason, RequestContext requestContext) throws ResponseException {
		services.getQueueEntryService().voidQueueEntry(qe.getUuid(), reason);
	}
	
	@Override
	public QueueEntry newDelegate() {
		return new QueueEntry();
	}
	
	@Override
	public QueueEntry save(QueueEntry queueEntry) {
		return services.getQueueEntryService().createQueueEntry(queueEntry);
	}
	
	@Override
	public void purge(QueueEntry queueEntry, RequestContext requestContext) throws ResponseException {
		services.getQueueEntryService().purgeQueueEntry(queueEntry);
	}
	
	@Override
	protected PageableResult doGetAll(RequestContext requestContext) throws ResponseException {
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		criteria.setIsEnded(false);
		List<QueueEntry> activeEntries = services.getQueueEntryService().getQueueEntries(criteria);
		return new NeedsPaging<>(new ArrayList<>(activeEntries), requestContext);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected PageableResult doSearch(RequestContext requestContext) {
		boolean criteriaFound = false;
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		Map<String, String[]> parameterMap = requestContext.getRequest().getParameterMap();
		for (String parameterName : parameterMap.keySet()) {
			switch (parameterName) {
				case SEARCH_PARAM_QUEUE: {
					criteriaFound = true;
					criteria.setQueues(services.getQueues(parameterMap.get(SEARCH_PARAM_QUEUE)));
					break;
				}
				case SEARCH_PARAM_LOCATION: {
					criteriaFound = true;
					criteria.setLocations(services.getLocations(parameterMap.get(SEARCH_PARAM_LOCATION)));
					break;
				}
				case SEARCH_PARAM_SERVICE: {
					criteriaFound = true;
					criteria.setServices(services.getConcepts(parameterMap.get(SEARCH_PARAM_SERVICE)));
					break;
				}
				case SEARCH_PARAM_PATIENT: {
					criteriaFound = true;
					criteria.setPatient(services.getPatient(parameterMap.get(SEARCH_PARAM_PATIENT)[0]));
					break;
				}
				case SEARCH_PARAM_VISIT: {
					criteriaFound = true;
					criteria.setVisit(services.getVisit(parameterMap.get(SEARCH_PARAM_VISIT)[0]));
					break;
				}
				case SEARCH_PARAM_HAS_VISIT: {
					criteriaFound = true;
					criteria.setHasVisit(Boolean.parseBoolean(parameterMap.get(SEARCH_PARAM_HAS_VISIT)[0]));
					break;
				}
				case SEARCH_PARAM_PRIORITY: {
					criteriaFound = true;
					criteria.setPriorities(services.getConcepts(parameterMap.get(SEARCH_PARAM_PRIORITY)));
					break;
				}
				case SEARCH_PARAM_STATUS: {
					criteriaFound = true;
					criteria.setStatuses(services.getConcepts(parameterMap.get(SEARCH_PARAM_STATUS)));
					break;
				}
				case SEARCH_PARAM_LOCATION_WAITING_FOR: {
					criteriaFound = true;
					List<Location> l = services.getLocations(parameterMap.get(SEARCH_PARAM_LOCATION_WAITING_FOR));
					criteria.setLocationsWaitingFor(l);
					break;
				}
				case SEARCH_PARAM_PROVIDER_WAITING_FOR: {
					criteriaFound = true;
					List<Provider> l = services.getProviders(parameterMap.get(SEARCH_PARAM_PROVIDER_WAITING_FOR));
					criteria.setProvidersWaitingFor(l);
					break;
				}
				case SEARCH_PARAM_QUEUE_COMING_FROM: {
					criteriaFound = true;
					criteria.setQueuesComingFrom(services.getQueues(parameterMap.get(SEARCH_PARAM_QUEUE_COMING_FROM)));
					break;
				}
				case SEARCH_PARAM_STARTED_ON_OR_AFTER: {
					criteriaFound = true;
					String date = parameterMap.get(SEARCH_PARAM_STARTED_ON_OR_AFTER)[0];
					criteria.setStartedOnOrAfter(QueueUtils.parseDate(date));
					break;
				}
				case SEARCH_PARAM_STARTED_ON_OR_BEFORE: {
					criteriaFound = true;
					String date = parameterMap.get(SEARCH_PARAM_STARTED_ON_OR_BEFORE)[0];
					criteria.setStartedOnOrBefore(QueueUtils.parseDate(date));
					break;
				}
				case SEARCH_PARAM_ENDED_ON_OR_AFTER: {
					criteriaFound = true;
					String date = parameterMap.get(SEARCH_PARAM_ENDED_ON_OR_AFTER)[0];
					criteria.setEndedOnOrAfter(QueueUtils.parseDate(date));
					break;
				}
				case SEARCH_PARAM_ENDED_ON_OR_BEFORE: {
					criteriaFound = true;
					String date = parameterMap.get(SEARCH_PARAM_ENDED_ON_OR_BEFORE)[0];
					criteria.setEndedOnOrBefore(QueueUtils.parseDate(date));
					break;
				}
				case SEARCH_PARAM_IS_ENDED: {
					criteriaFound = true;
					criteria.setIsEnded(Boolean.parseBoolean(parameterMap.get(SEARCH_PARAM_IS_ENDED)[0]));
					break;
				}
				case SEARCH_PARAM_INCLUDE_VOIDED: {
					criteriaFound = true;
					criteria.setIncludedVoided(Boolean.parseBoolean(parameterMap.get(SEARCH_PARAM_INCLUDE_VOIDED)[0]));
					break;
				}
				default: {
					log.debug("Unhandled search parameter found: " + parameterName);
				}
			}
		}
		if (!criteriaFound) {
			return new EmptySearchResult();
		}
		Collection<QueueEntry> queueEntries = services.getQueueEntryService().getQueueEntries(criteria);
		return new NeedsPaging<>(new ArrayList<>(queueEntries), requestContext);
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("queue");
		description.addProperty("status");
		description.addProperty("priority");
		description.addProperty("priorityComment");
		description.addProperty("patient");
		description.addProperty("visit");
		description.addProperty("sortWeight");
		description.addProperty("startedAt");
		description.addProperty("locationWaitingFor");
		description.addProperty("queueComingFrom");
		description.addProperty("providerWaitingFor");
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("status");
		description.addProperty("priorityComment");
		description.addProperty("sortWeight");
		description.addProperty("endedAt");
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation representation) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		if (representation instanceof RefRepresentation) {
			addSharedResourceDescriptionProperties(description);
			description.addProperty("uuid");
			description.addProperty("display");
			description.addProperty("queue", Representation.REF);
			description.addProperty("status", Representation.REF);
			description.addProperty("patient", Representation.REF);
			description.addProperty("visit", Representation.REF);
			description.addProperty("priority", Representation.REF);
			description.addProperty("priorityComment");
			description.addProperty("sortWeight");
			description.addProperty("startedAt");
			description.addProperty("endedAt");
			description.addProperty("locationWaitingFor", Representation.REF);
			description.addProperty("queueComingFrom", Representation.REF);
			description.addProperty("providerWaitingFor", Representation.REF);
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		} else if (representation instanceof DefaultRepresentation) {
			addSharedResourceDescriptionProperties(description);
			description.addProperty("uuid");
			description.addProperty("display");
			description.addProperty("queue", Representation.DEFAULT);
			description.addProperty("status", Representation.DEFAULT);
			description.addProperty("patient", Representation.DEFAULT);
			description.addProperty("visit", Representation.DEFAULT);
			description.addProperty("priority", Representation.DEFAULT);
			description.addProperty("priorityComment");
			description.addProperty("sortWeight");
			description.addProperty("startedAt");
			description.addProperty("endedAt");
			description.addProperty("locationWaitingFor", Representation.DEFAULT);
			description.addProperty("queueComingFrom", Representation.DEFAULT);
			description.addProperty("providerWaitingFor", Representation.DEFAULT);
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		} else if (representation instanceof FullRepresentation) {
			addSharedResourceDescriptionProperties(description);
			description.addProperty("uuid");
			description.addProperty("display");
			description.addProperty("queue", Representation.FULL);
			description.addProperty("status", Representation.FULL);
			description.addProperty("patient", Representation.FULL);
			description.addProperty("visit", Representation.FULL);
			description.addProperty("priority", Representation.FULL);
			description.addProperty("priorityComment");
			description.addProperty("sortWeight");
			description.addProperty("startedAt");
			description.addProperty("endedAt");
			description.addProperty("locationWaitingFor", Representation.FULL);
			description.addProperty("queueComingFrom", Representation.FULL);
			description.addProperty("providerWaitingFor", Representation.FULL);
			description.addProperty("voided");
			description.addProperty("voidReason");
			description.addProperty("auditInfo");
		} else if (representation instanceof CustomRepresentation) {
			description = null;
		}
		return description;
	}
	
	private void addSharedResourceDescriptionProperties(DelegatingResourceDescription resourceDescription) {
		resourceDescription.addSelfLink();
		resourceDescription.addProperty("uuid");
	}
	
	@Override
	public String getResourceVersion() {
		return "2.3";
	}
}
