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

import lombok.extern.slf4j.Slf4j;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.model.Queue;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * Validates Queue object
 */
@Slf4j
@Handler(supports = { Queue.class }, order = 50)
public class QueueValidator implements Validator {
	
	@Override
	public boolean supports(Class<?> clazz) {
		return Queue.class.isAssignableFrom(clazz);
	}
	
	@Override
	public void validate(Object target, Errors errors) {
		log.debug("{}.validate", this.getClass().getName());
		//instanceof checks for null
		if (!(target instanceof Queue)) {
			throw new IllegalArgumentException("Invalid Queue class: " + target.getClass().getName());
		}
		Queue queue = (Queue) target;
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "queue.name.null", "Queue name can't be null");
		
		// TODO: Check if the location is tagged as a Queue Location?
		
		QueueServicesWrapper queueServices = Context.getRegisteredComponents(QueueServicesWrapper.class).get(0);
		if (queue.getService() == null) {
			return;
		}
		
		if (!queueServices.getAllowedServices().contains(queue.getService())) {
			errors.rejectValue("service", "Queue.service.invalid",
			    "The property service should be a member of configured queue service conceptSet.");
		}
	}
}
