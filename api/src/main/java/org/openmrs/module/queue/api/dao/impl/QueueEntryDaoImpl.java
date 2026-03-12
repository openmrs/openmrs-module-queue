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

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Patient;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.springframework.beans.factory.annotation.Qualifier;

@SuppressWarnings("unchecked")
public class QueueEntryDaoImpl extends AbstractBaseQueueDaoImpl<QueueEntry> implements QueueEntryDao {
	
	public QueueEntryDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
		super(sessionFactory);
	}
	
	@Override
	public List<QueueEntry> getQueueEntries(QueueEntrySearchCriteria searchCriteria) {
		Session session = getSessionFactory().getCurrentSession();
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<QueueEntry> query = cb.createQuery(QueueEntry.class);
		Root<QueueEntry> root = query.from(QueueEntry.class);
		List<Predicate> predicates = buildPredicates(cb, root, searchCriteria);
		query.where(cb.and(predicates.toArray(new Predicate[0])));
		query.orderBy(cb.desc(root.get("sortWeight")), cb.asc(root.get("startedAt")), cb.asc(root.get("dateCreated")),
		    cb.asc(root.get("queueEntryId")));
		return session.createQuery(query).getResultList();
	}
	
	@Override
	public Long getCountOfQueueEntries(QueueEntrySearchCriteria searchCriteria) {
		Session session = getSessionFactory().getCurrentSession();
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Long> query = cb.createQuery(Long.class);
		Root<QueueEntry> root = query.from(QueueEntry.class);
		List<Predicate> predicates = buildPredicates(cb, root, searchCriteria);
		query.select(cb.count(root));
		query.where(cb.and(predicates.toArray(new Predicate[0])));
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
		
		// Evict the entity to prevent Hibernate from auto-flushing changes
		session.evict(queueEntry);
		
		// Build conditional update query - only succeeds if dateChanged matches expected value
		StringBuilder jpql = new StringBuilder();
		jpql.append("UPDATE QueueEntry qe SET ");
		jpql.append("qe.endedAt = :endedAt ");
		jpql.append("WHERE qe.queueEntryId = :id ");
		
		if (expectedDateChanged == null) {
			jpql.append("AND qe.dateChanged IS NULL");
		} else {
			jpql.append("AND qe.dateChanged = :expectedDateChanged");
		}
		
		javax.persistence.Query query = session.createQuery(jpql.toString());
		query.setParameter("endedAt", queueEntry.getEndedAt());
		query.setParameter("id", queueEntry.getQueueEntryId());
		if (expectedDateChanged != null) {
			query.setParameter("expectedDateChanged", expectedDateChanged);
		}
		
		int rowsUpdated = query.executeUpdate();
		return rowsUpdated > 0;
	}
	
	private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<QueueEntry> root,
	        QueueEntrySearchCriteria searchCriteria) {
		List<Predicate> predicates = new ArrayList<>();
		
		if (!searchCriteria.isIncludedVoided()) {
			predicates.add(cb.equal(root.get("voided"), false));
		}
		limitCollection(predicates, root.get("queue"), searchCriteria.getQueues());
		if (searchCriteria.getLocations() != null || searchCriteria.getServices() != null) {
			Join<QueueEntry, Queue> queueJoin = root.join("queue", JoinType.LEFT);
			limitCollection(predicates, queueJoin.get("location"), searchCriteria.getLocations());
			limitCollection(predicates, queueJoin.get("service"), searchCriteria.getServices());
		}
		if (searchCriteria.getPatient() != null) {
			predicates.add(cb.equal(root.get("patient"), searchCriteria.getPatient()));
		}
		if (searchCriteria.getVisit() != null) {
			predicates.add(cb.equal(root.get("visit"), searchCriteria.getVisit()));
		}
		limitCollection(predicates, root.get("priority"), searchCriteria.getPriorities());
		limitCollection(predicates, root.get("status"), searchCriteria.getStatuses());
		limitCollection(predicates, root.get("locationWaitingFor"), searchCriteria.getLocationsWaitingFor());
		limitCollection(predicates, root.get("providerWaitingFor"), searchCriteria.getProvidersWaitingFor());
		limitCollection(predicates, root.get("queueComingFrom"), searchCriteria.getQueuesComingFrom());
		if (searchCriteria.getStartedOnOrAfter() != null) {
			predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), searchCriteria.getStartedOnOrAfter()));
		}
		if (searchCriteria.getStartedOnOrBefore() != null) {
			predicates.add(cb.lessThanOrEqualTo(root.get("startedAt"), searchCriteria.getStartedOnOrBefore()));
		}
		if (searchCriteria.getStartedOn() != null) {
			predicates.add(cb.equal(root.get("startedAt"), searchCriteria.getStartedOn()));
		}
		if (searchCriteria.getEndedOnOrAfter() != null) {
			predicates.add(cb.greaterThanOrEqualTo(root.get("endedAt"), searchCriteria.getEndedOnOrAfter()));
		}
		if (searchCriteria.getEndedOnOrBefore() != null) {
			predicates.add(cb.lessThanOrEqualTo(root.get("endedAt"), searchCriteria.getEndedOnOrBefore()));
		}
		if (searchCriteria.getEndedOn() != null) {
			predicates.add(cb.equal(root.get("endedAt"), searchCriteria.getEndedOn()));
		}
		if (searchCriteria.getHasVisit() == Boolean.TRUE) {
			predicates.add(root.get("visit").isNotNull());
		} else if (searchCriteria.getHasVisit() == Boolean.FALSE) {
			predicates.add(root.get("visit").isNull());
		}
		if (searchCriteria.getIsEnded() == Boolean.TRUE) {
			predicates.add(root.get("endedAt").isNotNull());
		} else if (searchCriteria.getIsEnded() == Boolean.FALSE) {
			predicates.add(root.get("endedAt").isNull());
		}
		
		return predicates;
	}
	
	private <T> void limitCollection(List<Predicate> predicates, Path<T> path, Collection<?> values) {
		if (values != null) {
			if (values.isEmpty()) {
				predicates.add(path.isNull());
			} else {
				predicates.add(path.in(values));
			}
		}
	}
}
