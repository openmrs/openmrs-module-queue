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
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.openmrs.module.queue.api.dao.QueueRoomDao;
import org.openmrs.module.queue.api.search.QueueRoomSearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueRoom;
import org.springframework.beans.factory.annotation.Qualifier;

public class QueueRoomDaoImpl extends AbstractBaseQueueDaoImpl<QueueRoom> implements QueueRoomDao {
	
	public QueueRoomDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
		super(sessionFactory);
	}
	
	@Override
	public List<QueueRoom> getQueueRooms(QueueRoomSearchCriteria searchCriteria) {
		CriteriaBuilder cb = getCurrentSession().getCriteriaBuilder();
		CriteriaQuery<QueueRoom> query = cb.createQuery(QueueRoom.class);
		Root<QueueRoom> qr = query.from(QueueRoom.class);
		Join<QueueRoom, Queue> q = qr.join("queue");
		List<Predicate> predicates = new ArrayList<>();
		includeVoidedObjects(cb, predicates, qr, searchCriteria.isIncludeRetired());
		limitByCollectionProperty(predicates, qr.get("queue"), searchCriteria.getQueues());
		limitByCollectionProperty(predicates, q.get("location"), searchCriteria.getLocations());
		limitByCollectionProperty(predicates, q.get("service"), searchCriteria.getServices());
		query.where(predicates.toArray(new Predicate[0]));
		return getCurrentSession().createQuery(query).list();
	}
	
}
