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

import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.Visit;
import org.openmrs.api.ValidationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;

/**
 * This iterates over all active VisitQueueEntries If the Visit associated with any of these has
 * ended (non-null stopDatetime), then the QueueEntry associated with it is also ended at the same
 * datetime as the Visit was stopped.
 */
@Slf4j
public class AutoCloseVisitQueueEntryTask implements Runnable {
	
	private static volatile boolean currentlyExecuting = false;
	
	@Override
	public void run() {
		if (currentlyExecuting) {
			log.debug(getClass() + " is still executing, not running again");
			return;
		}
		log.debug("Executing: " + getClass());
		try {
			currentlyExecuting = true;
			List<QueueEntry> queueEntries = getActiveVisitQueueEntries();
			log.debug("There are " + queueEntries.size() + " active visit queue entries");
			for (QueueEntry queueEntry : queueEntries) {
				try {
					Visit visit = queueEntry.getVisit();
					Date visitStopDatetime = visit.getStopDatetime();
					if (visitStopDatetime != null) {
						log.debug("Visit " + visit.getVisitId() + " is closed at " + visitStopDatetime);
						log.debug("Auto closing queue entry " + queueEntry.getQueueEntryId());
						queueEntry.setEndedAt(visitStopDatetime);
						saveQueueEntry(queueEntry);
						log.info("Queue entry auto-closed following close of visit: " + queueEntry.getQueueEntryId());
					}
				}
				catch (ValidationException ve) {
					log.warn("Unable to auto-close queue entry " + queueEntry.getQueueEntryId() + ": " + ve.getMessage());
				}
				catch (Exception e) {
					log.warn("Unable to auto-close queue entry " + queueEntry.getQueueEntryId(), e);
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
	protected List<QueueEntry> getActiveVisitQueueEntries() {
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		criteria.setIsEnded(false);
		criteria.setHasVisit(true);
		return Context.getService(QueueEntryService.class).getQueueEntries(criteria);
	}
	
	/**
	 * @param queueEntry the QueueEntry to save
     */
	protected void saveQueueEntry(QueueEntry queueEntry) {
		Context.getService(QueueEntryService.class).saveQueueEntry(queueEntry);
	}
}
