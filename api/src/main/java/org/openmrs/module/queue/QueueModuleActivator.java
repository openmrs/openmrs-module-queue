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

import lombok.extern.slf4j.Slf4j;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.DaemonTokenAware;
import org.openmrs.module.queue.tasks.QueueTimerTask;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */
@Slf4j
public class QueueModuleActivator extends BaseModuleActivator implements DaemonTokenAware {
	
	@Override
	public void started() {
		super.started();
		log.info("Queue Module Started");
		QueueTimerTask.setEnabled(true);
	}
	
	@Override
	public void setDaemonToken(DaemonToken daemonToken) {
		QueueTimerTask.setDaemonToken(daemonToken);
	}
}
