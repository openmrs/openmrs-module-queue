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

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.openmrs.Location;
import org.openmrs.api.APIException;
import org.openmrs.module.queue.api.dao.QueueRoomDao;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueRoom;
import org.springframework.beans.factory.annotation.Qualifier;

public class QueueRoomDaoImpl extends AbstractBaseQueueDaoImpl<QueueRoom> implements QueueRoomDao {
	
	public QueueRoomDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
		super(sessionFactory);
	}
	
	@Override
	public List<QueueRoom> getQueueRoomsByServiceAndLocation(Queue queue, Location location) {
		if (queue == null && location == null) {
			throw new APIException("Both Queue and Location cannot be null");
		}
		String stringQuery = "Select queueRoom from QueueRoom as queueRoom WHERE retired = 0 ";
		if (queue != null) {
			stringQuery += "AND queueRoom.queue = :queue ";
		}
		if (location != null) {
			stringQuery += "AND queueRoom.queue.location = :location ";
		}
		
		Query query = super.getSessionFactory().getCurrentSession().createQuery(stringQuery);
		if (queue != null) {
			query.setParameter("queue", queue);
		}
		if (location != null) {
			query.setParameter("location", location);
		}
		
		return query.list();
	}
	
}
