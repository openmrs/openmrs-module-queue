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

import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
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
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

@SuppressWarnings("unused")
@SubResource(parent = QueueResource.class, path = "entry", supportedClass = QueueEntry.class, supportedOpenmrsVersions = {
        "2.3 - 9.*" }, order = 10)
public class QueueEntrySubResource extends DelegatingSubResource<QueueEntry, Queue, QueueResource> {
	
	@Override
	public Queue getParent(QueueEntry queueEntry) {
		return queueEntry.getQueue();
	}
	
	@Override
	public void setParent(QueueEntry queueEntry, Queue queue) {
		queueEntry.setQueue(queue);
	}
	
	@Override
	public PageableResult doGetAll(Queue queue, RequestContext requestContext) throws ResponseException {
		Collection<QueueEntry> queueEntries = queue.getActiveQueueEntries();
		return new NeedsPaging<>(new ArrayList<>(queueEntries), requestContext);
	}
	
	@Override
	public QueueEntry getByUniqueId(@NotNull String uuid) {
		Optional<QueueEntry> queueEntryOptional = Context.getService(QueueEntryService.class).getQueueEntryByUuid(uuid);
		if (!queueEntryOptional.isPresent()) {
			throw new ObjectNotFoundException("Could not find queue entry with UUID " + uuid);
		}
		return queueEntryOptional.get();
	}
	
	@Override
	protected void delete(QueueEntry queueEntry, String voidReason, RequestContext requestContext) throws ResponseException {
		Context.getService(QueueEntryService.class).voidQueueEntry(queueEntry.getUuid(), voidReason);
	}
	
	@Override
	public QueueEntry newDelegate() {
		return new QueueEntry();
	}
	
	@Override
	public QueueEntry save(QueueEntry queueEntry) {
		return Context.getService(QueueEntryService.class).createQueueEntry(queueEntry);
	}
	
	@Override
	public void purge(QueueEntry queueEntry, RequestContext requestContext) throws ResponseException {
		Context.getService(QueueEntryService.class).purgeQueueEntry(queueEntry);
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("status");
		description.addProperty("priority");
		description.addProperty("priorityComment");
		description.addProperty("patient");
		description.addProperty("sortWeight");
		description.addProperty("startedAt");
		description.addProperty("locationWaitingFor");
		description.addProperty("queueComingFrom");
		description.addProperty("providerWaitingFor");
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("priorityComment");
		description.addProperty("sortWeight");
		description.addProperty("endedAt");
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation representation) {
		DelegatingResourceDescription resourceDescription = new DelegatingResourceDescription();
		if (representation instanceof RefRepresentation) {
			this.addSharedResourceDescriptionProperties(resourceDescription);
			resourceDescription.addProperty("status", Representation.REF);
			resourceDescription.addProperty("patient", Representation.REF);
			resourceDescription.addProperty("patient", Representation.REF);
			resourceDescription.addProperty("priority", Representation.REF);
			resourceDescription.addProperty("locationWaitingFor", Representation.REF);
			resourceDescription.addProperty("queueComingFrom", Representation.REF);
			resourceDescription.addProperty("providerWaitingFor", Representation.REF);
			resourceDescription.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		} else if (representation instanceof DefaultRepresentation) {
			this.addSharedResourceDescriptionProperties(resourceDescription);
			resourceDescription.addProperty("status", Representation.DEFAULT);
			resourceDescription.addProperty("patient", Representation.DEFAULT);
			resourceDescription.addProperty("visit", Representation.DEFAULT);
			resourceDescription.addProperty("priority", Representation.DEFAULT);
			resourceDescription.addProperty("locationWaitingFor", Representation.DEFAULT);
			resourceDescription.addProperty("queueComingFrom", Representation.DEFAULT);
			resourceDescription.addProperty("providerWaitingFor", Representation.DEFAULT);
			resourceDescription.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		} else if (representation instanceof FullRepresentation) {
			this.addSharedResourceDescriptionProperties(resourceDescription);
			resourceDescription.addProperty("voided");
			resourceDescription.addProperty("voidReason");
			resourceDescription.addProperty("auditInfo");
			resourceDescription.addProperty("status", Representation.FULL);
			resourceDescription.addProperty("patient", Representation.FULL);
			resourceDescription.addProperty("visit", Representation.FULL);
			resourceDescription.addProperty("priority", Representation.FULL);
			resourceDescription.addProperty("locationWaitingFor", Representation.FULL);
			resourceDescription.addProperty("queueComingFrom", Representation.FULL);
			resourceDescription.addProperty("providerWaitingFor", Representation.FULL);
		} else if (representation instanceof CustomRepresentation) {
			//Let the user decide
			resourceDescription = null;
		}
		return resourceDescription;
	}
	
	private void addSharedResourceDescriptionProperties(DelegatingResourceDescription resourceDescription) {
		resourceDescription.addSelfLink();
		resourceDescription.addProperty("uuid");
		resourceDescription.addProperty("display");
		resourceDescription.addProperty("priorityComment");
		resourceDescription.addProperty("sortWeight");
		resourceDescription.addProperty("startedAt");
		resourceDescription.addProperty("endedAt");
	}
	
	@PropertyGetter("display")
	public String getDisplay(QueueEntry queueEntry) {
		//Display patient name
		return queueEntry.getPatient().getPerson().getPersonName().getFullName();
	}
	
	@Override
	public String getResourceVersion() {
		return "2.3";
	}
}
