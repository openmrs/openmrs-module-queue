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

import java.util.List;

import org.openmrs.Visit;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
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
		
		rejectIfEmptyOrWhitespace(errors, "queue", "queueEntry.queue.null", "The property queue should not be null");
		rejectIfEmptyOrWhitespace(errors, "patient", "queueEntry.patient.null", "The property patient should not be null");
		rejectIfEmptyOrWhitespace(errors, "startedAt", "queueEntry.startedAt.null",
		    "The property startedAt should not be null");
		
		QueueEntry queueEntry = (QueueEntry) target;
		Queue queue = queueEntry.getQueue();
		
		Visit visit = queueEntry.getVisit();
		if (visit != null && queueEntry.getStartedAt() != null) {
			if (queueEntry.getStartedAt().before(visit.getStartDatetime())) {
				errors.rejectValue("startedAt", "queue.entry.error.cannotStartBeforeVisitStartDate",
				    "A queue entry cannot start before the associated visit start date");
			} else if (visit.getStopDatetime() != null) {
				if (queueEntry.getStartedAt().after(visit.getStopDatetime())) {
					errors.rejectValue("startedAt", "queue.entry.error.cannotStartAfterVisitStopDate",
					    "A queue entry cannot start after the associated visit stop date");
				} else if (queueEntry.getEndedAt() == null || queueEntry.getEndedAt().after(visit.getStopDatetime())) {
					errors.rejectValue("endedAt", "queue.entry.error.cannotEndAfterVisitStopDate",
					    "A queue entry cannot end after the associated visit stop date");
				}
			}
			
			if (queueEntry.getPatient() != null && (visit.getPatient() == null
			        || !visit.getPatient().getPatientId().equals(queueEntry.getPatient().getPatientId()))) {
				errors.rejectValue("visit", "queue.entry.error.visit.wrongPatient",
				    "This queue entry is associated with a visit for the wrong patient");
			}
		}
		
		if (queueEntry.getEndedAt() != null && queueEntry.getStartedAt() != null) {
			if (queueEntry.getStartedAt().after(queueEntry.getEndedAt())) {
				errors.rejectValue("endedAt", "queueEntry.endedAt.invalid",
				    "Queue entry endedAt should be on or after the startedAt date");
			}
		}
		
		QueueServicesWrapper queueServices = Context.getRegisteredComponent("queue.QueueServicesWrapper",
		    QueueServicesWrapper.class);
		
		if (queueEntry.getStatus() == null) {
			errors.rejectValue("status", "queueEntry.status.null", "The property status should not be null");
		} else if (queue != null) {
			if (!queueServices.getAllowedStatuses(queue).contains(queueEntry.getStatus())) {
				errors.rejectValue("status", "queueEntry.status.invalid",
				    "The property status should be a member of configured queue status conceptSet.");
			}
		}
		
		if (queueEntry.getPriority() == null) {
			errors.rejectValue("priority", "queueEntry.priority.null", "The property priority should not be null");
		} else if (queue != null) {
			if (!queueServices.getAllowedPriorities(queue).contains(queueEntry.getPriority())) {
				errors.rejectValue("priority", "queueEntry.priority.invalid",
				    "The property priority should be a member of configured queue priority conceptSet.");
			}
		}
		
		boolean canCheckDuplicate = queue != null && queueEntry.getPatient() != null && queueEntry.getStartedAt() != null;
		if (canCheckDuplicate && isDuplicate(queueEntry, queueServices.getQueueEntryService())) {
			errors.rejectValue("", "queue.entry.error.duplicate", "This patient is already in this queue");
		}
	}
	
	private boolean isDuplicate(QueueEntry queueEntry, QueueEntryService queueEntryService) {
		List<QueueEntry> queueEntries = queueEntryService.getOverlappingQueueEntries(queueEntry.getPatient(),
		    queueEntry.getQueue(), queueEntry.getStartedAt(), queueEntry.getEndedAt());
		
		// if we aren't checking an existing queue entry, any overlaps are "duplicates"
		if (queueEntry.getId() == null) {
			return !queueEntries.isEmpty();
		}
		
		// if we are checking an existing queue entry then check if there is at least one overlapping entry
		// that is not this entry
		for (QueueEntry qe : queueEntries) {
			if (!qe.getId().equals(queueEntry.getId())) {
				return true;
			}
		}
		return false;
	}
}
