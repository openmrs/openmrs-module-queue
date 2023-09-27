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
import java.util.Collections;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.VisitQueueEntryService;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.api.SearchConfig;
import org.openmrs.module.webservices.rest.web.resource.api.SearchHandler;
import org.openmrs.module.webservices.rest.web.resource.api.SearchQuery;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InActiveQueueEntrySearchHandler implements SearchHandler {
	
	private final static SearchConfig SEARCH_CONFIG = new SearchConfig("default",
	        RestConstants.VERSION_1 + "/visit-queue-entry", Collections.singletonList("2.3 - 9.*"),
	        new SearchQuery.Builder("Allows you to include/exclude inactive queue entries")
	                .withOptionalParameters("includeInactive").build());
	
	@Override
	public SearchConfig getSearchConfig() {
		return SEARCH_CONFIG;
	}
	
	@Override
	public PageableResult search(RequestContext requestContext) throws ResponseException {
		VisitQueueEntryService visitQueueEntryService = Context.getService(VisitQueueEntryService.class);
		String includeInactive = requestContext.getParameter("includeInactive");
		
		if (!StringUtils.isBlank(includeInactive)) {
			try {
				boolean includeInactiveQueueEntries = Boolean.parseBoolean(includeInactive);
				if (includeInactiveQueueEntries) {
					return new NeedsPaging<>(new ArrayList<>(visitQueueEntryService.findAllVisitQueueEntries()),
					        requestContext);
				} else {
					return new NeedsPaging<>(new ArrayList<>(visitQueueEntryService.getActiveVisitQueueEntries()),
					        requestContext);
				}
			}
			catch (Exception exception) {
				log.error("Unable to parse string {} " + includeInactive, exception.getMessage(), exception);
			}
		}
		return new NeedsPaging<>(new ArrayList<>(visitQueueEntryService.getActiveVisitQueueEntries()), requestContext);
	}
}
