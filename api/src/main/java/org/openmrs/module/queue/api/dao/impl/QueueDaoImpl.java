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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Concept;
import org.openmrs.api.APIException;
import org.openmrs.module.queue.api.dao.QueueDao;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.springframework.beans.factory.annotation.Qualifier;

@SuppressWarnings("unchecked")
public class QueueDaoImpl extends AbstractBaseQueueDaoImpl<Queue> implements QueueDao<Queue> {
	
	public QueueDaoImpl(@Qualifier(value = "sessionFactory") SessionFactory sessionFactory) {
		super(sessionFactory);
	}
	
	@Override
	public List<Queue> getAllQueuesByLocation(@NotNull String locationUuid) {
		return this.getAllQueuesByLocation(locationUuid, false);
	}
	
	@Override
	public List<Queue> getAllQueuesByLocation(@NotNull String locationUuid, boolean includeVoided) {
		Criteria criteria = getCurrentSession().createCriteria(Queue.class);
		//Include/exclude retired queues
		includeVoidedObjects(criteria, includeVoided);
		Criteria locationCriteria = criteria.createCriteria("location", "ql");
		locationCriteria.add(Restrictions.eq("ql.uuid", locationUuid));
		return (List<Queue>) locationCriteria.list();
	}
	
	@Override
	public Double getQueueAverageWaitTime(@NotNull Queue queue, Concept status, LocalDate today) {
		if (queue == null) {
			throw new APIException("Queue cannot be null");
		}
		LocalDate minDate = LocalDate.now();
		LocalDate maxDate = LocalDate.now().plusDays(1);
		
		Date date1 = Date.from(minDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
		Date date2 = Date.from(maxDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
		
		Criteria criteria = getCurrentSession().createCriteria(QueueEntry.class, "qe");
		handleVoidable(criteria);
		
		Conjunction queueEntryStartedAtCheck = Restrictions.conjunction();
		queueEntryStartedAtCheck.add(Restrictions.ge("startedAt", date1));
		queueEntryStartedAtCheck.add(Restrictions.lt("startedAt", date2));
		
		Conjunction queueEntryEndedAtCheck = Restrictions.conjunction();
		queueEntryEndedAtCheck.add(Restrictions.ge("endedAt", date1));
		queueEntryEndedAtCheck.add(Restrictions.lt("endedAt", date2));
		
		criteria.add(Restrictions.and(queueEntryStartedAtCheck, queueEntryEndedAtCheck, Restrictions.eq("queue", queue)));
		
		if (status != null) {
			criteria.add(Restrictions.and(Restrictions.eq("status", status)));
		}
		List<QueueEntry> queuedToday = criteria.list();
		
		Double averageWaitTime = 0.0;
		
		if (!queuedToday.isEmpty()) {
			Double totalWaitTime = 0.0;
			for (QueueEntry e : queuedToday) {
				LocalDateTime startedAt = e.getStartedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
				LocalDateTime endedAt = e.getEndedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
				
				totalWaitTime += Duration.between(startedAt, endedAt).toMinutes();
				
			}
			return totalWaitTime / (queuedToday.size());
		}
		
		return averageWaitTime;
	}
}
