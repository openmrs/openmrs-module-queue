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
import org.openmrs.api.handler.UnvoidHandler;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.utils.PrivilegeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Restores the queue entries that {@link PatientWithQueueEntriesVoidHandler} voided when a patient
 * is unvoided. Mirrors core's {@code PatientDataUnvoidHandler}: only entries whose void date and
 * voiding user match the patient's original void are unvoided, so entries voided independently stay
 * voided.
 */
@Handler(supports = Patient.class)
public class PatientWithQueueEntriesUnvoidHandler implements UnvoidHandler<Patient> {
	
	private final Log log = LogFactory.getLog(getClass());
	
	private final QueueEntryService queueEntryService;
	
	@Autowired
	public PatientWithQueueEntriesUnvoidHandler(@Qualifier("queue.QueueEntryService") QueueEntryService queueEntryService) {
		this.queueEntryService = queueEntryService;
	}
	
	/**
	 * @param patient the patient being unvoided
	 * @param originalVoidingUser the user who voided the patient
	 * @param originalVoidedDate the date the patient was voided
	 * @param unused not used for unvoiding
	 */
	@Override
	public void handle(Patient patient, User originalVoidingUser, Date originalVoidedDate, String unused) {
		// Voiding or unvoiding is driven by core services whose callers need not hold queue privileges,
		// so grant them for the duration of this cascade, as core's PatientDataVoidHandler does
		Context.addProxyPrivilege(PrivilegeConstants.GET_QUEUE_ENTRIES);
		Context.addProxyPrivilege(PrivilegeConstants.MANAGE_QUEUE_ENTRIES);
		try {
			if (patient.getPatientId() == null || originalVoidedDate == null) {
				return;
			}
			QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
			criteria.setPatient(patient);
			criteria.setIncludedVoided(true);
			List<QueueEntry> queueEntries = queueEntryService.getQueueEntries(criteria);
			int unvoidedCount = 0;
			for (QueueEntry qe : queueEntries) {
				if (shouldRestore(qe, originalVoidingUser, originalVoidedDate)) {
					// unvoid rather than save: saving would run the validator, and an entry that no longer validates
					// (e.g. still open on a visit that was stopped meanwhile) must not block restoring the patient
					queueEntryService.unvoidQueueEntry(qe);
					unvoidedCount++;
					log.trace("Unvoided queue entry " + qe);
				}
			}
			if (unvoidedCount > 0) {
				log.info("Unvoided " + unvoidedCount + " queue entries of patient " + patient.getPatientId());
			}
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.GET_QUEUE_ENTRIES);
			Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_QUEUE_ENTRIES);
		}
	}
	
	/**
	 * @return true if the entry was voided together with the patient and can be restored with it. An
	 *         entry on a voided visit was taken down by VisitWithQueueEntriesSaveHandler, not by this
	 *         cascade; restoring it would put an active entry back on a visit that is still voided.
	 */
	private static boolean shouldRestore(QueueEntry qe, User originalVoidingUser, Date originalVoidedDate) {
		if (!qe.getVoided() || qe.getDateVoided() == null) {
			return false;
		}
		if (qe.getVisit() != null && qe.getVisit().getVoided()) {
			return false;
		}
		if (qe.getDateVoided().getTime() != originalVoidedDate.getTime()) {
			return false;
		}
		return originalVoidingUser == null ? qe.getVoidedBy() == null : originalVoidingUser.equals(qe.getVoidedBy());
	}
}
