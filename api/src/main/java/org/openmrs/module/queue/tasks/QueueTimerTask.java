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

import java.util.TimerTask;

import org.openmrs.api.context.Daemon;
import org.openmrs.module.DaemonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Timer task implementation that utilises a daemon thread to execute a runnable
 */
public class QueueTimerTask extends TimerTask {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private static DaemonToken daemonToken;
	
	private static boolean enabled = false;
	
	private final Class<? extends Runnable> taskClass;
	
	public QueueTimerTask(Class<? extends Runnable> taskClass) {
		this.taskClass = taskClass;
	}
	
	/**
	 * @see TimerTask#run()
	 */
	@Override
	public final void run() {
		if (daemonToken != null && enabled) {
			try {
				log.debug("Running task: " + taskClass.getSimpleName());
				Runnable taskInstance = taskClass.getDeclaredConstructor().newInstance();
				Daemon.runInDaemonThread(taskInstance, daemonToken);
			}
			catch (Exception e) {
				log.error("An error occurred while running scheduled task " + taskClass.getSimpleName(), e);
			}
		} else {
			log.debug("Not running scheduled task. DaemonToken = " + daemonToken + "; enabled = " + enabled);
		}
	}
	
	public static void setEnabled(boolean enabled) {
		QueueTimerTask.enabled = enabled;
	}
	
	public static void setDaemonToken(DaemonToken daemonToken) {
		QueueTimerTask.daemonToken = daemonToken;
	}
}
