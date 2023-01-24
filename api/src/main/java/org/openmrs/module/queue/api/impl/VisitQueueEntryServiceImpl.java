/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.impl;

import javax.validation.constraints.NotNull;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.VisitQueueEntryService;
import org.openmrs.module.queue.api.dao.VisitQueueEntryDao;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.VisitQueueEntry;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
@Setter(AccessLevel.MODULE)
public class VisitQueueEntryServiceImpl extends BaseOpenmrsService implements VisitQueueEntryService {
	
	private VisitQueueEntryDao<VisitQueueEntry> dao;
	
	public void setDao(VisitQueueEntryDao<VisitQueueEntry> dao) {
		this.dao = dao;
	}
	
	@Override
	public Optional<VisitQueueEntry> getVisitQueueEntryByUuid(@NotNull String uuid) {
		return this.dao.get(uuid);
	}
	
	@Override
	public VisitQueueEntry createVisitQueueEntry(@NotNull VisitQueueEntry visitQueueEntry) {
		//verify visit -patient & queue entry patient
		//Todo more refactor
		Visit visit = Context.getVisitService().getVisitByUuid(visitQueueEntry.getVisit().getUuid());
		Patient visitPatient = visit.getPatient();
		Patient queueEntryPatient = visitQueueEntry.getQueueEntry().getPatient();
		if (visitPatient != null & queueEntryPatient != null) {
			boolean isPatientSame = visitPatient.getUuid().equals(queueEntryPatient.getUuid());
			if (!isPatientSame) {
				throw new IllegalArgumentException("Patient mismatch - visit.patient does not match queueEntry.patient");
			}
			QueueEntry newlyCreatedQueueEntry = Context.getService(QueueEntryService.class)
			        .createQueueEntry(visitQueueEntry.getQueueEntry());
			visitQueueEntry.setQueueEntry(newlyCreatedQueueEntry);
			return this.dao.createOrUpdate(visitQueueEntry);
		}
		return null;
	}
	
	/**
	 * @see VisitQueueEntryService#findAllVisitQueueEntries()
	 */
	@Override
	public Collection<VisitQueueEntry> findAllVisitQueueEntries() {
		return dao.findAll();
	}
	
	@Override
	public Collection<VisitQueueEntry> getActiveVisitQueueEntries() {
		Collection<VisitQueueEntry> queueEntries = this.dao.findAll(false);
		
		//Remove inactive queue entries
		//queueEntries.removeIf((vqe -> vqe.getQueueEntry().getEndedAt() != null));
		return queueEntries.stream().filter(visitQueueEntry -> visitQueueEntry.getQueueEntry().getEndedAt() == null)
		        .collect(Collectors.toList());
	}
	
	@Override
	public Collection<VisitQueueEntry> findVisitQueueEntries(String status, String service, String locationUuid) {
		//Restrict to fully_specified concept names
		return dao.findVisitQueueEntriesByConceptStatusAndConceptService(status, service, ConceptNameType.FULLY_SPECIFIED,
		    true, locationUuid);
	}
	
	@Override
	public void voidVisitQueueEntry(@NotNull String visitQueueEntryUuid, String voidReason) {
		this.dao.get(visitQueueEntryUuid).ifPresent(visitQueueEntry -> {
			visitQueueEntry.setVoided(true);
			visitQueueEntry.setDateVoided(new Date());
			visitQueueEntry.setVoidReason(voidReason);
			visitQueueEntry.setVoidedBy(Context.getAuthenticatedUser());
			this.dao.createOrUpdate(visitQueueEntry);
		});
	}
	
	@Override
	public void purgeQueueEntry(@NotNull VisitQueueEntry visitQueueEntry) throws APIException {
		this.dao.delete(visitQueueEntry);
	}
	
	/**
	 * @see VisitQueueEntryService#getVisitQueueEntriesCountByStatus(String)
	 */
	@Override
	public Long getVisitQueueEntriesCountByStatus(String status) {
		return dao.getVisitQueueEntriesCountByLocationStatusAndService(status, null, ConceptNameType.FULLY_SPECIFIED, true,
		    null);
	}
	
	/**
	 * @see VisitQueueEntryService#getVisitQueueEntriesCountByService(String)
	 */
	@Override
	public Long getVisitQueueEntriesCountByService(String service) {
		return dao.getVisitQueueEntriesCountByLocationStatusAndService(null, service, ConceptNameType.FULLY_SPECIFIED, true,
		    null);
	}
	
	/**
	 * @see VisitQueueEntryService#getVisitQueueEntriesCountByLocation(String)
	 */
	@Override
	public Long getVisitQueueEntriesCountByLocation(String locaitonUuid) {
		return dao.getVisitQueueEntriesCountByLocationStatusAndService(null, null, null, false, locaitonUuid);
	}
	
	/**
	 * @see VisitQueueEntryService#getVisitQueueEntriesCountByLocationStatusAndService(String, String,
	 *      String)
	 */
	@Override
	public Long getVisitQueueEntriesCountByLocationStatusAndService(String status, String service, String locationUuid) {
		return dao.getVisitQueueEntriesCountByLocationStatusAndService(status, service, ConceptNameType.FULLY_SPECIFIED,
		    true, locationUuid);
	}
}
