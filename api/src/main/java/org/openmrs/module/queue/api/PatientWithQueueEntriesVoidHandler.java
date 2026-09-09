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
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.api.handler.VoidHandler;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.utils.PrivilegeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Voids all queue entries belonging to a patient when that patient is voided. Core knows nothing
 * about queue entries, so nothing in its void cascade touches them; without this handler a voided
 * patient's queue entries would remain active and keep appearing in service queues.
 * <p>
 * Entries are stamped with the patient's void date and user so that
 * {@link PatientWithQueueEntriesUnvoidHandler} can restore exactly these entries if the patient is
 * unvoided. This also fires when patients are merged, since core voids the non-preferred patient
 * after moving its visits to the preferred one; entries on visits that are still open are voided
 * with the merge reason. A merge can still fail before reaching this handler if a queue entry is
 * left open on a visit that has already been stopped, because moving that visit re-validates the
 * entry against the wrong patient; that pre-existing gap is not covered here.
 */
@Handler(supports = Patient.class)
public class PatientWithQueueEntriesVoidHandler implements VoidHandler<Patient> {
	
	private final Log log = LogFactory.getLog(getClass());
	
	private final QueueEntryService queueEntryService;
	
	@Autowired
	public PatientWithQueueEntriesVoidHandler(@Qualifier("queue.QueueEntryService") QueueEntryService queueEntryService) {
		this.queueEntryService = queueEntryService;
	}
	
	@Override
	public void handle(Patient patient, User voidingUser, Date voidedDate, String voidReason) {
		// Voiding or unvoiding is driven by core services whose callers need not hold queue privileges,
		// so grant them for the duration of this cascade, as core's PatientDataVoidHandler does
		Context.addProxyPrivilege(PrivilegeConstants.GET_QUEUE_ENTRIES);
		Context.addProxyPrivilege(PrivilegeConstants.MANAGE_QUEUE_ENTRIES);
		try {
			if (patient.getPatientId() == null) {
				return;
			}
			QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
			criteria.setPatient(patient);
			// By the time this runs, the patient may already be flagged as voided in the session, and the
			// default search hides entries of voided patients (see QueueEntryDaoImpl), so include voided
			// rows in the search and skip the ones that are already voided
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
				log.trace("Voided queue entry " + qe + " on " + voidedDate);
			}
			if (voidedCount > 0) {
				log.info("Voided " + voidedCount + " queue entries of patient " + patient.getPatientId() + " with reason: "
				        + voidReason);
			}
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.GET_QUEUE_ENTRIES);
			Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_QUEUE_ENTRIES);
		}
	}
}
