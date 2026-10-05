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

import java.util.Date;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.queue.tasks.AutoCloseQueueEntryTask;
import org.openmrs.module.queue.tasks.AutoCloseVisitQueueEntryTask;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.Task;
import org.openmrs.scheduler.TaskDefinition;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */
@Slf4j
public class QueueModuleActivator extends BaseModuleActivator {
	
	private static final String AUTO_CLOSE_VISIT_QUEUE_ENTRY_TASK = "Queue Module - Auto Close Visit Queue Entries";
	
	private static final String AUTO_CLOSE_QUEUE_ENTRY_TASK = "Queue Module - Auto Close Queue Entries";
	
	private static final long REPEAT_INTERVAL_SECONDS = 60L;
	
	@Override
	public void started() {
		super.started();
		log.info("Queue Module Started");
		registerTask(AutoCloseVisitQueueEntryTask.class, AUTO_CLOSE_VISIT_QUEUE_ENTRY_TASK,
		    "Ends queue entries whose visit has been stopped");
		registerTask(AutoCloseQueueEntryTask.class, AUTO_CLOSE_QUEUE_ENTRY_TASK,
		    "Ends active queue entries at the time of day configured in "
		            + QueueModuleConstants.AUTO_CLOSE_QUEUE_ENTRIES_AT_TIME);
	}
	
	/**
	 * Creates the task definition the first time this module starts, and does nothing thereafter, so
	 * that an interval change or a stop made from the Manage Scheduler page is left alone. The
	 * scheduler starts the task at server startup and restores it across a module being started or
	 * stopped; starting it here covers only the case it cannot, of this module being installed into a
	 * running server.
	 */
	private void registerTask(Class<? extends Task> taskClass, String name, String description) {
		try {
			SchedulerService schedulerService = Context.getSchedulerService();
			if (schedulerService.getTaskByName(name) != null) {
				log.debug("Scheduled task {} is registered already", name);
				return;
			}
			TaskDefinition taskDefinition = new TaskDefinition();
			taskDefinition.setName(name);
			taskDefinition.setDescription(description);
			taskDefinition.setTaskClass(taskClass.getName());
			taskDefinition.setStartTime(new Date());
			taskDefinition.setRepeatInterval(REPEAT_INTERVAL_SECONDS);
			taskDefinition.setStartOnStartup(true);
			schedulerService.saveTaskDefinition(taskDefinition);
			schedulerService.scheduleIfNotRunning(taskDefinition);
			log.info("Registered scheduled task {}", name);
		}
		catch (Exception e) {
			log.error("Unable to register task {}", name, e);
		}
	}
}
