/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.impl;

import javax.validation.constraints.NotNull;

import java.util.Date;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Setter;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.queue.api.VisitQueueEntryService;
import org.openmrs.module.queue.api.dao.VisitQueueEntryDao;
import org.openmrs.module.queue.model.VisitQueueEntry;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Setter(AccessLevel.MODULE)
public class VisitQueueEntryServiceImpl extends BaseOpenmrsService implements VisitQueueEntryService {
	
	private VisitQueueEntryDao<VisitQueueEntry> dao;
	
	public void setDao(VisitQueueEntryDao<VisitQueueEntry> dao) {
		this.dao = dao;
	}
	
	@Override
	public Optional<VisitQueueEntry> getVisitQueueEntryByUuid(@NotNull String uuid) {
		return this.dao.get(uuid);
	}
	
	@Override
	public VisitQueueEntry createVisitQueueEntry(@NotNull VisitQueueEntry visitQueueEntry) {
		return this.dao.createOrUpdate(visitQueueEntry);
	}
	
	@Override
	public void voidVisitQueueEntry(@NotNull String visitQueueEntryUuid, String voidReason) {
		this.dao.get(visitQueueEntryUuid).ifPresent(visitQueueEntry -> {
			visitQueueEntry.setVoided(true);
			visitQueueEntry.setDateVoided(new Date());
			visitQueueEntry.setVoidReason(voidReason);
			visitQueueEntry.setVoidedBy(Context.getAuthenticatedUser());
			this.dao.createOrUpdate(visitQueueEntry);
		});
	}
}
