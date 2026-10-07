/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.dao.impl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.springframework.beans.factory.annotation.Qualifier;

@SuppressWarnings("unchecked")
public class QueueEntryDaoImpl extends AbstractBaseQueueDaoImpl<QueueEntry> implements QueueEntryDao {
	
	private static final String ENDED_AT = "endedAt";
	
	private static final String DATE_CHANGED = "dateChanged";
	
	private static final String CHANGED_BY = "changedBy";
	
	private static final String QUEUE_ENTRY_ID = "queueEntryId";
	
	public QueueEntryDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
		super(sessionFactory);
	}
	
	@Override
	public List<QueueEntry> getQueueEntries(QueueEntrySearchCriteria searchCriteria) {
		Session session = getSessionFactory().getCurrentSession();
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<QueueEntry> query = cb.createQuery(QueueEntry.class);
		Root<QueueEntry> qe = query.from(QueueEntry.class);
		query.where(buildPredicates(cb, qe, searchCriteria).toArray(new Predicate[0]));
		query.orderBy(cb.desc(qe.get("sortWeight")), cb.asc(qe.get("startedAt")), cb.asc(qe.get("dateCreated")),
		    cb.asc(qe.get("queueEntryId")));
		return session.createQuery(query).list();
	}
	
	@Override
	public Long getCountOfQueueEntries(QueueEntrySearchCriteria searchCriteria) {
		Session session = getSessionFactory().getCurrentSession();
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Long> query = cb.createQuery(Long.class);
		Root<QueueEntry> qe = query.from(QueueEntry.class);
		query.select(cb.count(qe));
		query.where(buildPredicates(cb, qe, searchCriteria).toArray(new Predicate[0]));
		return session.createQuery(query).getSingleResult();
	}
	
	@Override
	public List<QueueEntry> getOverlappingQueueEntries(QueueEntrySearchCriteria searchCriteria) {
		Session session = getSessionFactory().getCurrentSession();
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<QueueEntry> query = cb.createQuery(QueueEntry.class);
		Root<QueueEntry> root = query.from(QueueEntry.class);
		List<Predicate> predicates = new ArrayList<>();
		
		predicates.add(cb.equal(root.get("voided"), false));
		
		Collection<Queue> queues = searchCriteria.getQueues();
		if (queues != null) {
			if (queues.isEmpty()) {
				predicates.add(root.get("queue").isNull());
			} else {
				predicates.add(root.get("queue").in(searchCriteria.getQueues()));
			}
		}
		
		Patient patient = searchCriteria.getPatient();
		if (patient != null) {
			predicates.add(cb.equal(root.get("patient"), patient));
		}
		
		Date startedAt = searchCriteria.getStartedOn();
		if (startedAt != null) {
			// any queue entries that have either not ended or end after this queue entry starts
			predicates.add(cb.or(root.get("endedAt").isNull(), cb.greaterThan(root.get("endedAt"), startedAt)));
		}
		
		Date endedAt = searchCriteria.getEndedOn();
		if (endedAt != null) {
			// any queue entries that started before this queue entry ends
			predicates.add(cb.lessThan(root.get("startedAt"), endedAt));
		}
		
		query.where(cb.and(predicates.toArray(new Predicate[0])));
		
		return session.createQuery(query).list();
	}
	
	@Override
	public void flushSession() {
		getSessionFactory().getCurrentSession().flush();
	}
	
	@Override
	public boolean updateIfUnmodified(QueueEntry queueEntry, Date expectedDateChanged) {
		Session session = getSessionFactory().getCurrentSession();
		
		// This path issues a direct update and bypasses QueueEntryValidator; enforce the
		// strict-positive-duration invariant here so the DB never ends up with ended_at <= started_at.
		Date endedAt = queueEntry.getEndedAt();
		Date startedAt = queueEntry.getStartedAt();
		if (endedAt != null && startedAt != null && !endedAt.after(startedAt)) {
			throw new IllegalArgumentException(
			        "Queue entry endedAt (" + endedAt + ") must be after startedAt (" + startedAt + ")");
		}
		
		// Evict the entity to prevent Hibernate from auto-flushing changes
		session.evict(queueEntry);
		
		// A bulk update also bypasses AuditableInterceptor, so set the audit fields here; without them
		// the dateChanged guard below has nothing to detect a concurrent write with.
		Date dateChanged = new Date();
		User changedBy = Context.getAuthenticatedUser();
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaUpdate<QueueEntry> update = cb.createCriteriaUpdate(QueueEntry.class);
		Root<QueueEntry> root = update.from(QueueEntry.class);
		
		if (endedAt == null) {
			update.set(root.<Date> get(ENDED_AT), cb.nullLiteral(Date.class));
		} else {
			update.set(root.get(ENDED_AT), endedAt);
		}
		update.set(root.get(DATE_CHANGED), dateChanged);
		if (changedBy != null) {
			update.set(root.get(CHANGED_BY), changedBy);
		}
		
		// Only update if dateChanged still matches the value the caller loaded
		List<Predicate> predicates = new ArrayList<>();
		predicates.add(cb.equal(root.get(QUEUE_ENTRY_ID), queueEntry.getQueueEntryId()));
		if (expectedDateChanged == null) {
			predicates.add(root.get(DATE_CHANGED).isNull());
		} else {
			predicates.add(cb.equal(root.get(DATE_CHANGED), expectedDateChanged));
		}
		if (endedAt != null) {
			// Never end an entry another transaction has already ended; re-opening one deliberately
			// targets an ended row, so it is exempt
			predicates.add(root.get(ENDED_AT).isNull());
		}
		update.where(cb.and(predicates.toArray(new Predicate[0])));
		
		return session.createMutationQuery(update).executeUpdate() > 0;
	}
	
	@Override
	public void refresh(QueueEntry queueEntry) {
		Session session = getSessionFactory().getCurrentSession();
		session.refresh(queueEntry);
		// refresh does not cascade to the visit, and its stop date is what closeQueueEntry checks
		if (queueEntry.getVisit() != null) {
			session.refresh(queueEntry.getVisit());
		}
	}
	
	/**
	 * Convert the given {@link QueueEntrySearchCriteria} into ORM predicates. Unless includedVoided is
	 * set, voided entries and entries whose patient is voided are excluded.
	 */
	private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<QueueEntry> qe,
	        QueueEntrySearchCriteria searchCriteria) {
		List<Predicate> predicates = new ArrayList<>();
		Join<QueueEntry, Queue> q = qe.join("queue");
		includeVoidedObjects(cb, predicates, qe, searchCriteria.isIncludedVoided());
		if (!searchCriteria.isIncludedVoided()) {
			Join<QueueEntry, Patient> p = qe.join("patient");
			predicates.add(cb.equal(p.get("voided"), false));
		}
		limitByCollectionProperty(predicates, qe.get("queue"), searchCriteria.getQueues());
		limitByCollectionProperty(predicates, q.get("location"), searchCriteria.getLocations());
		limitByCollectionProperty(predicates, q.get("service"), searchCriteria.getServices());
		limitToEqualsProperty(cb, predicates, qe.get("patient"), searchCriteria.getPatient());
		limitToEqualsProperty(cb, predicates, qe.get("visit"), searchCriteria.getVisit());
		limitByCollectionProperty(predicates, qe.get("priority"), searchCriteria.getPriorities());
		limitByCollectionProperty(predicates, qe.get("status"), searchCriteria.getStatuses());
		limitByCollectionProperty(predicates, qe.get("locationWaitingFor"), searchCriteria.getLocationsWaitingFor());
		limitByCollectionProperty(predicates, qe.get("providerWaitingFor"), searchCriteria.getProvidersWaitingFor());
		limitByCollectionProperty(predicates, qe.get("queueComingFrom"), searchCriteria.getQueuesComingFrom());
		limitToGreaterThanOrEqualToProperty(cb, predicates, qe.<Date> get("startedAt"),
		    searchCriteria.getStartedOnOrAfter());
		limitToLessThanOrEqualToProperty(cb, predicates, qe.<Date> get("startedAt"), searchCriteria.getStartedOnOrBefore());
		limitToEqualsProperty(cb, predicates, qe.get("startedAt"), searchCriteria.getStartedOn());
		limitToGreaterThanOrEqualToProperty(cb, predicates, qe.<Date> get("endedAt"), searchCriteria.getEndedOnOrAfter());
		limitToLessThanOrEqualToProperty(cb, predicates, qe.<Date> get("endedAt"), searchCriteria.getEndedOnOrBefore());
		limitToEqualsProperty(cb, predicates, qe.get("endedAt"), searchCriteria.getEndedOn());
		if (searchCriteria.getHasVisit() == Boolean.TRUE) {
			predicates.add(qe.get("visit").isNotNull());
		} else if (searchCriteria.getHasVisit() == Boolean.FALSE) {
			predicates.add(qe.get("visit").isNull());
		}
		if (searchCriteria.getIsEnded() == Boolean.TRUE) {
			predicates.add(qe.get("endedAt").isNotNull());
		} else if (searchCriteria.getIsEnded() == Boolean.FALSE) {
			predicates.add(qe.get("endedAt").isNull());
		}
		return predicates;
	}
}
