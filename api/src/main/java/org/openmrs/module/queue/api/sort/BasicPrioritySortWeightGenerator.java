/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.sort;

import java.util.List;

import org.openmrs.Concept;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.model.QueueEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Basic implementation of a sort weight generator that is based on the configured priority In this
 * implementation, the position of the priority within the configured concept set determines the
 * relative priority, where the set members are ordered in increasing priority. This means the lower
 * the position in the concept set, the lower the sort weight (and the lower the priority)
 */
@Component("basicPrioritySortWeightGenerator")
public class BasicPrioritySortWeightGenerator implements SortWeightGenerator {
	
	private final QueueServicesWrapper services;
	
	@Autowired
	public BasicPrioritySortWeightGenerator(QueueServicesWrapper services) {
		this.services = services;
	}
	
	/**
	 * Generates a sort weight based on the order of the priority within the configured concept set
	 */
	public Double generateSortWeight(QueueEntry queueEntry) {
		List<Concept> allowedPriorities = services.getAllowedPriorities(queueEntry.getQueue());
		int ret = 0; // Default to the lowest sort weight
		if (queueEntry.getPriority() != null && allowedPriorities.contains(queueEntry.getPriority())) {
			ret = allowedPriorities.indexOf(queueEntry.getPriority());
		}
		return (double) ret;
	}
}
