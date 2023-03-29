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
import org.openmrs.module.queue.model.QueueRoom;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Slf4j
@Handler(supports = { QueueRoom.class }, order = 50)
public class QueueRoomValidation implements Validator {
	
	@Override
	public boolean supports(Class<?> aClass) {
		return QueueRoom.class.isAssignableFrom(aClass);
	}
	
	@Override
	public void validate(Object obj, Errors errors) {
		log.debug("{}.validate", this.getClass().getName());
		//instanceof checks for null
		if (!(obj instanceof QueueRoom)) {
			throw new IllegalArgumentException(
			        "The parameter target should not be null & must be of type" + QueueRoom.class);
		}
		QueueRoom queueRoom = (QueueRoom) obj;
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "queueRoom.name.null", "QueueRoom name can't be null");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "queue", "queueRoom.queue.null",
		    "Queue in QueueRoom can't be null");
	}
}
