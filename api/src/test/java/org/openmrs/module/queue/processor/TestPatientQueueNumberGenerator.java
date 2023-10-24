/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.processor;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.module.queue.model.VisitQueueEntry;
import org.springframework.stereotype.Component;

/**
 * Test class that allows delegating to a particular generator for a specific thread
 */
@Slf4j
@Component
public class TestPatientQueueNumberGenerator implements VisitQueueEntryProcessor {
	
	ThreadLocal<VisitQueueEntryProcessor> processorToUse = new ThreadLocal<>();
	
	/**
	 * This populates the given VisitQueueEntry with an appropriate patient queue number
	 */
	public void beforeSaveVisitQueueEntry(VisitQueueEntry visitQueueEntry) {
		VisitQueueEntryProcessor processor = processorToUse.get();
		if (processor != null) {
			processor.beforeSaveVisitQueueEntry(visitQueueEntry);
		}
	}
	
	public void setProcessorToUse(VisitQueueEntryProcessor visitQueueEntryProcessor) {
		processorToUse.set(visitQueueEntryProcessor);
	}
}
