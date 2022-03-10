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

import static org.hibernate.criterion.Restrictions.and;

import java.util.Collection;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Subqueries;
import org.openmrs.api.ConceptNameType;
import org.openmrs.module.queue.api.dao.VisitQueueEntryDao;
import org.openmrs.module.queue.model.VisitQueueEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Slf4j
@SuppressWarnings("unchecked")
@Repository("queue.VisitQueueEntryDao")
public class VisitQueueEntryDaoImpl extends AbstractBaseQueueDaoImpl<VisitQueueEntry> implements VisitQueueEntryDao<VisitQueueEntry> {
	
	@Autowired
	public VisitQueueEntryDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
		super(sessionFactory);
	}
	
	/**
	 * @see VisitQueueEntryDao#findVisitQueueEntriesByConceptStatusAndConceptService(String, String,
	 *      ConceptNameType, boolean)
	 */
	@Override
	public Collection<VisitQueueEntry> findVisitQueueEntriesByConceptStatusAndConceptService(String conceptStatus,
	        String conceptService, ConceptNameType conceptNameType, boolean localePreferred) {
		Criteria criteriaVisitQueueEntries = getCurrentSession().createCriteria(VisitQueueEntry.class, "_vqe");
		includeVoidedObjects(criteriaVisitQueueEntries, false);
		Criteria criteriaQueueEntries = criteriaVisitQueueEntries.createCriteria("_vqe.queueEntry", "_qe");
		Criteria criteriaQueue = criteriaQueueEntries.createCriteria("_qe.queue", "_q");
		
		if (conceptStatus != null && conceptService != null) {
			criteriaQueue.add(and(
			    Subqueries.propertiesIn(new String[] { "_qe.status" },
			        conceptByNameDetachedCriteria(conceptStatus, localePreferred, conceptNameType)),
			    Subqueries.propertiesIn(new String[] { "_q.service" },
			        conceptByNameDetachedCriteria(conceptService, localePreferred, conceptNameType))));
		} else if (conceptStatus != null) {
			criteriaQueue.add(Subqueries.propertiesIn(new String[] { "_qe.status" },
			    conceptByNameDetachedCriteria(conceptStatus, localePreferred, conceptNameType)));
		} else if (conceptService != null) {
			criteriaQueue.add(Subqueries.propertiesIn(new String[] { "_q.service" },
			    conceptByNameDetachedCriteria(conceptService, localePreferred, conceptNameType)));
		}
		
		return criteriaQueue.list();
	}
}
