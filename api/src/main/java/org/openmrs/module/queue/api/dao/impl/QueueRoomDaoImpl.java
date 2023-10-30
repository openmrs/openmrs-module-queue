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
import org.openmrs.module.queue.api.dao.QueueRoomDao;
import org.openmrs.module.queue.api.search.QueueRoomSearchCriteria;
import org.openmrs.module.queue.model.QueueRoom;
import org.springframework.beans.factory.annotation.Qualifier;

public class QueueRoomDaoImpl extends AbstractBaseQueueDaoImpl<QueueRoom> implements QueueRoomDao {
	
	public QueueRoomDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
		super(sessionFactory);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<QueueRoom> getQueueRooms(QueueRoomSearchCriteria searchCriteria) {
		Criteria c = getCurrentSession().createCriteria(QueueRoom.class, "qr");
		c.createAlias("queue", "q");
		includeVoidedObjects(c, searchCriteria.isIncludeRetired());
		limitByCollectionProperty(c, "qr.queue", searchCriteria.getQueues());
		limitByCollectionProperty(c, "q.location", searchCriteria.getLocations());
		limitByCollectionProperty(c, "q.service", searchCriteria.getServices());
		return c.list();
	}
	
}
