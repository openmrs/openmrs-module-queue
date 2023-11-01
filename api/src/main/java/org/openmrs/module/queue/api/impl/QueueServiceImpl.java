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
import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Setter;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.queue.api.QueueService;
import org.openmrs.module.queue.api.dao.QueueDao;
import org.openmrs.module.queue.api.search.QueueSearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Setter(AccessLevel.MODULE)
public class QueueServiceImpl extends BaseOpenmrsService implements QueueService {
	
	private QueueDao dao;
	
	public void setDao(QueueDao dao) {
		this.dao = dao;
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueService#getQueueByUuid(String)
	 */
	@Override
	@Transactional(readOnly = true)
	public Optional<Queue> getQueueByUuid(@NotNull String queueUuid) {
		return dao.get(queueUuid);
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueService#getQueueById(Integer)
	 */
	@Override
	@Transactional(readOnly = true)
	public Optional<Queue> getQueueById(@NotNull Integer queueId) {
		return dao.get(queueId);
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueService#saveQueue(Queue)
	 */
	@Override
	public Queue saveQueue(@NotNull Queue queue) {
		return dao.createOrUpdate(queue);
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueService#getQueues(QueueSearchCriteria)
	 */
	@Override
	@Transactional(readOnly = true)
	public List<Queue> getQueues(@NotNull QueueSearchCriteria searchCriteria) {
		return dao.getQueues(searchCriteria);
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueService#getAllQueues()
	 */
	@Override
	@Transactional(readOnly = true)
	public List<Queue> getAllQueues() {
		return dao.findAll();
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueService#retireQueue(Queue, String)
	 */
	@Override
	public void retireQueue(@NotNull Queue queue, String retireReason) {
		queue.setRetired(true);
		queue.setDateRetired(new Date());
		queue.setRetireReason(retireReason);
		queue.setRetiredBy(Context.getAuthenticatedUser());
		dao.createOrUpdate(queue);
	}
	
	/**
	 * @see org.openmrs.module.queue.api.QueueService#purgeQueue(Queue)
	 */
	@Override
	public void purgeQueue(Queue queue) throws APIException {
		this.dao.delete(queue);
	}
}
