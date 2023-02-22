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

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.model.QueueEntry;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
@Setter(AccessLevel.MODULE)
public class QueueEntryServiceImpl extends BaseOpenmrsService implements QueueEntryService {
	
	private QueueEntryDao<QueueEntry> dao;
	
	public void setDao(QueueEntryDao<QueueEntry> dao) {
		this.dao = dao;
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueEntryService#getQueueEntryByUuid(String)
	 */
	@Override
	public Optional<QueueEntry> getQueueEntryByUuid(@NotNull String queueEntryUuid) {
		return this.dao.get(queueEntryUuid);
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueEntryService#getQueueEntryById(Integer)
	 */
	@Override
	public Optional<QueueEntry> getQueueEntryById(@NotNull Integer queueEntryId) {
		return this.dao.get(queueEntryId);
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueEntryService#getActiveQueueEntryByPatientUuid(String)
	 */
	@Override
	public Collection<QueueEntry> getActiveQueueEntryByPatientUuid(@NotNull String patientUuid) {
		return this.dao.getActiveQueueEntryByPatientUuid(patientUuid);
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueEntryService#createQueueEntry(org.openmrs.module.queue.model.QueueEntry)
	 */
	@Override
	public QueueEntry createQueueEntry(QueueEntry queueEntry) {
		return this.dao.createOrUpdate(queueEntry);
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueEntryService#voidQueueEntry(String, String)
	 */
	@Override
	public void voidQueueEntry(String queueEntryUuid, String voidReason) {
		this.dao.get(queueEntryUuid).ifPresent(queueEntry -> {
			queueEntry.setVoided(true);
			queueEntry.setVoidReason(voidReason);
			queueEntry.setDateVoided(new Date());
			queueEntry.setVoidedBy(Context.getAuthenticatedUser());
			//Update
			this.dao.createOrUpdate(queueEntry);
		});
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueEntryService#purgeQueueEntry(org.openmrs.module.queue.model.QueueEntry)
	 */
	@Override
	public void purgeQueueEntry(QueueEntry queueEntry) throws APIException {
		this.dao.delete(queueEntry);
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueEntryService#searchQueueEntriesByConceptStatus(String,
	 *      boolean)
	 */
	@Override
	public Collection<QueueEntry> searchQueueEntriesByConceptStatus(String conceptStatus, boolean includeVoided) {
		return this.dao.SearchQueueEntriesByConceptStatus(conceptStatus, ConceptNameType.FULLY_SPECIFIED, false,
		    includeVoided);
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueEntryService#getQueueEntriesCountByStatus(String)
	 */
	@Override
	public Long getQueueEntriesCountByStatus(@NotNull String status) {
		return this.dao.getQueueEntriesCountByConceptStatus(status, ConceptNameType.FULLY_SPECIFIED, false);
	}
}
