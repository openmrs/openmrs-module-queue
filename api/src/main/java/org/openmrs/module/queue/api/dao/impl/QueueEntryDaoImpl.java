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

import java.util.Collection;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.openmrs.ConceptName;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.model.QueueEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unchecked")
@Repository("queue.QueueEntryDao")
public class QueueEntryDaoImpl extends AbstractBaseQueueDaoImpl<QueueEntry> implements QueueEntryDao<QueueEntry> {
	
	@Autowired
	public QueueEntryDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
		super(sessionFactory);
	}
	
	/**
	 * @see org.openmrs.module.queue.api.dao.QueueEntryDao#SearchQueueEntries(String, boolean)
	 */
	@Override
	public Collection<QueueEntry> SearchQueueEntries(@NotNull String status, boolean includeVoided) {
		Criteria criteria = getCurrentSession().createCriteria(QueueEntry.class, "qe");
		//Include/exclude retired queues
		includeVoidedObjects(criteria, includeVoided);
		criteria.add(Property.forName("qe.status").in(conceptStatusDetachedCriteria(status)));
		
		return criteria.list();
	}
	
	/**
	 * @see org.openmrs.module.queue.api.dao.QueueEntryDao#getQueueEntriesCountByStatus(String)
	 */
	@Override
	public Long getQueueEntriesCountByStatus(@NotNull String status) {
		Criteria criteria = getCurrentSession().createCriteria(QueueEntry.class, "qe");
		//Include/exclude retired queues
		includeVoidedObjects(criteria, false);
		criteria.add(Property.forName("qe.status").in(conceptStatusDetachedCriteria(status)));
		criteria.setProjection(Projections.rowCount());
		
		return (Long) criteria.uniqueResult();
	}
	
	private DetachedCriteria conceptStatusDetachedCriteria(@NotNull String status) {
		return DetachedCriteria.forClass(ConceptName.class, "cn").add(Restrictions.eq("cn.name", status))
		        .setProjection(Projections.property("cn.concept"));
	}
}
