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
import org.openmrs.Person;
import org.openmrs.User;
import org.openmrs.annotation.Handler;
import org.openmrs.api.PatientService;
import org.openmrs.api.handler.SaveHandler;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.person.PersonMergeLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Handler(supports = PersonMergeLog.class)
public class MergePatientsWithQueueEntriesSaveHandler implements SaveHandler<PersonMergeLog> {
	
	private final Log log = LogFactory.getLog(getClass());
	
	private final QueueEntryService queueEntryService;
	
	private final PatientService patientService;
	
	@Autowired
	public MergePatientsWithQueueEntriesSaveHandler(
	    @Qualifier("queue.QueueEntryService") QueueEntryService queueEntryService,
	    @Qualifier("patientService") PatientService patientService) {
		this.queueEntryService = queueEntryService;
		this.patientService = patientService;
	}
	
	@Override
	public void handle(PersonMergeLog mergeLog, User creator, Date dateCreated, String other) {
		Person winner = mergeLog.getWinner();
		Person loser = mergeLog.getLoser();
		Patient winnerPatient = patientService.getPatient(winner.getPersonId());
		Patient loserPatient = patientService.getPatient(loser.getPersonId());
		
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		criteria.setPatient(loserPatient);
		List<QueueEntry> queueEntries = queueEntryService.getQueueEntries(criteria);
		for (QueueEntry qe : queueEntries) {
			qe.setPatient(winnerPatient);
			queueEntryService.saveQueueEntry(qe);
			log.trace("Changed queue entry " + qe.getUuid() + ", setting patient to " + winner.getUuid());
		}
	}
}
