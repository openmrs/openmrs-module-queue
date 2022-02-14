/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.web.resources.response;

import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openmrs.module.queue.model.Queue;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueueCount implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String uuid;
	
	private Queue queue;
	
	private int count;
	
	public QueueCount(Queue queue, int count) {
		this.uuid = String.valueOf(UUID.randomUUID());
		this.queue = queue;
		this.count = count;
	}
}
