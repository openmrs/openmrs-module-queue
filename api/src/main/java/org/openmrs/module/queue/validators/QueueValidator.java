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

import javax.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.Location;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.utils.QueueValidationUtils;
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
			throw new IllegalArgumentException("The parameter target should not be null & must be of type" + Queue.class);
		}
		Queue queue = (Queue) target;
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "queue.name.null", "Queue name can't be null");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "location", "queue.location.null", "Location can't be null");
		
		if (!isValidLocation(queue.getLocation())) {
			errors.rejectValue("location", "queue.location.null", "Location is null or doesn't exists");
		}
		
		if (queue.getService() == null) {
			errors.rejectValue("service", "QueueEntry.service.null", "The property service should not be null");
		} else {
			if (!QueueValidationUtils.isValidService(queue.getService())) {
				errors.rejectValue("service", "Queue.service.invalid",
				    "The property service should be a member of configured queue service conceptSet.");
			}
		}
	}
	
	/**
	 * For now checks for existence
	 * 
	 * @param location location to check if it exists
	 * @return true or false if the location exists
	 */
	public boolean isValidLocation(@NotNull Location location) {
		if (location == null) {
			return false;
		}
		Location isExistentLocation = Context.getLocationService().getLocationByUuid(location.getUuid());
		return isExistentLocation != null;
	}
	
}
