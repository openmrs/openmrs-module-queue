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

import org.hibernate.SessionFactory;
import org.openmrs.module.queue.api.dao.VisitQueueEntryDao;
import org.openmrs.module.queue.model.VisitQueueEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository("queue.VisitQueueEntryDao")
public class VisitQueueEntryDaoImpl extends AbstractBaseQueueDaoImpl<VisitQueueEntry> implements VisitQueueEntryDao<VisitQueueEntry> {
	
	@Autowired
	public VisitQueueEntryDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
		super(sessionFactory);
	}
}
