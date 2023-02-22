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
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.openmrs.api.ConceptNameType;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.model.QueueEntry;
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
	
	/**
	 * @see org.openmrs.module.queue.api.dao.QueueEntryDao#getActiveQueueEntryByPatientUuid(String)
	 */
	@Override
	public Collection<QueueEntry> getActiveQueueEntryByPatientUuid(@NotNull String patientUuid) {
		Criteria criteria = getCurrentSession().createCriteria(QueueEntry.class, "qe");
		//Include/exclude retired queues
		includeVoidedObjects(criteria, false);
		criteria.add(Restrictions.and(Restrictions.isNull("qe.endedAt"), Restrictions.isNotNull("qe.startedAt")));
		criteria.createAlias("patient", "patient");
		criteria.add(Restrictions.eq("patient.uuid", patientUuid));
		
		return criteria.list();
	}
}
