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
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
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
		Criteria c = createCriteriaFromSearchCriteria(searchCriteria);
		c.addOrder(Order.desc("qe.sortWeight"));
		c.addOrder(Order.asc("qe.startedAt"));
		c.addOrder(Order.asc("qe.dateCreated"));
		c.addOrder(Order.asc("qe.queueEntryId"));
		return c.list();
	}
	
	@Override
	public Long getCountOfQueueEntries(QueueEntrySearchCriteria searchCriteria) {
		Criteria criteria = createCriteriaFromSearchCriteria(searchCriteria);
		criteria.setProjection(Projections.rowCount());
		return (Long) criteria.uniqueResult();
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
	
	/**
	 * Convert the given {@link QueueEntrySearchCriteria} into ORM criteria
	 */
	private Criteria createCriteriaFromSearchCriteria(QueueEntrySearchCriteria searchCriteria) {
		Criteria c = getCurrentSession().createCriteria(QueueEntry.class, "qe");
		c.createAlias("queue", "q");
		includeVoidedObjects(c, searchCriteria.isIncludedVoided());
		limitByCollectionProperty(c, "queue", searchCriteria.getQueues());
		limitByCollectionProperty(c, "q.location", searchCriteria.getLocations());
		limitByCollectionProperty(c, "q.service", searchCriteria.getServices());
		limitToEqualsProperty(c, "qe.patient", searchCriteria.getPatient());
		limitToEqualsProperty(c, "qe.visit", searchCriteria.getVisit());
		limitByCollectionProperty(c, "qe.priority", searchCriteria.getPriorities());
		limitByCollectionProperty(c, "qe.status", searchCriteria.getStatuses());
		limitByCollectionProperty(c, "qe.locationWaitingFor", searchCriteria.getLocationsWaitingFor());
		limitByCollectionProperty(c, "qe.providerWaitingFor", searchCriteria.getProvidersWaitingFor());
		limitByCollectionProperty(c, "qe.queueComingFrom", searchCriteria.getQueuesComingFrom());
		limitToGreaterThanOrEqualToProperty(c, "qe.startedAt", searchCriteria.getStartedOnOrAfter());
		limitToLessThanOrEqualToProperty(c, "qe.startedAt", searchCriteria.getStartedOnOrBefore());
		limitToEqualsProperty(c, "qe.startedAt", searchCriteria.getStartedOn());
		limitToGreaterThanOrEqualToProperty(c, "qe.endedAt", searchCriteria.getEndedOnOrAfter());
		limitToLessThanOrEqualToProperty(c, "qe.endedAt", searchCriteria.getEndedOnOrBefore());
		limitToEqualsProperty(c, "qe.endedAt", searchCriteria.getEndedOn());
		if (searchCriteria.getHasVisit() == Boolean.TRUE) {
			c.add(Restrictions.isNotNull("qe.visit"));
		} else if (searchCriteria.getHasVisit() == Boolean.FALSE) {
			c.add(Restrictions.isNull("qe.visit"));
		}
		if (searchCriteria.getIsEnded() == Boolean.TRUE) {
			c.add(Restrictions.isNotNull("qe.endedAt"));
		} else if (searchCriteria.getIsEnded() == Boolean.FALSE) {
			c.add(Restrictions.isNull("qe.endedAt"));
		}
		return c;
	}
}
