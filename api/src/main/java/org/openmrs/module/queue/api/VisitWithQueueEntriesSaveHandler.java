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

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.api.handler.SaveHandler;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.utils.PrivilegeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Ends a visit's open queue entries when the visit is stopped. Voiding is owned by
 * {@link VisitWithQueueEntriesVoidHandler}, which core reaches on the {@code voidVisit} path, and
 * purging by {@link VisitWithQueueEntriesDeleteAdvice}.
 */
@Handler(supports = Visit.class)
public class VisitWithQueueEntriesSaveHandler implements SaveHandler<Visit> {
	
	private final Log log = LogFactory.getLog(getClass());
	
	private final QueueEntryService queueEntryService;
	
	@Autowired
	public VisitWithQueueEntriesSaveHandler(@Qualifier("queue.QueueEntryService") QueueEntryService queueEntryService) {
		this.queueEntryService = queueEntryService;
	}
	
	@Override
	public void handle(Visit visit, User user, Date date, String s) {
		// Stopping a visit is driven by core services whose callers need not hold queue privileges,
		// so grant them for the duration of this cascade, as core's PatientDataVoidHandler does
		Context.addProxyPrivilege(PrivilegeConstants.GET_QUEUE_ENTRIES);
		Context.addProxyPrivilege(PrivilegeConstants.MANAGE_QUEUE_ENTRIES);
		try {
			if (visit.getVisitId() != null && visit.getStopDatetime() != null) {
				QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
				criteria.setVisit(visit);
				criteria.setIsEnded(false);
				List<QueueEntry> queueEntries = queueEntryService.getQueueEntries(criteria);
				if (!queueEntries.isEmpty()) {
					log.debug("Closing " + +queueEntries.size() + " queue entries associated with stopped visit");
				}
				for (QueueEntry qe : queueEntries) {
					qe.setEndedAt(visit.getStopDatetime());
					queueEntryService.saveQueueEntry(qe);
					log.trace("Closed queue entry " + qe + " on " + visit.getStopDatetime());
				}
			}
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.GET_QUEUE_ENTRIES);
			Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_QUEUE_ENTRIES);
		}
	}
}
