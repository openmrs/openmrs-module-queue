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

import javax.validation.constraints.NotNull;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.springframework.beans.factory.annotation.Qualifier;

public class QueueEntryDaoImpl extends AbstractBaseQueueDaoImpl<QueueEntry> implements QueueEntryDao {
	
	public QueueEntryDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
		super(sessionFactory);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<QueueEntry> getQueueEntries(QueueEntrySearchCriteria searchCriteria) {
		
		Criteria criteria = createCriteriaFromSearchCriteria(searchCriteria);
		
		criteria.addOrder(Order.desc("qe.sortWeight"));
		criteria.addOrder(Order.asc("qe.startedAt"));
		criteria.addOrder(Order.asc("qe.dateCreated"));
		criteria.addOrder(Order.asc("qe.queueEntryId"));
		
		return criteria.list();
	}
	
	@Override
	public Long getCountOfQueueEntries(QueueEntrySearchCriteria searchCriteria) {
		Criteria criteria = createCriteriaFromSearchCriteria(searchCriteria);
		criteria.setProjection(Projections.rowCount());
		return (Long) criteria.uniqueResult();
	}
	
	@Override
	public boolean updateIfUnmodified(QueueEntry entity, Date lastModified) {
		
		QueueEntry existing = getCurrentSession().get(QueueEntry.class, entity.getQueueEntryId());
		
		if (existing == null) {
			return false;
		}
		
		Date existingDate = existing.getDateChanged();
		
		if (existingDate != null && lastModified != null && !existingDate.equals(lastModified)) {
			return false;
		}
		
		getCurrentSession().merge(entity);
		return true;
	}
	
	@Override
	public void flushSession() {
		getCurrentSession().flush();
	}
	
	@Override
	public List<QueueEntry> getOverlappingQueueEntries(@NotNull QueueEntrySearchCriteria searchCriteria) {
		
		String hql = "SELECT qe FROM QueueEntry qe " + "WHERE qe.patient = :patient " + "AND qe.queue IN (:queues) "
		        + "AND qe.voided = false " + "AND qe.endedAt IS NULL";
		
		Query<QueueEntry> query = getCurrentSession().createQuery(hql, QueueEntry.class);
		
		query.setParameter("patient", searchCriteria.getPatient());
		query.setParameterList("queues", searchCriteria.getQueues());
		
		return query.list();
	}
	
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
		limitToGreaterThanOrEqualToProperty(c, "qe.endedAt", searchCriteria.getEndedOnOrAfter());
		limitToLessThanOrEqualToProperty(c, "qe.endedAt", searchCriteria.getEndedOnOrBefore());
		
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
