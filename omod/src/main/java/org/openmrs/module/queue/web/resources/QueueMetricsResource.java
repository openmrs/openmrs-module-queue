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

import java.util.Arrays;
import java.util.Optional;

import org.openmrs.Concept;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueService;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.web.resources.custom.response.GenericSingleObjectResult;
import org.openmrs.module.queue.web.resources.custom.response.PropValue;
import org.openmrs.module.queue.web.resources.custom.response.QueueMetric;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

@Resource(name = RestConstants.VERSION_1 + "/queue-metrics", supportedClass = QueueMetric.class, supportedOpenmrsVersions = {
        "2.0 - 2.*" })
public class QueueMetricsResource extends DelegatingCrudResource<SimpleObject> {
	
	@Override
	public SimpleObject getByUniqueId(String s) {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	protected void delete(SimpleObject simpleObject, String s, RequestContext requestContext) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	public SimpleObject newDelegate() {
		return new SimpleObject();
	}
	
	@Override
	public SimpleObject save(SimpleObject simpleObject) {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	protected PageableResult doSearch(RequestContext requestContext) {
		String queueUuid = requestContext.getParameter("queue");
		if (!queueUuid.isEmpty()) {
			
			Optional<Queue> queueOptional = Context.getService(QueueService.class).getQueueByUuid(queueUuid);
			Concept status = requestContext.getParameter("status") != null
			        ? Context.getConceptService().getConceptByUuid(requestContext.getParameter("status"))
			        : null;
			if (queueOptional.isPresent()) {
				Double averageWaitTime = Context.getService(QueueService.class).getQueueAverageWaitTime(queueOptional.get(),
				    status);
				return new GenericSingleObjectResult(Arrays.asList(
				    new PropValue("queue", Context.getService(QueueService.class).getQueueByUuid(queueUuid).get().getName()),
				    new PropValue("averageWaitTime", averageWaitTime)));
			}
			
		}
		
		return new EmptySearchResult();
	}
	
	@Override
	public void purge(SimpleObject simpleObject, RequestContext requestContext) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation representation) {
		return null;
	}
}
