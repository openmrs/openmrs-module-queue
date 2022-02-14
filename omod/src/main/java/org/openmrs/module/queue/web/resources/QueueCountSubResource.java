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

import java.util.Collections;

import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.web.resources.response.QueueCount;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.SubResource;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingSubResource;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

/**
 * Probably, this sub-resource is unnecessary, the idea is; with a searchHandler -
 * QueueCountSearchHandler, I can achieve the following URL formats; /ws/rest/v1/queue/<UUID>/count
 * - returns the count of queue entries for the specified resource
 * /ws/rest/v1/queue/<UUID>/count?status=Waiting for Service Queue Count
 * /ws/rest/v1/queue/<UUID>/count?status=With Service /ws/rest/v1/queue/<UUID>/count?status=Waiting
 * For Service&v=custom:(count) -returns only the count.
 */
@SuppressWarnings("unused")
@SubResource(parent = QueueResource.class, path = "count", supportedClass = QueueCount.class, supportedOpenmrsVersions = {
        "2.0 - 2.*" })
public class QueueCountSubResource extends DelegatingSubResource<QueueCount, Queue, QueueResource> {
	
	@Override
	public Queue getParent(QueueCount queueCount) {
		return queueCount.getQueue();
	}
	
	@Override
	public void setParent(QueueCount queueCount, Queue queue) {
		queueCount.setQueue(queue);
	}
	
	@Override
	public PageableResult doGetAll(Queue queue, RequestContext requestContext) throws ResponseException {
		return new NeedsPaging<>(Collections.singletonList(new QueueCount(queue, queue.getQueueEntries().size())),
		        requestContext);
	}
	
	@Override
	public QueueCount getByUniqueId(String s) {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	protected void delete(QueueCount queueCount, String s, RequestContext requestContext) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	public QueueCount newDelegate() {
		return new QueueCount();
	}
	
	@Override
	public QueueCount save(QueueCount queueCount) {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	public void purge(QueueCount queueCount, RequestContext requestContext) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation representation) {
		DelegatingResourceDescription resourceDescription = new DelegatingResourceDescription();
		if (representation instanceof CustomRepresentation) {
			resourceDescription = null;
		} else if (representation instanceof DefaultRepresentation) {
			resourceDescription.addProperty("count");
			resourceDescription.addProperty("display");
		} else if (representation instanceof RefRepresentation) {
			resourceDescription.addProperty("count");
			resourceDescription.addProperty("display");
			resourceDescription.addProperty("queue", Representation.REF);
		} else if (representation instanceof FullRepresentation) {
			resourceDescription.addProperty("count");
			resourceDescription.addProperty("display");
			resourceDescription.addProperty("queue", Representation.FULL);
		}
		return resourceDescription;
	}
	
	@PropertyGetter("display")
	public String getDisplay(QueueCount queueCount) {
		//Display queue name
		return queueCount.getQueue().getName();
	}
	
	@Override
	public String getResourceVersion() {
		return "2.3";
	}
}
