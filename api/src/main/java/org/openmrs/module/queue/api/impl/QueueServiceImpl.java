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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Setter;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.queue.api.QueueService;
import org.openmrs.module.queue.api.dao.QueueDao;
import org.openmrs.module.queue.model.Queue;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Setter(AccessLevel.MODULE)
public class QueueServiceImpl extends BaseOpenmrsService implements QueueService {
	
	private QueueDao<Queue> dao;
	
	public void setDao(QueueDao<Queue> dao) {
		this.dao = dao;
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueService#getQueueByUuid(String)
	 */
	@Override
	@Transactional(readOnly = true)
	public Optional<Queue> getQueueByUuid(@NotNull String queueUuid) {
		return this.dao.get(queueUuid);
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueService#getQueueById(Integer)
	 */
	@Override
	@Transactional(readOnly = true)
	public Optional<Queue> getQueueById(@NotNull Integer queueId) {
		return this.dao.get(queueId);
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueService#createQueue(org.openmrs.module.queue.model.Queue)
	 */
	@Override
	public Queue createQueue(@NotNull Queue queue) {
		return this.dao.createOrUpdate(queue);
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueService#getAllQueuesByLocation(String)
	 */
	@Override
	@Transactional(readOnly = true)
	public List<Queue> getAllQueuesByLocation(@NotNull String locationUuid) {
		return null;
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueService#getAllQueues()
	 */
	@Override
	@Transactional(readOnly = true)
	public Collection<Queue> getAllQueues() {
		return this.dao.findAll();
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueService#voidQueue(String, String)
	 */
	@Override
	public void voidQueue(@NotNull String queueUuid, String voidReason) {
		this.dao.get(queueUuid).ifPresent(queue -> {
			queue.setRetired(true);
			queue.setDateRetired(new Date());
			queue.setRetireReason(voidReason);
			queue.setRetiredBy(Context.getAuthenticatedUser());
			//Effect the change
			this.dao.createOrUpdate(queue);
		});
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueService#purgeQueue(org.openmrs.module.queue.model.Queue)
	 */
	@Override
	public void purgeQueue(Queue queue) throws APIException {
		this.dao.delete(queue);
	}
}
