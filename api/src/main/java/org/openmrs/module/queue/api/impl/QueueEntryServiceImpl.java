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

import static org.openmrs.module.queue.QueueModuleConstants.EXISTING_VALUE_SORT_WEIGHT_GENERATOR;
import static org.openmrs.module.queue.QueueModuleConstants.QUEUE_SORT_WEIGHT_GENERATOR;

import javax.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.VisitAttributeType;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.api.sort.SortWeightGenerator;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.QueueEntryTransition;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
public class QueueEntryServiceImpl extends BaseOpenmrsService implements QueueEntryService {
	
	@Setter
	private QueueEntryDao dao;
	
	@Setter
	private VisitService visitService;
	
	@Setter
	private AdministrationService administrationService;
	
	@Setter
	private SortWeightGenerator sortWeightGenerator = null;
	
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
	
	@Override
	@Transactional(readOnly = true)
	public List<QueueEntry> getOverlappingQueueEntries(Patient patient, Queue queue, Date startedAt, Date endedAt) {
		QueueEntrySearchCriteria criteria = QueueEntrySearchCriteria.builder().queues(Collections.singletonList(queue))
		        .patient(patient).startedOn(startedAt).endedOn(endedAt).build();
		return dao.getOverlappingQueueEntries(criteria);
	}
	
	/**
	 * @see QueueEntryService#saveQueueEntry(org.openmrs.module.queue.model.QueueEntry)
	 */
	@Override
	public QueueEntry saveQueueEntry(QueueEntry queueEntry) {
		Double sortWeight = getSortWeightGenerator().generateSortWeight(queueEntry);
		queueEntry.setSortWeight(sortWeight);
		return dao.createOrUpdate(queueEntry);
	}
	
	/**
	 * @see QueueEntryService#transitionQueueEntry(QueueEntryTransition)
	 */
	@Override
	public QueueEntry transitionQueueEntry(QueueEntryTransition queueEntryTransition) {
		QueueEntry queueEntryToStop = queueEntryTransition.getQueueEntryToTransition();
		
		if (queueEntryToStop.getId() == null) {
			throw new IllegalArgumentException("Cannot transition a queue entry that has not been saved");
		}
		
		// Reload from database to check current state and guard against concurrent modifications
		QueueEntry currentState = dao.get(queueEntryToStop.getId())
		        .orElseThrow(() -> new IllegalArgumentException("Queue entry not found"));
		if (currentState.getVoided()) {
			throw new IllegalStateException("Cannot transition a voided queue entry");
		}
		if (currentState.getEndedAt() != null) {
			throw new IllegalStateException("Cannot transition a queue entry that has already ended");
		}
		
		// Capture the dateChanged for optimistic locking
		Date expectedDateChanged = currentState.getDateChanged();
		
		QueueEntry queueEntryToStart = queueEntryTransition.constructNewQueueEntry();
		
		// Use optimistic locking to end the current entry
		queueEntryToStop.setEndedAt(queueEntryTransition.getTransitionDate());
		boolean updated = dao.updateIfUnmodified(queueEntryToStop, expectedDateChanged);
		if (!updated) {
			throw new IllegalStateException("Queue entry was modified by another transaction");
		}
		
		dao.flushSession();
		
		return getProxiedQueueEntryService().saveQueueEntry(queueEntryToStart);
	}
	
	/**
	 * @see QueueEntryService#undoTransition(QueueEntry)
	 */
	@Override
	public QueueEntry undoTransition(@NotNull QueueEntry queueEntry) {
		// TODO: Exceptions should be translatable and human readable on the frontend.
		// See: https://openmrs.atlassian.net/browse/O3-2988
		if (queueEntry.getId() == null) {
			throw new IllegalArgumentException("Cannot undo transition on a queue entry that has not been saved");
		}
		
		// Reload from database to check current state and guard against concurrent modifications
		QueueEntry currentState = dao.get(queueEntry.getId())
		        .orElseThrow(() -> new IllegalArgumentException("Queue entry not found"));
		if (currentState.getVoided()) {
			throw new IllegalStateException("Cannot undo transition on a voided queue entry");
		}
		if (currentState.getEndedAt() != null) {
			throw new IllegalStateException("Cannot undo transition on an ended queue entry");
		}
		
		QueueEntry prevQueueEntry = getProxiedQueueEntryService().getPreviousQueueEntry(queueEntry);
		if (prevQueueEntry == null) {
			throw new IllegalArgumentException("Specified queue entry does not have a previous queue entry");
		}
		
		// Capture the dateChanged for optimistic locking before re-opening the previous entry
		Date expectedDateChanged = prevQueueEntry.getDateChanged();
		
		// Re-open the previous entry using optimistic locking
		prevQueueEntry.setEndedAt(null);
		boolean updated = dao.updateIfUnmodified(prevQueueEntry, expectedDateChanged);
		if (!updated) {
			throw new IllegalStateException("Previous queue entry was modified by another transaction");
		}
		
		getProxiedQueueEntryService().voidQueueEntry(queueEntry, "Transition undone");
		
		// Reload the previous entry to return the updated state
		return dao.get(prevQueueEntry.getId()).orElse(prevQueueEntry);
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
		criteria.setStartedOnOrBefore(onOrBefore);
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
	
	@Override
	public SortWeightGenerator getSortWeightGenerator() {
		if (sortWeightGenerator == null) {
			String beanName = administrationService.getGlobalProperty(QUEUE_SORT_WEIGHT_GENERATOR);
			if (StringUtils.isBlank(beanName)) {
				beanName = EXISTING_VALUE_SORT_WEIGHT_GENERATOR;
			}
			sortWeightGenerator = Context.getRegisteredComponent(beanName, SortWeightGenerator.class);
		}
		return sortWeightGenerator;
	}
	
	/**
	 * @return the instance of the QueueEntryService from the context. This is needed for
	 *         self-referential access to ensure proxied instance is returned with relevant AOP
	 *         operations. Only for internal use.
	 */
	protected QueueEntryService getProxiedQueueEntryService() {
		return Context.getService(QueueEntryService.class);
	}
	
	private void endQueueEntry(@NotNull QueueEntry queueEntry) {
		queueEntry.setEndedAt(new Date());
		dao.createOrUpdate(queueEntry);
	}
	
	@Override
	@Transactional(readOnly = true)
	public QueueEntry getPreviousQueueEntry(@NotNull QueueEntry queueEntry) {
		Queue queueComingFrom = queueEntry.getQueueComingFrom();
		if (queueComingFrom == null) {
			return null;
		}
		
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		criteria.setPatient(queueEntry.getPatient());
		criteria.setVisit(queueEntry.getVisit());
		criteria.setEndedOn(queueEntry.getStartedAt());
		criteria.setQueues(Collections.singletonList(queueComingFrom));
		
		List<QueueEntry> prevQueueEntries = dao.getQueueEntries(criteria);
		
		if (prevQueueEntries.size() == 1) {
			return prevQueueEntries.get(0);
		} else if (prevQueueEntries.size() > 1) {
			// TODO: Exceptions should be translatable and human readable on the frontend.
			// See: https://openmrs.atlassian.net/browse/O3-2988
			throw new IllegalStateException("Multiple previous queue entries found");
		} else {
			return null;
		}
	}
}
