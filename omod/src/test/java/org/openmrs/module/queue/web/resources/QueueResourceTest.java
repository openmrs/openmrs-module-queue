/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.web.resources;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueService;
import org.openmrs.module.queue.model.Queue;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class QueueResourceTest extends BaseQueueResourceTest<Queue, QueueResource> {
	
	private static final String QUEUE_UUID = "6hje567a-fca0-11e5-9e59-08002719a7";
	
	@Mock
	private QueueService queueService;
	
	private Queue queue;
	
	@Before
	public void setup() {
		queue = new Queue();
		queue.setUuid(QUEUE_UUID);
		
		this.prepareMocks();
		when(Context.getService(QueueService.class)).thenReturn(queueService);
		
		this.setResource(new QueueResource());
		this.setObject(queue);
	}
	
	@Test
	public void shouldGetQueueService() {
		assertThat(queueService, notNullValue());
	}
}
