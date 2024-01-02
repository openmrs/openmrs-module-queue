/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.VisitService;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Getter
public class QueueServicesWrapper {
	
	private final QueueService queueService;
	
	private final QueueEntryService queueEntryService;
	
	private final QueueRoomService queueRoomService;
	
	private final RoomProviderMapService roomProviderMapService;
	
	private final ConceptService conceptService;
	
	private final LocationService locationService;
	
	private final PatientService patientService;
	
	private final VisitService visitService;
	
	private final ProviderService providerService;
	
	@Autowired
	public QueueServicesWrapper(@Qualifier("queue.QueueService") QueueService queueService,
	    @Qualifier("queue.QueueEntryService") QueueEntryService queueEntryService,
	    @Qualifier("queue.QueueRoomService") QueueRoomService queueRoomService,
	    @Qualifier("queue.RoomProviderMapService") RoomProviderMapService roomProviderMapService,
	    ConceptService conceptService, LocationService locationService, PatientService patientService,
	    VisitService visitService, ProviderService providerService) {
		this.queueService = queueService;
		this.queueEntryService = queueEntryService;
		this.queueRoomService = queueRoomService;
		this.roomProviderMapService = roomProviderMapService;
		this.conceptService = conceptService;
		this.locationService = locationService;
		this.patientService = patientService;
		this.visitService = visitService;
		this.providerService = providerService;
	}
	
	/**
	 * @param conceptRefs array of concept references
	 * @return a List of Concepts matching those references
	 */
	public List<Concept> getConcepts(String[] conceptRefs) {
		List<Concept> ret = new ArrayList<>();
		for (String conceptRef : conceptRefs) {
			ret.add(getConcept(conceptRef.trim()));
		}
		return ret;
	}
	
	/**
	 * @param conceptRef a uuid, source:mapping, or unique name for the concept to retrieve
	 * @return the concept that matches the conceptRef
	 */
	public Concept getConcept(String conceptRef) {
		if (StringUtils.isBlank(conceptRef)) {
			return null;
		}
		Concept c = getConceptService().getConceptByUuid(conceptRef);
		if (c != null) {
			return c;
		}
		//handle mapping
		int idx = conceptRef.indexOf(":");
		if (idx >= 0 && idx < conceptRef.length() - 1) {
			String conceptSource = conceptRef.substring(0, idx);
			String conceptCode = conceptRef.substring(idx + 1);
			c = getConceptService().getConceptByMapping(conceptCode, conceptSource);
			if (c != null) {
				return c;
			}
		}
		//handle name.  this isn't ideal, as core will just log a warning if there are 2 concepts with the same name
		//but there are no alternative suitable methods to get a List of Concepts that exactly match a given name
		c = getConceptService().getConceptByName(conceptRef);
		if (c == null) {
			throw new IllegalArgumentException("Unable to find concept: " + conceptRef);
		}
		return c;
	}
	
	/**
	 * @param locationRefs array of concept references
	 * @return a List of Locations matching those references
	 */
	public List<Location> getLocations(String[] locationRefs) {
		List<Location> ret = new ArrayList<>();
		for (String locationRef : locationRefs) {
			ret.add(getLocation(locationRef.trim()));
		}
		return ret;
	}
	
	/**
	 * @param locationRef a uuid or unique name for the location to retrieve
	 * @return the location that matches the locationRef
	 */
	public Location getLocation(String locationRef) {
		if (StringUtils.isBlank(locationRef)) {
			return null;
		}
		Location l = getLocationService().getLocationByUuid(locationRef);
		if (l != null) {
			return l;
		}
		List<Location> locations = getLocationService().getLocations(locationRef);
		if (locations.size() == 1) {
			return locations.get(0);
		} else if (locations.size() > 1) {
			throw new IllegalArgumentException("More than one location is found with name: " + locationRef);
		}
		throw new IllegalArgumentException("Unable to find location: " + locationRef);
	}
	
	/**
	 * @param patientRef a uuid for the patient to retrieve
	 * @return the patient that matches the patientRef
	 */
	public Patient getPatient(String patientRef) {
		if (StringUtils.isBlank(patientRef)) {
			return null;
		}
		Patient p = getPatientService().getPatientByUuid(patientRef);
		if (p != null) {
			return p;
		}
		throw new IllegalArgumentException("Unable to find patient: " + patientRef);
	}
	
	/**
	 * @param visitRef a uuid for the Visit to retrieve
	 * @return the Visit that matches the visitRef
	 */
	public Visit getVisit(String visitRef) {
		if (StringUtils.isBlank(visitRef)) {
			return null;
		}
		Visit v = getVisitService().getVisitByUuid(visitRef);
		if (v != null) {
			return v;
		}
		throw new IllegalArgumentException("Unable to find Visit: " + visitRef);
	}
	
	/**
	 * @param queueRefs array of Queue references
	 * @return a List of Queues matching those references
	 */
	public List<Queue> getQueues(String[] queueRefs) {
		List<Queue> ret = new ArrayList<>();
		for (String queueRef : queueRefs) {
			ret.add(getQueue(queueRef.trim()));
		}
		return ret;
	}
	
	/**
	 * @param queueRef a uuid for the queue to retrieve
	 * @return the queue that matches the queueRef
	 */
	public Queue getQueue(String queueRef) {
		if (StringUtils.isBlank(queueRef)) {
			return null;
		}
		Queue queue = getQueueService().getQueueByUuid(queueRef).orElse(null);
		if (queue != null) {
			return queue;
		}
		throw new IllegalArgumentException("Unable to find queue: " + queueRef);
	}
	
	/**
	 * @param providerRefs array of Provider references
	 * @return a List of Providers matching those references
	 */
	public List<Provider> getProviders(String[] providerRefs) {
		List<Provider> ret = new ArrayList<>();
		for (String providerRef : providerRefs) {
			ret.add(getProvider(providerRef.trim()));
		}
		return ret;
	}
	
	/**
	 * @param providerRef a uuid for the provider to retrieve
	 * @return the provider that matches the providerRef
	 */
	public Provider getProvider(String providerRef) {
		if (StringUtils.isBlank(providerRef)) {
			return null;
		}
		Provider provider = getProviderService().getProviderByUuid(providerRef);
		if (provider != null) {
			return provider;
		}
		throw new IllegalArgumentException("Unable to find provider: " + providerRef);
	}
	
	/**
	 * @param queueRoomRefs array of QueueRoom references
	 * @return a List of QueueRooms matching those references
	 */
	public List<QueueRoom> getQueueRooms(String[] queueRoomRefs) {
		List<QueueRoom> ret = new ArrayList<>();
		for (String queueRoomRef : queueRoomRefs) {
			ret.add(getQueueRoom(queueRoomRef.trim()));
		}
		return ret;
	}
	
	/**
	 * @param queueRoomRef a uuid for the queueRoom to retrieve
	 * @return the queueRoom that matches the queueRoomRef
	 */
	public QueueRoom getQueueRoom(String queueRoomRef) {
		if (StringUtils.isBlank(queueRoomRef)) {
			return null;
		}
		QueueRoom queueRoom = getQueueRoomService().getQueueRoomByUuid(queueRoomRef).orElse(null);
		if (queueRoom != null) {
			return queueRoom;
		}
		throw new IllegalArgumentException("Unable to find queueRoom: " + queueRoomRef);
	}
}
