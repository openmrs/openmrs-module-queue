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
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.scheduler.tasks.AbstractTask;

/**
 * This iterates over all active VisitQueueEntries If the Visit associated with any of these has
 * ended (non-null stopDatetime), then the QueueEntry associated with it is also ended at the same
 * datetime as the Visit was stopped.
 */
@Slf4j
public class AutoCloseVisitQueueEntryTask extends AbstractTask {
	
	@Override
	public void execute() {
		if (isExecuting) {
			log.debug("AutoCloseVisitQueueEntryTask is still executing, not running again");
			return;
		}
		log.debug("Executing AutoCloseVisitQueueEntryTask");
		startExecuting();
		try {
			List<QueueEntry> queueEntries = getActiveVisitQueueEntries();
			log.debug("There are {} active visit queue entries", queueEntries.size());
			for (QueueEntry queueEntry : queueEntries) {
				try {
					Visit visit = queueEntry.getVisit();
					Date visitStopDatetime = visit.getStopDatetime();
					if (visitStopDatetime != null) {
						log.debug("Visit {} is closed at {}", visit.getVisitId(), visitStopDatetime);
						log.debug("Auto closing queue entry {}", queueEntry.getQueueEntryId());
						if (endQueueEntry(queueEntry, visitStopDatetime)) {
							log.info("Queue entry auto-closed following close of visit: {}", queueEntry.getQueueEntryId());
						} else {
							log.debug("Queue entry {} was left alone by closeQueueEntry", queueEntry.getQueueEntryId());
						}
					}
				}
				catch (Exception e) {
					evictFromSession(queueEntry);
					log.warn("Unable to auto-close queue entry {}", queueEntry.getQueueEntryId(), e);
				}
			}
		}
		finally {
			stopExecuting();
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
	 * @param queueEntry the QueueEntry to end
	 * @param endedAt the time at which to end it
	 * @return true if the queue entry was ended, false if it was ended or otherwise modified since it
	 *         was loaded
	 */
	protected boolean endQueueEntry(QueueEntry queueEntry, Date endedAt) {
		return Context.getService(QueueEntryService.class).closeQueueEntry(queueEntry, endedAt);
	}
	
	/**
	 * @param queueEntry the QueueEntry to evict from the current Hibernate session
	 */
	protected void evictFromSession(QueueEntry queueEntry) {
		Context.evictFromSession(queueEntry);
	}
}
