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

import static org.hibernate.criterion.Restrictions.eq;
import static org.hibernate.criterion.Restrictions.or;

import javax.validation.constraints.NotNull;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Location;
import org.openmrs.api.ConceptNameType;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.VisitQueueEntry;
import org.springframework.beans.factory.annotation.Qualifier;

@SuppressWarnings("unchecked")
public class QueueEntryDaoImpl extends AbstractBaseQueueDaoImpl<QueueEntry> implements QueueEntryDao<QueueEntry> {
	
	public QueueEntryDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
		super(sessionFactory);
	}
	
	/**
	 * @see QueueEntryDao#SearchQueueEntriesByConceptStatus(String, ConceptNameType, boolean, boolean)
	 */
	@Override
	public Collection<QueueEntry> SearchQueueEntriesByConceptStatus(@NotNull String status, ConceptNameType conceptNameType,
	        boolean localePreferred, boolean includeVoided) {
		Criteria criteria = getCurrentSession().createCriteria(QueueEntry.class, "qe");
		//Include/exclude retired queues
		includeVoidedObjects(criteria, includeVoided);
		criteria.add(
		    Property.forName("qe.status").in(conceptByNameDetachedCriteria(status, localePreferred, conceptNameType)));
		
		return criteria.list();
	}
	
	/**
	 * @see org.openmrs.module.queue.api.dao.QueueEntryDao#getQueueEntriesCountByConceptStatus(String,
	 *      ConceptNameType, boolean)
	 */
	@Override
	public Long getQueueEntriesCountByConceptStatus(@NotNull String conceptStatus, ConceptNameType conceptNameType,
	        boolean localePreferred) {
		Criteria criteria = getCurrentSession().createCriteria(QueueEntry.class, "qe");
		//Include/exclude retired queues
		includeVoidedObjects(criteria, false);
		criteria.add(Restrictions.and(Restrictions.isNull("qe.endedAt"), Restrictions.isNotNull("qe.startedAt")));
		criteria.add(Property.forName("qe.status")
		        .in(conceptByNameDetachedCriteria(conceptStatus, localePreferred, conceptNameType)));
		criteria.setProjection(Projections.rowCount());
		
		return (Long) criteria.uniqueResult();
	}
	
	@Override
	public String generateVisitQueueNumber(Location location, Queue queue) {
		Criteria criteriaVisitQueueEntries = getCurrentSession().createCriteria(VisitQueueEntry.class, "_vqe");
		includeVoidedObjects(criteriaVisitQueueEntries, false);
		Criteria criteriaQueueEntries = criteriaVisitQueueEntries.createCriteria("_vqe.queueEntry", "_qe");
		Criteria criteriaQueue = criteriaQueueEntries.createCriteria("_qe.queue", "_q");
		Criteria criteriaQueueLocation = criteriaQueue.createCriteria("_q.location", "_ql");
		criteriaQueueLocation.add(eq("_ql.uuid", location.getUuid()));
		criteriaQueueLocation.add(eq("_q.uuid", queue.getUuid()));
		
		List<VisitQueueEntry> queueEntryList = criteriaQueueLocation.list();
		int visitQueueNumber = 0;
		
		if (!queueEntryList.isEmpty()) {
			visitQueueNumber = queueEntryList.size() + 1;
		}
		
		String paddedString = StringUtils.leftPad(String.valueOf(visitQueueNumber), 3, "0");
		
		String serviceName = queue.getName().toUpperCase();
		String prefix = serviceName.length() < 3 ? serviceName : serviceName.substring(0, 3);
		return prefix + "-" + paddedString;
	}
	
	/**
	 * @see QueueEntryDao#getActiveQueueEntries()
	 */
	@Override
	public List<QueueEntry> getActiveQueueEntries() {
		Criteria criteria = getCurrentSession().createCriteria(QueueEntry.class);
		// exclude voided queue entries
		includeVoidedObjects(criteria, false);
		criteria.add(or(Restrictions.isEmpty("endedAt"), Restrictions.isNull("endedAt")));
		return (List<QueueEntry>) criteria.list();
	}
}
