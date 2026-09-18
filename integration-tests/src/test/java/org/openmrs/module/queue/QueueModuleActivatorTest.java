/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.tasks.AutoCloseQueueEntryTask;
import org.openmrs.module.queue.tasks.AutoCloseVisitQueueEntryTask;
import org.openmrs.scheduler.SchedulerException;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * The scheduled tasks are registered at module startup and instantiated by the scheduler from their
 * class name, which no other test covers.
 */
@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class QueueModuleActivatorTest extends BaseModuleContextSensitiveTest {
	
	private static final String AUTO_CLOSE_VISIT_QUEUE_ENTRY_TASK = "Queue Module - Auto Close Visit Queue Entries";
	
	private static final String AUTO_CLOSE_QUEUE_ENTRY_TASK = "Queue Module - Auto Close Queue Entries";
	
	@Before
	public void setup() {
		new QueueModuleActivator().started();
	}
	
	@Test
	public void shouldRegisterAndStartBothTasks() {
		assertThat(taskDefinition(AUTO_CLOSE_VISIT_QUEUE_ENTRY_TASK).getTaskClass(),
		    equalTo(AutoCloseVisitQueueEntryTask.class.getName()));
		assertThat(taskDefinition(AUTO_CLOSE_QUEUE_ENTRY_TASK).getTaskClass(),
		    equalTo(AutoCloseQueueEntryTask.class.getName()));
		assertThat(taskDefinition(AUTO_CLOSE_QUEUE_ENTRY_TASK).getStarted(), equalTo(true));
		assertThat(taskDefinition(AUTO_CLOSE_QUEUE_ENTRY_TASK).getRepeatInterval(), equalTo(60L));
		assertThat(taskDefinition(AUTO_CLOSE_QUEUE_ENTRY_TASK).getStartOnStartup(), equalTo(true));
	}
	
	@Test
	public void shouldNotRegisterATaskThatIsRegisteredAlready() {
		Integer id = taskDefinition(AUTO_CLOSE_QUEUE_ENTRY_TASK).getId();
		
		new QueueModuleActivator().started();
		
		assertThat(taskDefinition(AUTO_CLOSE_QUEUE_ENTRY_TASK).getId(), equalTo(id));
	}
	
	@Test
	public void shouldNotRestartATaskThatHasBeenStopped() throws SchedulerException {
		Context.getSchedulerService().shutdownTask(taskDefinition(AUTO_CLOSE_QUEUE_ENTRY_TASK));
		
		new QueueModuleActivator().started();
		
		assertThat(taskDefinition(AUTO_CLOSE_QUEUE_ENTRY_TASK).getStarted(), equalTo(false));
	}
	
	private TaskDefinition taskDefinition(String name) {
		return Context.getSchedulerService().getTaskByName(name);
	}
}
