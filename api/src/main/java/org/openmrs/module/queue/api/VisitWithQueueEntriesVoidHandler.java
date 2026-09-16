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

import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.api.handler.VoidHandler;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.utils.PrivilegeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Voids all queue entries of a visit when that visit is voided. Core knows nothing about queue
 * entries, so nothing in its void cascade touches them; without this handler a deleted visit would
 * leave its entries active and the patient would stay in the service queue.
 * <p>
 * The void state is read from the handler arguments rather than from {@code visit.getVoided()}, so
 * this does not depend on running after core's {@code BaseVoidHandler}. That matters: both handlers
 * carry the default {@code @Handler} order, {@code HandlerUtil} sorts stably, and the tie is broken
 * by the iteration order of the map {@code ServiceContext.getRegisteredComponents} builds, which
 * moves with the number of {@code VoidHandler} beans a deployment happens to register. Core's own
 * {@code VisitVoidHandler} takes the same approach for a visit's encounters.
 * <p>
 * {@code RequiredDataAdvice} passes every handler the same void date and reason it hands to
 * {@code BaseVoidHandler}, so the entries carry the visit's own void stamp whichever runs first.
 * Entries that were already voided keep the stamp they had, so an unvoid can tell them apart from
 * entries taken down with the visit.
 */
@Handler(supports = Visit.class)
public class VisitWithQueueEntriesVoidHandler implements VoidHandler<Visit> {
	
	private static final Logger log = LoggerFactory.getLogger(VisitWithQueueEntriesVoidHandler.class);
	
	private final QueueEntryService queueEntryService;
	
	@Autowired
	public VisitWithQueueEntriesVoidHandler(@Qualifier("queue.QueueEntryService") QueueEntryService queueEntryService) {
		this.queueEntryService = queueEntryService;
	}
	
	@Override
	public void handle(Visit visit, User voidingUser, Date voidedDate, String voidReason) {
		if (visit.getVisitId() == null) {
			return;
		}
		// Voiding is driven by core services whose callers need not hold queue privileges, so grant
		// them for the duration of this cascade, as core's PatientDataVoidHandler does
		Context.addProxyPrivilege(PrivilegeConstants.GET_QUEUE_ENTRIES);
		Context.addProxyPrivilege(PrivilegeConstants.MANAGE_QUEUE_ENTRIES);
		try {
			QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
			criteria.setVisit(visit);
			// The visit's patient may itself be voided, which would hide its entries from the default search
			criteria.setIncludedVoided(true);
			List<QueueEntry> queueEntries = queueEntryService.getQueueEntries(criteria);
			int voidedCount = 0;
			for (QueueEntry qe : queueEntries) {
				if (qe.getVoided()) {
					continue;
				}
				qe.setVoided(true);
				qe.setVoidReason(voidReason);
				qe.setVoidedBy(voidingUser);
				qe.setDateVoided(voidedDate);
				queueEntryService.saveQueueEntry(qe);
				voidedCount++;
				log.trace("Voided queue entry {} on {}", qe, voidedDate);
			}
			if (voidedCount > 0) {
				log.info("Voided {} queue entries of visit {} with reason: {}", voidedCount, visit.getVisitId(), voidReason);
			}
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.GET_QUEUE_ENTRIES);
			Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_QUEUE_ENTRIES);
		}
	}
}
