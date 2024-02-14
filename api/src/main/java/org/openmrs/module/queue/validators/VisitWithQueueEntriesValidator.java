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

import java.util.Date;
import java.util.List;

import org.openmrs.Visit;
import org.openmrs.annotation.Handler;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Handler(supports = { Visit.class }, order = 60)
public class VisitWithQueueEntriesValidator implements Validator {
	
	private final QueueEntryService queueEntryService;
	
	@Autowired
	public VisitWithQueueEntriesValidator(@Qualifier("queue.QueueEntryService") QueueEntryService queueEntryService) {
		this.queueEntryService = queueEntryService;
	}
	
	@Override
	public boolean supports(Class<?> clazz) {
		return Visit.class.isAssignableFrom(clazz);
	}
	
	@Override
	public void validate(Object target, Errors errors) {
		if (!(target instanceof Visit)) {
			throw new IllegalArgumentException("the parameter target must be of type " + Visit.class);
		}
		Visit visit = (Visit) target;
		
		// This implementation is copied to match the core validator approach to nested encounters
		if (visit.getId() != null) {
			Date startDateTime = visit.getStartDatetime();
			Date stopDateTime = visit.getStopDatetime();
			
			QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
			criteria.setIsEnded(null);
			criteria.setVisit(visit);
			List<QueueEntry> queueEntries = queueEntryService.getQueueEntries(criteria);
			for (QueueEntry queueEntry : queueEntries) {
				if (queueEntry.getStartedAt().before(startDateTime)) {
					errors.rejectValue("startDatetime", "queue.entry.error.cannotStartBeforeVisitStartDate",
					    "This visit has queue entries whose dates cannot be before the start date");
					break;
				}
				if (stopDateTime != null) {
					if (queueEntry.getStartedAt().after(stopDateTime)) {
						errors.rejectValue("stopDatetime", "queue.entry.error.cannotStartAfterVisitStopDate",
						    "This visit has queue entries which start after the stop date");
						break;
					}
					if (queueEntry.getEndedAt() != null && queueEntry.getEndedAt().after(stopDateTime)) {
						errors.rejectValue("stopDatetime", "queue.entry.error.cannotEndAfterVisitStopDate",
						    "This visit has queue entries which end after the stop date");
						break;
					}
				}
			}
		}
	}
}
