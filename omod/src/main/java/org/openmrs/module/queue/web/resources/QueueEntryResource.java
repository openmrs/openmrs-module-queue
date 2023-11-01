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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.web.resources.parser.QueueEntrySearchCriteriaParser;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
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
	
	private final QueueServicesWrapper services;
	
	private final QueueEntrySearchCriteriaParser searchCriteriaParser;
	
	public QueueEntryResource() {
		services = Context.getRegisteredComponents(QueueServicesWrapper.class).get(0);
		searchCriteriaParser = Context.getRegisteredComponents(QueueEntrySearchCriteriaParser.class).get(0);
	}
	
	public QueueEntryResource(QueueServicesWrapper services, QueueEntrySearchCriteriaParser searchCriteriaParser) {
		this.services = services;
		this.searchCriteriaParser = searchCriteriaParser;
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
		services.getQueueEntryService().voidQueueEntry(qe, reason);
	}
	
	@Override
	public QueueEntry newDelegate() {
		return new QueueEntry();
	}
	
	@Override
	public QueueEntry save(QueueEntry queueEntry) {
		return services.getQueueEntryService().saveQueueEntry(queueEntry);
	}
	
	@Override
	public void purge(QueueEntry queueEntry, RequestContext requestContext) throws ResponseException {
		services.getQueueEntryService().purgeQueueEntry(queueEntry);
	}
	
	@Override
	protected PageableResult doGetAll(RequestContext requestContext) throws ResponseException {
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		List<QueueEntry> activeEntries = services.getQueueEntryService().getQueueEntries(criteria);
		return new NeedsPaging<>(new ArrayList<>(activeEntries), requestContext);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected PageableResult doSearch(RequestContext requestContext) {
		Map<String, String[]> parameters = requestContext.getRequest().getParameterMap();
		QueueEntrySearchCriteria criteria = searchCriteriaParser.constructFromRequest(parameters);
		List<QueueEntry> queueEntries = services.getQueueEntryService().getQueueEntries(criteria);
		return new NeedsPaging<>(queueEntries, requestContext);
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
	
	@PropertyGetter("display")
	public String getDisplay(QueueEntry queueEntry) {
		PersonName personName = queueEntry.getPatient().getPersonName();
		return (personName == null ? queueEntry.getPatient().toString() : personName.getFullName());
	}
	
	@Override
	public String getResourceVersion() {
		return "2.3";
	}
}
