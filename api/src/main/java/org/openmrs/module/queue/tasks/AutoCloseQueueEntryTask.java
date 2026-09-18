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

import static org.openmrs.module.queue.QueueModuleConstants.AUTO_CLOSE_QUEUE_ENTRIES_AT_TIME;
import static org.openmrs.module.queue.QueueModuleConstants.AUTO_CLOSE_QUEUE_ENTRIES_FOR_QUEUES;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.scheduler.tasks.AbstractTask;

/**
 * This ends all active queue entries in the configured queues once per day at a configured time of
 * day. The set of queues to clear and the time to clear them are both controlled by global
 * properties, and nothing is cleared unless a time has been configured. Each run works from the
 * most recent occurrence of that time rather than from the current tick, so patients added since
 * then are left in the queue and a run missed at the configured time is caught up by the next one.
 */
@Slf4j
public class AutoCloseQueueEntryTask extends AbstractTask {
	
	private static final String TIME_FORMAT = "HH:mm";
	
	/**
	 * One task run uses one Hibernate session. Each save checks every entry still in the session for
	 * changes, so a first run over thousands of old entries slows down as it goes. The task empties the
	 * session every FLUSH_BATCH_SIZE entries to keep each check small.
	 */
	private static final int FLUSH_BATCH_SIZE = 250;
	
	@Override
	public void execute() {
		if (isExecuting) {
			log.debug("AutoCloseQueueEntryTask is still executing, not running again");
			return;
		}
		log.debug("Executing AutoCloseQueueEntryTask");
		startExecuting();
		try {
			String configuredTime = getConfiguredCloseTime();
			if (StringUtils.isBlank(configuredTime)) {
				log.debug("No auto-close time configured, not clearing any queue entries");
				return;
			}
			
			Date now = now();
			Date closeTime = getMostRecentCloseTime(configuredTime.trim(), now);
			if (closeTime == null) {
				return;
			}
			
			List<Queue> queues = getQueuesToClear();
			if (queues != null && queues.isEmpty()) {
				log.debug("None of the queues configured for auto-close could be resolved, nothing to do");
				return;
			}
			
			QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
			criteria.setIsEnded(false);
			criteria.setStartedOnOrBefore(closeTime);
			criteria.setQueues(queues);
			
			List<QueueEntry> queueEntries = getQueueEntries(criteria);
			log.debug("There are {} queue entries to auto-close", queueEntries.size());
			int processed = 0;
			for (QueueEntry queueEntry : queueEntries) {
				closeQueueEntry(queueEntry, closeTime);
				if (++processed % FLUSH_BATCH_SIZE == 0) {
					flushAndClearSession();
				}
			}
		}
		catch (Exception e) {
			log.error("AutoCloseQueueEntryTask failed to complete", e);
		}
		finally {
			stopExecuting();
		}
	}
	
	private void closeQueueEntry(QueueEntry queueEntry, Date closeTime) {
		try {
			Date endedAt = closeTime;
			Date startedAt = queueEntry.getStartedAt();
			if (startedAt != null && !endedAt.after(startedAt)) {
				// startedOnOrBefore is inclusive, so an entry started exactly at the close time is swept
				// too, and QueueEntryDaoImpl.updateIfUnmodified rejects an endedAt that is not strictly
				// after startedAt
				endedAt = new Date(startedAt.getTime() + 1000L);
			}
			if (endQueueEntry(queueEntry, endedAt)) {
				log.info("Queue entry auto-closed on schedule: {}", queueEntry.getQueueEntryId());
			} else {
				log.debug("Queue entry {} was left alone by closeQueueEntry", queueEntry.getQueueEntryId());
			}
		}
		catch (Exception e) {
			evictFromSession(queueEntry);
			log.warn("Unable to auto-close queue entry {}", queueEntry.getQueueEntryId(), e);
		}
	}
	
