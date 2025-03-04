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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import lombok.Setter;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.search.QueueRoomSearchCriteria;
import org.openmrs.module.queue.model.QueueRoom;
import org.openmrs.module.queue.web.resources.parser.QueueRoomSearchCriteriaParser;
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

@SuppressWarnings("unused")
@Resource(name = RestConstants.VERSION_1 + "/queue-room", supportedClass = QueueRoom.class, supportedOpenmrsVersions = {
        "2.3 - 9.*" })
@Setter
public class QueueRoomResource extends DelegatingCrudResource<QueueRoom> {
	
	private QueueServicesWrapper services;
	
	private QueueRoomSearchCriteriaParser searchCriteriaParser;
	
	public QueueRoomResource() {
	}
	
	public QueueRoomResource(QueueServicesWrapper services, QueueRoomSearchCriteriaParser searchCriteriaParser) {
		this.services = services;
		this.searchCriteriaParser = searchCriteriaParser;
	}
	
	@Override
	public NeedsPaging<QueueRoom> doGetAll(RequestContext requestContext) throws ResponseException {
		return new NeedsPaging<>(new ArrayList<>(getServices().getQueueRoomService().getAllQueueRooms()), requestContext);
	}
	
	@Override
	public QueueRoom getByUniqueId(String uuid) {
		Optional<QueueRoom> optionalQueueRoom = getServices().getQueueRoomService().getQueueRoomByUuid(uuid);
		if (!optionalQueueRoom.isPresent()) {
			throw new ObjectNotFoundException("Could not find queueRoom with UUID " + uuid);
		}
		return optionalQueueRoom.get();
	}
	
	@Override
	protected void delete(QueueRoom queueRoom, String retireReason, RequestContext requestContext) throws ResponseException {
		getServices().getQueueRoomService().retireQueueRoom(queueRoom, retireReason);
	}
	
	@Override
	public QueueRoom newDelegate() {
		return new QueueRoom();
	}
	
	@Override
	public QueueRoom save(QueueRoom queueRoom) {
		return getServices().getQueueRoomService().saveQueueRoom(queueRoom);
	}
	
	@Override
	public void purge(QueueRoom queueRoom, RequestContext requestContext) throws ResponseException {
		getServices().getQueueRoomService().purgeQueueRoom(queueRoom);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected PageableResult doSearch(RequestContext requestContext) {
		Map<String, String[]> parameters = requestContext.getRequest().getParameterMap();
		QueueRoomSearchCriteria criteria = getSearchCriteriaParser().constructFromRequest(parameters);
		List<QueueRoom> queueRooms = getServices().getQueueRoomService().getQueueRooms(criteria);
		return new NeedsPaging<>(queueRooms, requestContext);
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation representation) {
		DelegatingResourceDescription resourceDescription = new DelegatingResourceDescription();
		if (representation instanceof RefRepresentation) {
			this.addSharedResourceDescriptionProperties(resourceDescription);
			resourceDescription.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		} else if (representation instanceof DefaultRepresentation) {
			this.addSharedResourceDescriptionProperties(resourceDescription);
			resourceDescription.addProperty("queue", Representation.REF);
			resourceDescription.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		} else if (representation instanceof FullRepresentation) {
			this.addSharedResourceDescriptionProperties(resourceDescription);
			resourceDescription.addProperty("queue", Representation.FULL);
			resourceDescription.addProperty("auditInfo");
		} else if (representation instanceof CustomRepresentation) {
			//For custom representation, must be null
			// - let the user decide which properties should be included in the response
			resourceDescription = null;
		}
		return resourceDescription;
	}
	
	private void addSharedResourceDescriptionProperties(DelegatingResourceDescription resourceDescription) {
		resourceDescription.addSelfLink();
		resourceDescription.addProperty("uuid");
		resourceDescription.addProperty("display");
		resourceDescription.addProperty("name");
		resourceDescription.addProperty("description");
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription resourceDescription = new DelegatingResourceDescription();
		resourceDescription.addProperty("name");
		resourceDescription.addProperty("description");
		resourceDescription.addProperty("queue");
		return resourceDescription;
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
		return this.getCreatableProperties();
	}
	
	@Override
	public Model getGETModel(Representation rep) {
		ModelImpl model = (ModelImpl) super.getGETModel(rep);
		if (rep instanceof DefaultRepresentation) {
			model.property("uuid", new StringProperty()).property("display", new StringProperty())
			        .property("name", new StringProperty()).property("description", new StringProperty())
			        .property("queue", new RefProperty("#/definitions/QueueGet"));
		}
		if (rep instanceof FullRepresentation) {
			model.property("queue", new RefProperty("#/definitions/QueueGetFull"))
			        .property("auditInfo", new StringProperty()).property("queue", new StringProperty());
		}
		
		if (rep instanceof RefRepresentation) {
			model.property("uuid", new StringProperty()).property("display", new StringProperty())
			        .property("name", new StringProperty()).property("description", new StringProperty())
			        .property("queue", new RefProperty("#/definitions/QueueGetRef"));
		}
		return model;
	}
	
	@Override
	public Model getCREATEModel(Representation rep) {
		return new ModelImpl().property("name", new StringProperty()).property("description", new StringProperty())
		        .property("queue", new RefProperty("#/definitions/QueueCreate"));
	}
	
	@Override
	public Model getUPDATEModel(Representation rep) {
		return getCREATEModel(rep);
	}
	
	@PropertyGetter("display")
	public String getDisplay(QueueRoom queueRoom) {
		return queueRoom.getName();
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
	
	public QueueRoomSearchCriteriaParser getSearchCriteriaParser() {
		if (searchCriteriaParser == null) {
			searchCriteriaParser = Context.getRegisteredComponents(QueueRoomSearchCriteriaParser.class).get(0);
		}
		return searchCriteriaParser;
	}
}
