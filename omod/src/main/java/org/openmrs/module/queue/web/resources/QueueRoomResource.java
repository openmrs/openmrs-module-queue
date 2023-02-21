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

import java.util.Optional;

import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueRoomService;
import org.openmrs.module.queue.api.QueueService;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueRoom;
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

@Resource(name = RestConstants.VERSION_1 + "/queueroom", supportedClass = QueueRoom.class, supportedOpenmrsVersions = {
        "2.0 - 2.*" })
public class QueueRoomResource extends DelegatingCrudResource<QueueRoom> {
	
	private final QueueRoomService queueRoomService;
	
	public QueueRoomResource() {
		this.queueRoomService = Context.getService(QueueRoomService.class);
	}
	
	@Override
	public QueueRoom getByUniqueId(String uuid) {
		Optional<QueueRoom> optionalQueueRoom = queueRoomService.getQueueRoomByUuid(uuid);
		if (!optionalQueueRoom.isPresent()) {
			throw new ObjectNotFoundException("Could not find queueRoom with UUID " + uuid);
		}
		return optionalQueueRoom.get();
	}
	
	@Override
	protected void delete(QueueRoom queueRoom, String s, RequestContext requestContext) throws ResponseException {
	}
	
	@Override
	public QueueRoom newDelegate() {
		return new QueueRoom();
	}
	
	@Override
	public QueueRoom save(QueueRoom queueRoom) {
		return queueRoomService.createQueueRoom(queueRoom);
	}
	
	@Override
	public void purge(QueueRoom queueRoom, RequestContext requestContext) throws ResponseException {
		
	}
	
	@Override
	protected PageableResult doSearch(RequestContext context) {
		Queue queue = context.getParameter("queue") != null
		        ? Context.getService(QueueService.class).getQueueByUuid(context.getParameter("queue")).get()
		        : null;
		Location location = context.getParameter("location") != null
		        ? Context.getLocationService().getLocationByUuid(context.getParameter("location"))
		        : null;
		return new NeedsPaging<>(queueRoomService.getQueueRoomsByServiceAndLocation(queue, location), context);
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
	
	@PropertyGetter("display")
	public String getDisplay(QueueRoom queueRoom) {
		return queueRoom.getName();
	}
	
	@Override
	public String getResourceVersion() {
		return "2.3";
	}
}
