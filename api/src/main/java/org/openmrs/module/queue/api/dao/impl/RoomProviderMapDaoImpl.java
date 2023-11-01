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
import org.openmrs.module.queue.api.dao.RoomProviderMapDao;
import org.openmrs.module.queue.api.search.RoomProviderMapSearchCriteria;
import org.openmrs.module.queue.model.RoomProviderMap;
import org.springframework.beans.factory.annotation.Qualifier;

public class RoomProviderMapDaoImpl extends AbstractBaseQueueDaoImpl<RoomProviderMap> implements RoomProviderMapDao {
	
	public RoomProviderMapDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
		super(sessionFactory);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<RoomProviderMap> getRoomProviderMaps(RoomProviderMapSearchCriteria searchCriteria) {
		Criteria c = getCurrentSession().createCriteria(RoomProviderMap.class, "rpm");
		includeVoidedObjects(c, searchCriteria.isIncludeVoided());
		limitByCollectionProperty(c, "rpm.queueRoom", searchCriteria.getQueueRooms());
		limitByCollectionProperty(c, "rpm.provider", searchCriteria.getProviders());
		return c.list();
	}
}
