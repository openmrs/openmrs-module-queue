/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api;

import java.lang.reflect.Method;
import java.util.List;

import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.MethodBeforeAdvice;

/**
 * AOP advice that intercepts {@link org.openmrs.api.VisitService} methods to manage associated
 * queue entries when a visit is voided or purged. For voiding, this ensures queue entries are
 * voided alongside the visit. For purging, this removes queue entries before the visit is
 * permanently deleted to prevent orphaned entries and foreign key constraint violations.
 */
public class VisitWithQueueEntriesDeleteAdvice implements MethodBeforeAdvice {
	
	private static final Logger log = LoggerFactory.getLogger(VisitWithQueueEntriesDeleteAdvice.class);
	
	@Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
		if (args.length == 0 || !(args[0] instanceof Visit)) {
			return;
		}
		Visit visit = (Visit) args[0];
		if (visit.getVisitId() == null) {
			return;
		}
		
		if ("voidVisit".equals(method.getName())) {
			String voidReason = args.length > 1 && args[1] instanceof String ? (String) args[1] : null;
			QueueEntryService queueEntryService = Context.getService(QueueEntryService.class);
			QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
			criteria.setVisit(visit);
			List<QueueEntry> queueEntries = queueEntryService.getQueueEntries(criteria);
			if (!queueEntries.isEmpty()) {
				log.debug("Voiding " + queueEntries.size() + " queue entries associated with voided visit");
			}
			for (QueueEntry qe : queueEntries) {
				if (!qe.getVoided()) {
					queueEntryService.voidQueueEntry(qe, voidReason);
					log.trace("Voided queue entry " + qe);
				}
			}
		} else if ("purgeVisit".equals(method.getName())) {
			QueueEntryService queueEntryService = Context.getService(QueueEntryService.class);
			QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
			criteria.setVisit(visit);
			criteria.setIncludedVoided(true);
			List<QueueEntry> queueEntries = queueEntryService.getQueueEntries(criteria);
			if (!queueEntries.isEmpty()) {
				log.debug("Purging " + queueEntries.size() + " queue entries associated with purged visit");
			}
			for (QueueEntry qe : queueEntries) {
				queueEntryService.purgeQueueEntry(qe);
				log.trace("Purged queue entry " + qe);
			}
		}
	}
}
