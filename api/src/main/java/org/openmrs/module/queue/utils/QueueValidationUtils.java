/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.utils;

import javax.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.Concept;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.QueueModuleConstants;
import org.openmrs.module.queue.model.QueueEntry;
import org.springframework.validation.Errors;

@Slf4j
public class QueueValidationUtils {
	
	/**
	 * Checks if the specified concept is a member of the conceptSet
	 *
	 * @param concept concept
	 * @param property global property that identifies certain concept
	 * @return true/false if concept belongs to a particular conceptSet
	 */
	public static boolean isAMember(@NotNull Concept concept, @NotNull String property) {
		String value = Context.getAdministrationService().getGlobalProperty(property);
		if (value == null || value.isEmpty()) {
			//throw the appropriate exception
			throw new IllegalArgumentException("Please configure concept set name for " + concept.getDisplayString()
			        + " via the global property " + property);
		}
		Concept conceptByName = Context.getConceptService().getConceptByName(value);
		return Context.getConceptService().getConceptsByConceptSet(conceptByName).contains(concept);
	}
	
	public static void validateQueueEntry(QueueEntry queueEntry, Errors errors) {
		if (queueEntry.getStatus() == null) {
			errors.rejectValue("status", "queueEntry.status.null", "The property status should not be null");
		} else {
			if (!isValidStatus(queueEntry.getStatus())) {
				errors.rejectValue("status", "queueEntry.status.invalid",
				    "The property status should be a member of configured queue status conceptSet.");
			}
		}
		if (queueEntry.getPriority() == null) {
			errors.rejectValue("priority", "queueEntry.priority.null", "The property priority should not be null");
		} else {
			if (!isValidPriority(queueEntry.getPriority())) {
				errors.rejectValue("priority", "queueEntry.priority.invalid",
				    "The property priority should be a member of configured queue priority conceptSet.");
			}
		}
	}
	
	public static boolean isValidStatus(@NotNull Concept concept) {
		return isAMember(concept, QueueModuleConstants.QUEUE_STATUS);
	}
	
	public static boolean isValidPriority(@NotNull Concept concept) {
		return isAMember(concept, QueueModuleConstants.QUEUE_PRIORITY);
	}
	
	public static boolean isValidService(@NotNull Concept concept) {
		return isAMember(concept, QueueModuleConstants.QUEUE_SERVICE);
	}
}
