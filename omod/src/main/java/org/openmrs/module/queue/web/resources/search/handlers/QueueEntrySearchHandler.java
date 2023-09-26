/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.web.resources.search.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.api.SearchConfig;
import org.openmrs.module.webservices.rest.web.resource.api.SearchQuery;
import org.openmrs.module.webservices.rest.web.resource.api.SubResourceSearchHandler;
import org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.stereotype.Component;

@Component
public class QueueEntrySearchHandler implements SubResourceSearchHandler {
	
	private final static SearchConfig SEARCH_CONFIG = new SearchConfig("default", RestConstants.VERSION_1 + "/queue/entry",
	        Collections.singletonList("2.3 - 9.*"),
	        new SearchQuery.Builder("Allows you to find queue entries by status").withOptionalParameters("status").build());
	
	@Override
	public PageableResult search(String parentUuid, RequestContext requestContext) throws ResponseException {
		String status = requestContext.getParameter("status");
		
		if (StringUtils.isBlank(status) || StringUtils.isBlank(parentUuid)) {
			return new EmptySearchResult();
		}
		Collection<QueueEntry> queueEntries = Context.getService(QueueEntryService.class)
		        .searchQueueEntriesByConceptStatus(status, false);
		return new NeedsPaging<>(new ArrayList<>(queueEntries), requestContext);
	}
	
	@Override
	public SearchConfig getSearchConfig() {
		return SEARCH_CONFIG;
	}
	
	@Override
	public PageableResult search(RequestContext requestContext) throws ResponseException {
		throw new UnsupportedOperationException("Cannot search for queue entries without parent queue");
	}
}
