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
import java.util.Optional;

import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueService;
import org.openmrs.module.queue.model.Queue;
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
@Resource(name = RestConstants.VERSION_1 + "/queue", supportedClass = Queue.class, supportedOpenmrsVersions = {
        "2.0 - 2.*" })
public class QueueResource extends DelegatingCrudResource<Queue> {
	
	private final QueueService queueService;
	
	public QueueResource() {
		this.queueService = Context.getService(QueueService.class);
	}
	
	@Override
	public NeedsPaging<Queue> doGetAll(RequestContext requestContext) throws ResponseException {
		return new NeedsPaging<Queue>(new ArrayList<Queue>(Context.getService(QueueService.class).getAllQueues()),
		        requestContext);
	}
	
	@Override
	public Queue getByUniqueId(@NotNull String uuid) {
		Optional<Queue> optionalQueue = queueService.getQueueByUuid(uuid);
		if (!optionalQueue.isPresent()) {
			throw new ObjectNotFoundException("Could not find queue with UUID " + uuid);
		}
		return optionalQueue.get();
	}
	
	@Override
	protected void delete(Queue queue, String retireReason, RequestContext requestContext) throws ResponseException {
		if (!this.queueService.getQueueByUuid(queue.getUuid()).isPresent()) {
			throw new ObjectNotFoundException("Could not find queue with uuid " + queue.getUuid());
		}
		this.queueService.voidQueue(queue.getUuid(), retireReason);
	}
	
	@Override
	public Queue newDelegate() {
		return new Queue();
	}
	
	@Override
	public Queue save(Queue queue) {
		return this.queueService.createQueue(queue);
	}
	
	@Override
	public void purge(Queue queue, RequestContext requestContext) throws ResponseException {
		this.queueService.purgeQueue(queue);
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation representation) {
		DelegatingResourceDescription resourceDescription = new DelegatingResourceDescription();
		if (representation instanceof RefRepresentation) {
			this.addSharedResourceDescriptionProperties(resourceDescription);
			resourceDescription.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		} else if (representation instanceof DefaultRepresentation) {
			this.addSharedResourceDescriptionProperties(resourceDescription);
			resourceDescription.addProperty("location", Representation.REF);
			resourceDescription.addProperty("service", Representation.REF);
			resourceDescription.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		} else if (representation instanceof FullRepresentation) {
			this.addSharedResourceDescriptionProperties(resourceDescription);
			resourceDescription.addProperty("location", Representation.FULL);
			resourceDescription.addProperty("service", Representation.FULL);
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
		resourceDescription.addProperty("location");
		resourceDescription.addProperty("service");
		return resourceDescription;
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
		return this.getCreatableProperties();
	}
	
	@Override
	protected PageableResult doSearch(RequestContext requestContext) {
		String locationUuid = requestContext.getParameter("location");
		Location location = Context.getLocationService().getLocationByUuid(locationUuid);
		if (location == null) {
			throw new ObjectNotFoundException("could not find location with uuid " + locationUuid);
		}
		List<Queue> queuesByLocation = queueService.getAllQueuesByLocation(locationUuid);
		return new NeedsPaging<>(queuesByLocation, requestContext);
	}
	
	@PropertyGetter("display")
	public String getDisplay(Queue queue) {
		return queue.getName();
	}
	
	@Override
	public String getResourceVersion() {
		//What determines the resource version? is it the target platform version or just 1.8
		return "2.3";
	}
}
