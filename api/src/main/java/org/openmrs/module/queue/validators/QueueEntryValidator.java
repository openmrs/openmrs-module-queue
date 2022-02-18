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

import static org.springframework.validation.ValidationUtils.rejectIfEmptyOrWhitespace;

import org.openmrs.annotation.Handler;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.utils.QueueValidationUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Handler(supports = { QueueEntry.class }, order = 50)
public class QueueEntryValidator implements Validator {
	
	@Override
	public boolean supports(Class<?> clazz) {
		return QueueEntry.class.isAssignableFrom(clazz);
	}
	
	@Override
	public void validate(Object target, Errors errors) {
		if (!(target instanceof QueueEntry)) {
			throw new IllegalArgumentException("the parameter target must be of type " + QueueEntry.class);
		}
		rejectIfEmptyOrWhitespace(errors, "patient", "queueEntry.patient.null", "The property patient should not be null");
		rejectIfEmptyOrWhitespace(errors, "startedAt", "queueEntry.startedAt.null",
		    "The property startedAt should not be null");
		
		QueueEntry queueEntry = (QueueEntry) target;
		if (queueEntry.getEndedAt() != null) {
			//queueEntry.endedAt >= queueEntry.startedAt
			if (queueEntry.getStartedAt().after(queueEntry.getEndedAt())) {
				errors.rejectValue("endedAt", "queueEntry.endedAt.invalid",
				    "Queue entry endedAt should after the startedAt date");
			}
		}
		
		QueueValidationUtils.validateQueueEntry((QueueEntry) target, errors);
	}
}
