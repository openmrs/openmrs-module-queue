/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.validators;

import org.openmrs.annotation.Handler;
import org.openmrs.module.queue.model.VisitQueueEntry;
import org.openmrs.module.queue.utils.QueueValidationUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Handler(supports = { VisitQueueEntry.class }, order = 50)
public class VisitQueueEntryValidator implements Validator {
	
	@Override
	public boolean supports(Class<?> clazz) {
		return VisitQueueEntry.class.isAssignableFrom(clazz);
	}
	
	@Override
	public void validate(Object target, Errors errors) {
		if (!(target instanceof VisitQueueEntry)) {
			throw new IllegalArgumentException("the parameter target must be of type " + VisitQueueEntry.class);
		}
		//Reject null visit & queueEntry
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "visit", "visitQueueEntry.visit.null",
		    "The property visit should not be null");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "queueEntry", "visitQueueEntry.queueEntry.null",
		    "The property queueEntry should not be null");
		
		VisitQueueEntry visitQueueEntry = (VisitQueueEntry) target;
		QueueValidationUtils.validateQueueEntry(visitQueueEntry.getQueueEntry(), errors);
	}
}
