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

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.DateProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import lombok.Setter;
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
@Setter
public class QueueEntryResource extends DelegatingCrudResource<QueueEntry> {
	
	private QueueServicesWrapper services;
	
	private QueueEntrySearchCriteriaParser searchCriteriaParser;
	
	public QueueEntryResource() {
	}
	
	public QueueEntryResource(QueueServicesWrapper services, QueueEntrySearchCriteriaParser searchCriteriaParser) {
		this.services = services;
		this.searchCriteriaParser = searchCriteriaParser;
	}
	
	@Override
	public QueueEntry getByUniqueId(@NotNull String uuid) {
		Optional<QueueEntry> queueEntryOptional = getServices().getQueueEntryService().getQueueEntryByUuid(uuid);
		if (!queueEntryOptional.isPresent()) {
			throw new ObjectNotFoundException("Could not find visit queue entry with uuid " + uuid);
		}
		return queueEntryOptional.get();
	}
	
	@Override
	protected void delete(QueueEntry qe, String reason, RequestContext requestContext) throws ResponseException {
		getServices().getQueueEntryService().voidQueueEntry(qe, reason);
	}
	
	@Override
	public QueueEntry newDelegate() {
		return new QueueEntry();
	}
	
	@Override
	public QueueEntry save(QueueEntry queueEntry) {
		return getServices().getQueueEntryService().saveQueueEntry(queueEntry);
	}
	
	@Override
	public void purge(QueueEntry queueEntry, RequestContext requestContext) throws ResponseException {
		getServices().getQueueEntryService().purgeQueueEntry(queueEntry);
	}
	
	@Override
	protected PageableResult doGetAll(RequestContext requestContext) throws ResponseException {
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		List<QueueEntry> activeEntries = getServices().getQueueEntryService().getQueueEntries(criteria);
		return new NeedsPaging<>(new ArrayList<>(activeEntries), requestContext);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected PageableResult doSearch(RequestContext requestContext) {
		Map<String, String[]> parameters = requestContext.getRequest().getParameterMap();
		QueueEntrySearchCriteria criteria = getSearchCriteriaParser().constructFromRequest(parameters);
		List<QueueEntry> queueEntries = getServices().getQueueEntryService().getQueueEntries(criteria);
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
	public Model getGETModel(Representation rep) {
		ModelImpl model = (ModelImpl) super.getGETModel(rep);
		if (rep instanceof RefRepresentation || rep instanceof DefaultRepresentation) {
			model.property("uuid", new StringProperty()).property("queue", new RefProperty("#/definitions/QueueGetRef"))
			        .property("display", new StringProperty())
			        .property("status", new RefProperty("#/definitions/ConceptGetRef"))
			        .property("priority", new RefProperty("#/definitions/ConceptGetRef"))
			        .property("priorityComment", new StringProperty())
			        .property("patient", new RefProperty("#/definitions/PatientGetRef"))
			        .property("visit", new RefProperty("#/definitions/VisitGetRef"))
			        .property("sortWeight", new DoubleProperty()).property("startedAt", new DateProperty())
			        .property("endedAt", new DateProperty())
			        .property("locationWaitingFor", new RefProperty("#/definitions/LocationGetRef"))
			        .property("queueComingFrom", new RefProperty("#/definitions/QueueGetRef"))
			        .property("providerWaitingFor", new RefProperty("#/definitions/ProviderGetRef"));
		} else if (rep instanceof FullRepresentation) {
			model.property("uuid", new StringProperty()).property("queue", new RefProperty("#/definitions/QueueGetRef"))
			        .property("display", new StringProperty())
			        .property("status", new RefProperty("#/definitions/ConceptGetRef"))
			        .property("priority", new RefProperty("#/definitions/ConceptGetRef"))
			        .property("priorityComment", new StringProperty())
			        .property("patient", new RefProperty("#/definitions/PatientGetRef"))
			        .property("visit", new RefProperty("#/definitions/VisitGetRef"))
			        .property("sortWeight", new DoubleProperty()).property("startedAt", new DateProperty())
			        .property("endedAt", new DateProperty())
			        .property("locationWaitingFor", new RefProperty("#/definitions/LocationGetRef"))
			        .property("queueComingFrom", new RefProperty("#/definitions/QueueGetRef"))
			        .property("providerWaitingFor", new RefProperty("#/definitions/ProviderGetRef"))
			        .property("voided", new BooleanProperty()).property("voidedReason", new StringProperty())
			        .property("auditInfo", new StringProperty())
			        .property("previousQueueEntry", new RefProperty("#/definitions/QueueGetRef"));
		}
		return model;
	}
	
	@Override
	public Model getCREATEModel(Representation rep) {
		return new ModelImpl().property("queue", new RefProperty("#/definitions/QueueCreate"))
		        .property("status", new RefProperty("#/definitions/ConceptCreate"))
		        .property("priority", new RefProperty("#/definitions/ConceptCreate"))
		        .property("priorityComment", new StringProperty())
		        .property("patient", new RefProperty("#/definitions/PatientCreate"))
		        .property("visit", new RefProperty("#/definitions/VisitCreate")).property("sortWeight", new DoubleProperty())
		        .property("startedAt", new DateProperty())
		        .property("locationWaitingFor", new RefProperty("#/definitions/LocationCreate"))
		        .property("queueComingFrom", new RefProperty("#/definitions/QueueCreate"))
		        .property("providerWaitingFor", new RefProperty("#/definitions/ProviderCreate"));
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("status");
		description.addProperty("priority");
		description.addProperty("priorityComment");
		description.addProperty("sortWeight");
		description.addProperty("startedAt");
		description.addProperty("endedAt");
		description.addProperty("locationWaitingFor");
		description.addProperty("providerWaitingFor");
		return description;
	}
	
	@Override
	public Model getUPDATEModel(Representation rep) {
		return new ModelImpl().property("status", new RefProperty("#/definitions/ConceptCreate"))
		        .property("priority", new RefProperty("#/definitions/ConceptCreate"))
		        .property("priorityComment", new StringProperty()).property("sortWeight", new DoubleProperty())
		        .property("startedAt", new DateProperty()).property("endedAt", new DateProperty())
		        .property("locationWaitingFor", new RefProperty("#/definitions/LocationCreate"))
		        .property("providerWaitingFor", new RefProperty("#/definitions/ProviderCreate"));
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
			
			// gets the previous queue entry, but with REF representation so it doesn't recursively
			// fetch more previous entries.
			description.addProperty("previousQueueEntry", Representation.REF);
			
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
			description.addProperty("previousQueueEntry", Representation.FULL);
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
	
	@PropertyGetter("previousQueueEntry")
	public QueueEntry getPreviousQueueEntry(QueueEntry queueEntry) {
		return getServices().getQueueEntryService().getPreviousQueueEntry(queueEntry);
	}
	
	@Override
	public String getResourceVersion() {
		return "2.3";
	}
	
	public QueueServicesWrapper getServices() {
		if (services == null) {
			services = Context.getRegisteredComponents(QueueServicesWrapper.class).get(0);
		}
		return services;
	}
	
	public QueueEntrySearchCriteriaParser getSearchCriteriaParser() {
		if (searchCriteriaParser == null) {
			searchCriteriaParser = Context.getRegisteredComponents(QueueEntrySearchCriteriaParser.class).get(0);
		}
		return searchCriteriaParser;
	}
}
