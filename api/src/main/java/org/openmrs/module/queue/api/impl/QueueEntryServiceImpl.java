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
import java.util.Arrays;
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
import org.openmrs.api.AdministrationService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.api.sort.SortWeightGenerator;
import org.openmrs.module.queue.exception.DuplicateQueueEntryException;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.QueueEntryTransition;
import org.openmrs.module.queue.utils.QueueUtils;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
@Setter(AccessLevel.MODULE)
public class QueueEntryServiceImpl extends BaseOpenmrsService implements QueueEntryService {
	
	private QueueEntryDao<QueueEntry> dao;
	
	private VisitService visitService;
	
	private AdministrationService administrationService;
	
	@Setter
	private SortWeightGenerator sortWeightGenerator = null;
	
	public void setDao(QueueEntryDao<QueueEntry> dao) {
		this.dao = dao;
	}
	
	public void setVisitService(VisitService visitService) {
		this.visitService = visitService;
	}
	
	public void setAdministrationService(AdministrationService administrationService) {
		this.administrationService = administrationService;
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
	 * @see QueueEntryService#saveQueueEntry(org.openmrs.module.queue.model.QueueEntry)
	 */
	@Override
	public QueueEntry saveQueueEntry(QueueEntry queueEntry) {
		if (queueEntry.getVisit() != null) {
			if (!queueEntry.getVisit().getPatient().equals(queueEntry.getPatient())) {
				throw new IllegalArgumentException("Patient mismatch - visit.patient does not match patient");
			}
		}
		QueueEntrySearchCriteria searchCriteria = new QueueEntrySearchCriteria();
		searchCriteria.setPatient(queueEntry.getPatient());
		searchCriteria.setQueues(Collections.singletonList(queueEntry.getQueue()));
		List<QueueEntry> queueEntries = getQueueEntries(searchCriteria);
		for (QueueEntry qe : queueEntries) {
			if (!qe.equals(queueEntry)) {
				if (QueueUtils.datesOverlap(qe.getStartedAt(), qe.getEndedAt(), queueEntry.getStartedAt(),
				    queueEntry.getEndedAt())) {
					throw new DuplicateQueueEntryException("queue.entry.duplicate.patient");
				}
			}
		}
		Double sortWeight = getSortWeightGenerator().generateSortWeight(queueEntry);
		queueEntry.setSortWeight(sortWeight);
		return dao.createOrUpdate(queueEntry);
	}
	
	/**
	 * @see QueueEntryService#transitionQueueEntry(QueueEntryTransition)
	 */
	@Override
	public QueueEntry transitionQueueEntry(QueueEntryTransition queueEntryTransition) {
		// End the initial queue entry
		QueueEntry queueEntryToStop = queueEntryTransition.getQueueEntryToTransition();
		queueEntryToStop.setEndedAt(queueEntryTransition.getTransitionDate());
		getProxiedQueueEntryService().saveQueueEntry(queueEntryToStop);
		// Create a new queue entry
		QueueEntry queueEntryToStart = queueEntryTransition.constructNewQueueEntry();
		return getProxiedQueueEntryService().saveQueueEntry(queueEntryToStart);
	}
	
	/**
	 * @see QueueEntryService#undoTransition(QueueEntry)
	 */
	@Override
	public QueueEntry undoTransition(@NotNull QueueEntry queueEntry) {
		// TODO: Exceptions should be translatable and human readable on the frontend.
		// See: https://openmrs.atlassian.net/browse/O3-2988
		if (queueEntry.getVoided()) {
			throw new IllegalArgumentException("cannot undo transition on a voided queue entry");
		}
		if (queueEntry.getEndedAt() != null) {
			throw new IllegalArgumentException("cannot undo transition on an ended queue entry");
		}
		QueueEntry prevQueueEntry = getPreviousQueueEntry(queueEntry);
		if (prevQueueEntry == null) {
			throw new IllegalArgumentException("specified queue entry does not have a previous queue entry");
		}
		prevQueueEntry.setEndedAt(null);
		prevQueueEntry = dao.createOrUpdate(prevQueueEntry);
		
		queueEntry.setVoided(true);
		queueEntry.setVoidReason("undo transition");
		dao.createOrUpdate(queueEntry);
		return prevQueueEntry;
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
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		criteria.setPatient(queueEntry.getPatient());
		criteria.setVisit(queueEntry.getVisit());
		criteria.setEndedOn(queueEntry.getStartedAt());
		criteria.setQueues(queueComingFrom == null ? Arrays.asList() : Arrays.asList(queueComingFrom));
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
