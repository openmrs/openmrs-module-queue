/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.search;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.module.queue.model.Queue;

/**
 * Bean definition used for retrieving Queue Entries that meet specific criteria All properties
 * should be considered to further limit results (ANDed together) Any property that is null will not
 * limit by the related property Any Collection that is empty will return only those results for
 * which the related property is null Any Boolean property without a default value will not limit if
 * the property is null For example, to not limit by priority, set the priority property to null To
 * limit to only those entries whose priority is null, set the priority property to an empty
 * collection
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class QueueEntrySearchCriteria implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Collection<Queue> queues;
	
	private Collection<Location> locations;
	
	private Collection<Concept> services;
	
	private Patient patient;
	
	private Visit visit;
	
	private Boolean hasVisit;
	
	private Collection<Concept> priorities;
	
	private Collection<Concept> statuses;
	
	private Collection<Location> locationsWaitingFor;
	
	private Collection<Provider> providersWaitingFor;
	
	private Collection<Queue> queuesComingFrom;
	
	private Date startedOnOrAfter;
	
	private Date startedOnOrBefore;
	
	private Date startedOn;
	
	private Boolean isEnded = null;
	
	private Date endedOnOrAfter;
	
	private Date endedOnOrBefore;
	
	private Date endedOn;
	
	private boolean includedVoided = false;
}
