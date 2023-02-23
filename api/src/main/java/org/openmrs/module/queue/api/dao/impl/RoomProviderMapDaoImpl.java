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
import org.openmrs.Provider;
import org.openmrs.api.APIException;
import org.openmrs.module.queue.api.dao.RoomProviderMapDao;
import org.openmrs.module.queue.model.QueueRoom;
import org.openmrs.module.queue.model.RoomProviderMap;
import org.springframework.beans.factory.annotation.Qualifier;

public class RoomProviderMapDaoImpl extends AbstractBaseQueueDaoImpl<RoomProviderMap> implements RoomProviderMapDao {
	
	public RoomProviderMapDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
		super(sessionFactory);
	}
	
	@Override
	public List<RoomProviderMap> getRoomProvider(Provider provider, QueueRoom queueRoom) {
		if (provider == null && queueRoom == null) {
			throw new APIException("Both QueueRoom and Provider cannot be null");
		}
		String stringQuery = "Select roomProviderMap from RoomProviderMap as roomProviderMap WHERE voided = 0 ";
		if (provider != null) {
			stringQuery += "AND roomProviderMap.provider = :provider ";
		}
		if (queueRoom != null) {
			stringQuery += "AND roomProviderMap.queueRoom = :queueRoom ";
		}
		
		Query query = super.getSessionFactory().getCurrentSession().createQuery(stringQuery);
		if (provider != null) {
			query.setParameter("provider", provider);
		}
		if (queueRoom != null) {
			query.setParameter("queueRoom", queueRoom);
		}
		
		return query.list();
	}
}
