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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.utils.QueueEntrySearchCriteria;
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
        + "/queue-entry-metrics", supportedClass = QueueEntryMetric.class, supportedOpenmrsVersions = { "2.3 - 9.*" })
public class QueueEntryMetricsResource extends DelegatingCrudResource<SimpleObject> {
	
	public static final String SEARCH_PARAM_STATUS = "status";
	
	public static final String SEARCH_PARAM_SERVICE = "service";
	
	public static final String SEARCH_PARAM_LOCATION = "location";
	
	private final QueueServicesWrapper services;
	
	public QueueEntryMetricsResource() {
		this.services = Context.getRegisteredComponents(QueueServicesWrapper.class).get(0);
	}
	
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
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		Map<String, String[]> parameterMap = getParameters(requestContext);
		for (String parameterName : parameterMap.keySet()) {
			switch (parameterName) {
				case SEARCH_PARAM_STATUS: {
					criteria.setStatuses(services.getConcepts(parameterMap.get(SEARCH_PARAM_STATUS)));
					break;
				}
				case SEARCH_PARAM_SERVICE: {
					criteria.setServices(services.getConcepts(parameterMap.get(SEARCH_PARAM_SERVICE)));
					break;
				}
				case SEARCH_PARAM_LOCATION: {
					criteria.setLocations(services.getLocations(parameterMap.get(SEARCH_PARAM_LOCATION)));
					break;
				}
				default: {
					log.debug("Unhandled search parameter found: " + parameterName);
				}
			}
		}
		if (criteria.getStatuses() == null && criteria.getServices() == null && criteria.getLocations() == null) {
			return new EmptySearchResult();
		}
		
		StringBuilder metric = new StringBuilder();
		if (criteria.getStatuses() != null) {
			metric.append(String.join(",", parameterMap.get(SEARCH_PARAM_STATUS)));
		}
		if (criteria.getServices() != null) {
			if (metric.length() > 0) {
				metric.append(" ");
			}
			metric.append(String.join(",", parameterMap.get(SEARCH_PARAM_SERVICE)));
		}
		if (criteria.getLocations() != null && metric.length() == 0) {
			metric.append(StringUtils.join(parameterMap.get(SEARCH_PARAM_LOCATION)));
		}
		
		Long count = services.getQueueEntryService().getCountOfQueueEntries(criteria);
		return new GenericSingleObjectResult(
		        Arrays.asList(new PropValue("metric", metric.toString()), new PropValue("count", count)));
	}
	
	/**
	 * @return the parameters for the given request context
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, String[]> getParameters(RequestContext requestContext) {
		return requestContext.getRequest().getParameterMap();
	}
}
