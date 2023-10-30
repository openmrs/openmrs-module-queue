/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.dao;

import javax.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

import org.openmrs.Auditable;
import org.openmrs.OpenmrsObject;
import org.springframework.transaction.annotation.Transactional;

public interface BaseQueueDao<Q extends OpenmrsObject & Auditable> {
	
	@Transactional(readOnly = true)
	Optional<Q> get(@NotNull int id);
	
	@Transactional(readOnly = true)
	Optional<Q> get(@NotNull String uuid);
	
	Q createOrUpdate(Q object);
	
	void delete(Q object);
	
	void delete(@NotNull String uuid);
	
	@Transactional(readOnly = true)
	List<Q> findAll();
	
	@Transactional(readOnly = true)
	List<Q> findAll(boolean includeVoided);
}
