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

import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.VisitQueueEntryService;
import org.openmrs.module.queue.web.resources.custom.response.GenericSingleObjectResult;
import org.openmrs.module.queue.web.resources.custom.response.PropValue;
import org.openmrs.module.queue.web.resources.custom.response.QueueEntryMetric;
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

/**
 * Sort of a placeholder resource
 */
@SuppressWarnings("unused")
@Resource(name = RestConstants.VERSION_1
        + "/queue-entry-metrics", supportedClass = QueueEntryMetric.class, supportedOpenmrsVersions = { "2.0 - 2.*" })
public class QueueEntryMetricsResource extends DelegatingCrudResource<SimpleObject> {
	
	@Override
	public SimpleObject getByUniqueId(String uuid) {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	protected void delete(SimpleObject queueEntryMetric, String s, RequestContext requestContext) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	public SimpleObject newDelegate() {
		return new SimpleObject();
	}
	
	@Override
	public SimpleObject save(SimpleObject queueEntryMetric) {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	public void purge(SimpleObject simpleObject, RequestContext requestContext) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation representation) {
		return null;
	}
	
	@Override
	protected PageableResult doSearch(RequestContext requestContext) {
		String status = requestContext.getParameter("status");
		String service = requestContext.getParameter("service");
		String locationUuid = requestContext.getParameter("location");
		
		if (service != null && status != null && locationUuid != null) {
			Long patientsCount = Context.getService(VisitQueueEntryService.class)
			        .getVisitQueueEntriesCountByLocationStatusAndService(status, service, locationUuid);
			
			return new GenericSingleObjectResult(
			        Arrays.asList(new PropValue("metric", status + " " + service), new PropValue("count", patientsCount)));
		} else if (service != null && !service.isEmpty()) {
			Long count = Context.getService(VisitQueueEntryService.class).getVisitQueueEntriesCountByService(service);
			return new GenericSingleObjectResult(
			        Arrays.asList(new PropValue("metric", service), new PropValue("count", count)));
			
		} else if (status != null && !status.isEmpty()) {
			Long count = Context.getService(VisitQueueEntryService.class).getVisitQueueEntriesCountByStatus(status);
			return new GenericSingleObjectResult(
			        Arrays.asList(new PropValue("metric", status), new PropValue("count", count)));
			
		} else if (locationUuid != null && !locationUuid.isEmpty()) {
			Long count = Context.getService(VisitQueueEntryService.class).getVisitQueueEntriesCountByLocation(locationUuid);
			return new GenericSingleObjectResult(
			        Arrays.asList(new PropValue("metric", locationUuid), new PropValue("count", count)));
		}
		
		return new EmptySearchResult();
	}
}
