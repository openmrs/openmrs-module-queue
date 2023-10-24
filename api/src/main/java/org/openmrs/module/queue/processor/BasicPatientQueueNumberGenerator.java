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
import org.apache.commons.lang.StringUtils;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.VisitQueueEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Basic implementation of a VisitQueueEntryProcessor which generates a patientQueueNumber prior to
 * saving which: 1. If visit queue number is already populated, retain the existing value 2.
 * Otherwise, find the highest numerical value of all currently active visit queue entries and add 1
 * Note: This assumes that the only consumer of this generator is the VisitQueueEntryService, which
 * is synchronized
 */
@Slf4j
@Component
public class BasicPatientQueueNumberGenerator implements VisitQueueEntryProcessor {
	
	@Autowired
	@Qualifier("queue.QueueEntryService")
	QueueEntryService queueEntryService;
	
	/**
	 * This populates the given VisitQueueEntry with an appropriate patient queue number
	 */
	public void beforeSaveVisitQueueEntry(VisitQueueEntry visitQueueEntry) {
		QueueEntry queueEntry = visitQueueEntry.getQueueEntry();
		if (StringUtils.isBlank(queueEntry.getPatientQueueNumber())) {
			queueEntry.setPatientQueueNumber(generateNextQueueNumber());
		}
	}
	
	/**
	 * @return the maximum patient queue number found in active queue entries that can be parsed into an
	 *         int
	 */
	private String generateNextQueueNumber() {
		int queueNum = 0;
		for (QueueEntry qe : queueEntryService.getActiveQueueEntries()) {
			if (qe.getPatientQueueNumber() != null) {
				try {
					int currentNum = Integer.parseInt(qe.getPatientQueueNumber());
					if (currentNum > queueNum) {
						queueNum = currentNum;
					}
				}
				catch (Exception e) {
					log.debug("Unable to parse patient queue number into an int: " + qe.getPatientQueueNumber(), e);
				}
			}
		}
		queueNum += 1;
		return Integer.toString(queueNum);
	}
	
}
