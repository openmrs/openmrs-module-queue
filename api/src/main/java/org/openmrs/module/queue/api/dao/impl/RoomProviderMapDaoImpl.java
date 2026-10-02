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
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;

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
	public List<RoomProviderMap> getRoomProviderMaps(RoomProviderMapSearchCriteria searchCriteria) {
		CriteriaBuilder cb = getCurrentSession().getCriteriaBuilder();
		CriteriaQuery<RoomProviderMap> query = cb.createQuery(RoomProviderMap.class);
		Root<RoomProviderMap> rpm = query.from(RoomProviderMap.class);
		List<Predicate> predicates = new ArrayList<>();
		includeVoidedObjects(cb, predicates, rpm, searchCriteria.isIncludeVoided());
		limitByCollectionProperty(predicates, rpm.get("queueRoom"), searchCriteria.getQueueRooms());
		limitByCollectionProperty(predicates, rpm.get("provider"), searchCriteria.getProviders());
		query.where(predicates.toArray(new Predicate[0]));
		return getCurrentSession().createQuery(query).list();
	}
}
