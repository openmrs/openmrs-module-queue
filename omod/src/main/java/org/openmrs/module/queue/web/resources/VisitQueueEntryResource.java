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
import java.util.Collection;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.VisitQueueEntryService;
import org.openmrs.module.queue.model.VisitQueueEntry;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
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
 * By convention, resource names should use exclusively lowercase letters. Similarly, dashes (-) are
 * conventionally used in place of underscores (_).
 */
@Slf4j
@SuppressWarnings("unused")
@Resource(name = RestConstants.VERSION_1
        + "/visit-queue-entry", supportedClass = VisitQueueEntry.class, supportedOpenmrsVersions = { "2.0 - 2.*" })
public class VisitQueueEntryResource extends DelegatingCrudResource<VisitQueueEntry> {
	
	private final VisitQueueEntryService visitQueueEntryService;
	
	public VisitQueueEntryResource() {
		this.visitQueueEntryService = Context.getService(VisitQueueEntryService.class);
	}
	
	@Override
	public VisitQueueEntry getByUniqueId(@NotNull String uuid) {
		Optional<VisitQueueEntry> visitQueueEntryOptional = this.visitQueueEntryService.getVisitQueueEntryByUuid(uuid);
		if (!visitQueueEntryOptional.isPresent()) {
			throw new ObjectNotFoundException("Could not find visit queue entry with uuid " + uuid);
		}
		return visitQueueEntryOptional.get();
	}
	
	@Override
	protected void delete(VisitQueueEntry visitQueueEntry, String voidReason, RequestContext requestContext)
	        throws ResponseException {
		this.visitQueueEntryService.voidVisitQueueEntry(visitQueueEntry.getUuid(), voidReason);
	}
	
	@Override
	public VisitQueueEntry newDelegate() {
		return new VisitQueueEntry();
	}
	
	@Override
	public VisitQueueEntry save(VisitQueueEntry visitQueueEntry) {
		return this.visitQueueEntryService.createVisitQueueEntry(visitQueueEntry);
	}
	
	@Override
	public void purge(VisitQueueEntry visitQueueEntry, RequestContext requestContext) throws ResponseException {
		this.visitQueueEntryService.purgeQueueEntry(visitQueueEntry);
	}
	
	@Override
	protected PageableResult doGetAll(RequestContext requestContext) throws ResponseException {
		return new NeedsPaging<>(new ArrayList<>(this.visitQueueEntryService.getActiveVisitQueueEntries()), requestContext);
	}
	
	@Override
	protected PageableResult doSearch(RequestContext requestContext) {
		String status = requestContext.getParameter("status");
		String service = requestContext.getParameter("service");
		String location = requestContext.getParameter("location");
		//Both status,location & service are nullable
		Collection<VisitQueueEntry> visitQueueEntries = this.visitQueueEntryService.findVisitQueueEntries(status, service,
		    location);
		return new NeedsPaging<>(new ArrayList<>(visitQueueEntries), requestContext);
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription resourceDescription = new DelegatingResourceDescription();
		resourceDescription.addProperty("visit");
		resourceDescription.addProperty("queueEntry");
		return resourceDescription;
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation representation) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		if (representation instanceof RefRepresentation) {
			this.addSharedResourceDescriptionProperties(description);
			description.addProperty("visit", Representation.REF);
			description.addProperty("queueEntry", Representation.REF);
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		} else if (representation instanceof DefaultRepresentation) {
			this.addSharedResourceDescriptionProperties(description);
			description.addProperty("visit", Representation.DEFAULT);
			description.addProperty("queueEntry", Representation.DEFAULT);
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		} else if (representation instanceof FullRepresentation) {
			this.addSharedResourceDescriptionProperties(description);
			description.addProperty("visit", Representation.FULL);
			description.addProperty("queueEntry", Representation.FULL);
			description.addProperty("auditInfo");
		} else if (representation instanceof CustomRepresentation) {
			description = null;
		}
		return description;
	}
	
	private void addSharedResourceDescriptionProperties(DelegatingResourceDescription resourceDescription) {
		resourceDescription.addSelfLink();
		resourceDescription.addProperty("uuid");
	}
	
	@Override
	public String getResourceVersion() {
		return "2.3";
	}
}
