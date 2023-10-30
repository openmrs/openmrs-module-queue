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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Location;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.VisitAttributeType;
import org.openmrs.api.APIException;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
@Setter(AccessLevel.MODULE)
public class QueueEntryServiceImpl extends BaseOpenmrsService implements QueueEntryService {
	
	private QueueEntryDao<QueueEntry> dao;
	
	private VisitService visitService;
	
	public void setDao(QueueEntryDao<QueueEntry> dao) {
		this.dao = dao;
	}
	
	public void setVisitService(VisitService visitService) {
		this.visitService = visitService;
	}
	
	/**
	 * @see QueueEntryService#getQueueEntryByUuid(String)
	 */
	@Override
	@Transactional(readOnly = true)
	public Optional<QueueEntry> getQueueEntryByUuid(@NotNull String queueEntryUuid) {
		return dao.get(queueEntryUuid);
	}
	
	/**
	 * @see QueueEntryService#getQueueEntryById(Integer)
	 */
	@Override
	@Transactional(readOnly = true)
	public Optional<QueueEntry> getQueueEntryById(@NotNull Integer queueEntryId) {
		return dao.get(queueEntryId);
	}
	
	/**
	 * @see QueueEntryService#createQueueEntry(org.openmrs.module.queue.model.QueueEntry)
	 */
	@Override
	public QueueEntry createQueueEntry(QueueEntry queueEntry) {
		if (queueEntry.getVisit() != null) {
			if (!queueEntry.getVisit().getPatient().equals(queueEntry.getPatient())) {
				throw new IllegalArgumentException("Patient mismatch - visit.patient does not match patient");
			}
		}
		return dao.createOrUpdate(queueEntry);
	}
	
	/**
	 * @see QueueEntryService#voidQueueEntry(QueueEntry, String)
	 */
	@Override
	public void voidQueueEntry(QueueEntry queueEntry, String voidReason) {
		queueEntry.setVoided(true);
		queueEntry.setVoidReason(voidReason);
		queueEntry.setDateVoided(new Date());
		queueEntry.setVoidedBy(Context.getAuthenticatedUser());
		dao.createOrUpdate(queueEntry);
	}
	
	/**
	 * @see QueueEntryService#purgeQueueEntry(org.openmrs.module.queue.model.QueueEntry)
	 */
	@Override
	public void purgeQueueEntry(QueueEntry queueEntry) throws APIException {
		dao.delete(queueEntry);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<QueueEntry> getQueueEntries(QueueEntrySearchCriteria searchCriteria) {
		return dao.getQueueEntries(searchCriteria);
	}
	
	@Override
	@Transactional(readOnly = true)
	public Long getCountOfQueueEntries(QueueEntrySearchCriteria searchCriteria) {
		return dao.getCountOfQueueEntries(searchCriteria);
	}
	
	@Override
	public String generateVisitQueueNumber(Location location, Queue queue, Visit visit,
	        VisitAttributeType visitAttributeType) {
		if (location == null || queue == null || visit == null || visitAttributeType == null) {
			throw new APIException("Sufficient parameters not supplied for generation of VisitQueueNumber");
		}
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		criteria.setHasVisit(Boolean.TRUE);
		criteria.setQueues(Collections.singletonList(queue));
		criteria.setLocations(Collections.singletonList(location));
		Date onOrAfter = Date.from(LocalDateTime.now().with(LocalTime.MIN).atZone(ZoneId.systemDefault()).toInstant());
		criteria.setStartedOnOrAfter(onOrAfter);
		Date onOrBefore = Date.from(LocalDateTime.now().with(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());
		criteria.setStartedOnOrAfter(onOrBefore);
		Long nextQueueNumber = getCountOfQueueEntries(criteria) + 1;
		String paddedString = StringUtils.leftPad(String.valueOf(nextQueueNumber), 3, "0");
		String serviceName = queue.getName().toUpperCase();
		String prefix = serviceName.length() < 3 ? serviceName : serviceName.substring(0, 3);
		String queueNumber = prefix + "-" + paddedString;
		
		// Create Visit Attribute using generated queue number
		VisitAttribute visitQueueNumber = new VisitAttribute();
		visitQueueNumber.setAttributeType(visitAttributeType);
		visitQueueNumber.setValue(queueNumber);
		visit.setAttribute(visitQueueNumber);
		visitService.saveVisit(visit);
		return queueNumber;
	}
	
	@Override
	public void closeActiveQueueEntries() {
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		criteria.setIsEnded(Boolean.FALSE);
		List<QueueEntry> queueEntries = getQueueEntries(criteria);
		queueEntries.forEach(this::endQueueEntry);
	}
	
	private void endQueueEntry(@NotNull QueueEntry queueEntry) {
		queueEntry.setEndedAt(new Date());
		dao.createOrUpdate(queueEntry);
	}
}
