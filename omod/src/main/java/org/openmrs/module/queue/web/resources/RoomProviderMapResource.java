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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.search.RoomProviderMapSearchCriteria;
import org.openmrs.module.queue.model.RoomProviderMap;
import org.openmrs.module.queue.web.resources.parser.RoomProviderMapSearchCriteriaParser;
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
import org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

@Resource(name = RestConstants.VERSION_1
        + "/roomprovidermap", supportedClass = RoomProviderMap.class, supportedOpenmrsVersions = { "2.3 - 9.*" })
public class RoomProviderMapResource extends DelegatingCrudResource<RoomProviderMap> {
	
	private final QueueServicesWrapper services;
	
	private final RoomProviderMapSearchCriteriaParser searchCriteriaParser;
	
	public RoomProviderMapResource() {
		this.services = Context.getService(QueueServicesWrapper.class);
		this.searchCriteriaParser = Context.getRegisteredComponents(RoomProviderMapSearchCriteriaParser.class).get(0);
	}
	
	public RoomProviderMapResource(QueueServicesWrapper services, RoomProviderMapSearchCriteriaParser parser) {
		this.services = services;
		this.searchCriteriaParser = parser;
	}
	
	@Override
	public RoomProviderMap getByUniqueId(String uuid) {
		Optional<RoomProviderMap> optional = services.getRoomProviderMapService().getRoomProviderMapByUuid(uuid);
		if (!optional.isPresent()) {
			throw new ObjectNotFoundException("Could not find roomProviderMap with UUID " + uuid);
		}
		return optional.get();
	}
	
	@Override
	protected void delete(RoomProviderMap rpm, String voidReason, RequestContext context) throws ResponseException {
		services.getRoomProviderMapService().voidRoomProviderMap(rpm, voidReason);
	}
	
	@Override
	public RoomProviderMap newDelegate() {
		return new RoomProviderMap();
	}
	
	@Override
	public RoomProviderMap save(RoomProviderMap roomProviderMap) {
		return services.getRoomProviderMapService().createRoomProviderMap(roomProviderMap);
	}
	
	@Override
	public void purge(RoomProviderMap roomProviderMap, RequestContext requestContext) throws ResponseException {
		services.getRoomProviderMapService().purgeRoomProviderMap(roomProviderMap);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected PageableResult doSearch(RequestContext requestContext) {
		Map<String, String[]> parameters = requestContext.getRequest().getParameterMap();
		if (!searchCriteriaParser.hasSearchParameter(parameters)) {
			return new EmptySearchResult();
		}
		RoomProviderMapSearchCriteria criteria = searchCriteriaParser.constructFromRequest(parameters);
		List<RoomProviderMap> rpms = services.getRoomProviderMapService().getRoomProviderMaps(criteria);
		return new NeedsPaging<>(rpms, requestContext);
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation representation) {
		DelegatingResourceDescription resourceDescription = new DelegatingResourceDescription();
		if (representation instanceof RefRepresentation) {
			this.addSharedResourceDescriptionProperties(resourceDescription);
			resourceDescription.addProperty("queueRoom", Representation.REF);
			resourceDescription.addProperty("provider", Representation.REF);
			resourceDescription.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		} else if (representation instanceof DefaultRepresentation) {
			this.addSharedResourceDescriptionProperties(resourceDescription);
			resourceDescription.addProperty("queueRoom", Representation.REF);
			resourceDescription.addProperty("provider", Representation.REF);
			resourceDescription.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		} else if (representation instanceof FullRepresentation) {
			this.addSharedResourceDescriptionProperties(resourceDescription);
			resourceDescription.addProperty("queueRoom", Representation.FULL);
			resourceDescription.addProperty("provider", Representation.FULL);
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
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription resourceDescription = new DelegatingResourceDescription();
		resourceDescription.addProperty("queueRoom");
		resourceDescription.addProperty("provider");
		return resourceDescription;
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
		return this.getCreatableProperties();
	}
	
}