	/**
	 * Parses the configured HH:mm time and returns the most recent instant at which that time of day
	 * occurred: today's occurrence if it has already passed, otherwise yesterday's. Returns null if the
	 * configured value cannot be parsed.
	 */
	protected Date getMostRecentCloseTime(String configuredTime, Date referenceDate) {
		SimpleDateFormat format = new SimpleDateFormat(TIME_FORMAT);
		format.setLenient(false);
		ParsePosition position = new ParsePosition(0);
		Date parsedTime = format.parse(configuredTime, position);
		if (parsedTime == null || position.getIndex() != configuredTime.length()) {
			log.warn("Invalid value '{}' for global property {}, expected format {}", configuredTime,
			    AUTO_CLOSE_QUEUE_ENTRIES_AT_TIME, TIME_FORMAT);
			return null;
		}
		Calendar parsed = Calendar.getInstance();
		parsed.setTime(parsedTime);
		
		Calendar closeTime = Calendar.getInstance();
		closeTime.setTime(referenceDate);
		closeTime.set(Calendar.HOUR_OF_DAY, parsed.get(Calendar.HOUR_OF_DAY));
		closeTime.set(Calendar.MINUTE, parsed.get(Calendar.MINUTE));
		closeTime.set(Calendar.SECOND, 0);
		closeTime.set(Calendar.MILLISECOND, 0);
		if (closeTime.getTime().after(referenceDate)) {
			closeTime.add(Calendar.DATE, -1);
		}
		return closeTime.getTime();
	}
	
	/**
	 * @return the configured time of day (HH:mm) at which to clear queue entries, or blank/null if
	 *         auto-clearing is disabled
	 */
	protected String getConfiguredCloseTime() {
		return getServices().getGlobalProperty(AUTO_CLOSE_QUEUE_ENTRIES_AT_TIME);
	}
	
	/**
	 * @return the queues whose entries should be cleared, or null to clear entries in all queues
	 */
	protected List<Queue> getQueuesToClear() {
		String configuredQueues = getServices().getGlobalProperty(AUTO_CLOSE_QUEUE_ENTRIES_FOR_QUEUES);
		if (StringUtils.isBlank(configuredQueues)) {
			return null;
		}
		List<Queue> queues = new ArrayList<>();
		for (String queueRef : configuredQueues.split(",")) {
			String trimmed = queueRef.trim();
			if (StringUtils.isBlank(trimmed)) {
				continue;
			}
			try {
				queues.add(getServices().getQueue(trimmed));
			}
			catch (IllegalArgumentException e) {
				log.warn("Ignoring unknown queue '{}' configured in global property {}", trimmed,
				    AUTO_CLOSE_QUEUE_ENTRIES_FOR_QUEUES);
			}
		}
		return queues;
	}
	
	/**
	 * @param criteria the criteria identifying the queue entries to end
	 * @return the queue entries matching the given criteria
	 */
	protected List<QueueEntry> getQueueEntries(QueueEntrySearchCriteria criteria) {
		return getServices().getQueueEntryService().getQueueEntries(criteria);
	}
	
	/**
	 * @param queueEntry the QueueEntry to end
	 * @param endedAt the time at which to end it
	 * @return true if the queue entry was ended, false if it was ended or otherwise modified since it
	 *         was loaded
	 */
	protected boolean endQueueEntry(QueueEntry queueEntry, Date endedAt) {
		return getServices().getQueueEntryService().closeQueueEntry(queueEntry, endedAt);
	}
	
	/**
	 * @param queueEntry the QueueEntry to evict from the current Hibernate session
	 */
	protected void evictFromSession(QueueEntry queueEntry) {
		Context.evictFromSession(queueEntry);
	}
	
	/**
	 * Flushes and clears the Hibernate session of the thread running this task
	 */
	protected void flushAndClearSession() {
		Context.flushSession();
		Context.clearSession();
	}
	
	/**
	 * @return the current time; overridable to allow deterministic testing
	 */
	protected Date now() {
		return new Date();
	}
	
	protected QueueServicesWrapper getServices() {
		return Context.getRegisteredComponent("queue.QueueServicesWrapper", QueueServicesWrapper.class);
	}
}
