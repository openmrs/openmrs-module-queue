/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.tasks;

import java.util.Collection;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.VisitQueueEntryService;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.VisitQueueEntry;

/**
 * This iterates over all active VisitQueueEntries If the Visit associated with any of these has
 * ended (non-null stopDatetime), then the QueueEntry associated with it is also ended at the same
 * datetime as the Visit was stopped.
 */
@Slf4j
public class AutoCloseVisitQueueEntryTask implements Runnable {
	
	private static boolean currentlyExecuting = false;
	
	@Override
	public void run() {
		if (currentlyExecuting) {
			log.debug(getClass() + " is still executing, not running again");
			return;
		}
		log.debug("Executing: " + getClass());
		try {
			currentlyExecuting = true;
			Collection<VisitQueueEntry> queueEntries = getActiveVisitQueueEntries();
			log.debug("There are " + queueEntries.size() + " active visit queue entries");
			for (VisitQueueEntry visitQueueEntry : queueEntries) {
				Visit visit = visitQueueEntry.getVisit();
				QueueEntry queueEntry = visitQueueEntry.getQueueEntry();
				if (visit != null) {
					Date visitStopDatetime = visit.getStopDatetime();
					if (visitStopDatetime != null) {
						log.debug("Visit " + visit.getVisitId() + " is closed at " + visitStopDatetime);
						log.debug("Auto closing queue entry " + queueEntry.getQueueEntryId());
						queueEntry.setEndedAt(visitStopDatetime);
						saveQueueEntry(queueEntry);
					}
				}
			}
		}
		finally {
			currentlyExecuting = false;
		}
	}
	
	/**
	 * @return the active VisitQueueEntries
	 */
	Collection<VisitQueueEntry> getActiveVisitQueueEntries() {
		return Context.getService(VisitQueueEntryService.class).getActiveVisitQueueEntries();
	}
	
	/**
	 * @param queueEntry the QueueEntry to save
	 * @return the saved QueueEntry
	 */
	QueueEntry saveQueueEntry(QueueEntry queueEntry) {
		return Context.getService(QueueEntryService.class).createQueueEntry(queueEntry);
	}
}
