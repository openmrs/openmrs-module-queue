/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ScheduledExecutorFactoryBean;
import org.springframework.scheduling.concurrent.ScheduledExecutorTask;
import org.springframework.stereotype.Component;

/**
 * Executor that is responsible for scheduling and running the scheduled tasks
 */
@Component
public class QueueTaskExecutor extends ScheduledExecutorFactoryBean {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private static final long ONE_SECOND = 1000;
	
	private static final long ONE_MINUTE = ONE_SECOND * 60;
	
	public QueueTaskExecutor() {
		setScheduledExecutorTasks(task(ONE_MINUTE, ONE_MINUTE, AutoCloseVisitQueueEntryTask.class));
	}
	
	private ScheduledExecutorTask task(long delay, long period, Class<? extends Runnable> runnable) {
		log.info("Scheduling task " + runnable.getSimpleName() + " with delay " + delay + " and period " + period);
		ScheduledExecutorTask task = new ScheduledExecutorTask();
		task.setDelay(delay);
		task.setPeriod(period);
		task.setRunnable(new QueueTimerTask(runnable));
		return task;
	}
}
