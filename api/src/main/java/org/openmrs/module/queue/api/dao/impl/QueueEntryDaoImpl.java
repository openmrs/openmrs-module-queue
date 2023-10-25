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

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.utils.QueueEntrySearchCriteria;
import org.springframework.beans.factory.annotation.Qualifier;

@SuppressWarnings("unchecked")
public class QueueEntryDaoImpl extends AbstractBaseQueueDaoImpl<QueueEntry> implements QueueEntryDao<QueueEntry> {
	
	public QueueEntryDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
		super(sessionFactory);
	}
	
	@Override
	public List<QueueEntry> getQueueEntries(QueueEntrySearchCriteria searchCriteria) {
		Criteria criteria = createCriteriaFromSearchCriteria(searchCriteria);
		return criteria.list();
	}
	
	@Override
	public Long getCountOfQueueEntries(QueueEntrySearchCriteria searchCriteria) {
		Criteria criteria = createCriteriaFromSearchCriteria(searchCriteria);
		criteria.setProjection(Projections.rowCount());
		return (Long) criteria.uniqueResult();
	}
	
	/**
	 * Convert the given {@link QueueEntrySearchCriteria} into ORM criteria
	 */
	private Criteria createCriteriaFromSearchCriteria(QueueEntrySearchCriteria searchCriteria) {
		Criteria c = getCurrentSession().createCriteria(QueueEntry.class, "qe");
		includeVoidedObjects(c, searchCriteria.isIncludedVoided());
		limitByCollectionProperty(c, "qe.queue", searchCriteria.getQueues());
		limitByCollectionProperty(c, "qe.queue.location", searchCriteria.getLocations());
		limitByCollectionProperty(c, "qe.queue.service", searchCriteria.getServices());
		limitToEqualsProperty(c, "qe.patient", searchCriteria.getPatient());
		limitToEqualsProperty(c, "qe.visit", searchCriteria.getVisit());
		limitByCollectionProperty(c, "qe.priority", searchCriteria.getPriorities());
		limitByCollectionProperty(c, "qe.status", searchCriteria.getStatuses());
		limitByCollectionProperty(c, "qe.locationWaitingFor", searchCriteria.getLocationsWaitingFor());
		limitByCollectionProperty(c, "qe.providerWaitingFor", searchCriteria.getProvidersWaitingFor());
		limitByCollectionProperty(c, "qe.queueComingFrom", searchCriteria.getQueuesComingFrom());
		limitToGreaterThanOrEqualToProperty(c, "ge.startedAt", searchCriteria.getStartedOnOrAfter());
		limitToLessThanOrEqualToProperty(c, "ge.startedAt", searchCriteria.getStartedOnOrBefore());
		limitToGreaterThanOrEqualToProperty(c, "ge.endedAt", searchCriteria.getEndedOnOrAfter());
		limitToLessThanOrEqualToProperty(c, "ge.endedAt", searchCriteria.getEndedOnOrBefore());
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
