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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.openmrs.Location;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.VisitAttributeType;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.VisitService;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.VisitQueueEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of a VisitQueueEntryProcessor which aims to retain legacy functionality using
 * visit attributes
 */
@Slf4j
@Component
public class VisitAttributeQueueNumberGenerator implements VisitQueueEntryProcessor {
	
	@Autowired
	VisitService visitService;
	
	@Autowired
	AdministrationService adminService;
	
	/**
	 * This populates the given VisitQueueEntry with an appropriate patient queue number
	 */
	public void beforeSaveVisitQueueEntry(VisitQueueEntry visitQueueEntry) {
		QueueEntry queueEntry = visitQueueEntry.getQueueEntry();
		Queue queue = queueEntry.getQueue();
		if (queue == null) {
			throw new APIException("Queue is required");
		}
		Location location = queue.getLocation();
		if (location == null) {
			throw new APIException("Location is required");
		}
		Visit visit = visitQueueEntry.getVisit();
		if (visit == null) {
			throw new APIException("Visit is required");
		}
		VisitAttributeType visitAttributeType = getVisitAttributeType();
		
		LocalDate minDate = LocalDate.now();
		LocalDate maxDate = LocalDate.now().plusDays(1);
		Date startOfDay = Date.from(minDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
		Date endOfDay = Date.from(maxDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
		int visitQueueNumber = 1;
		for (QueueEntry qe : queue.getQueueEntries()) {
			if (BooleanUtils.isNotTrue(qe.getVoided())) {
				if (!qe.getStartedAt().before(startOfDay) && qe.getStartedAt().before(endOfDay)) {
					visitQueueNumber++;
				}
			}
		}
		String paddedString = StringUtils.leftPad(String.valueOf(visitQueueNumber), 3, "0");
		String serviceName = queue.getName().toUpperCase();
		String prefix = serviceName.length() < 3 ? serviceName : serviceName.substring(0, 3);
		String queueNumber = prefix + "-" + paddedString;
		
		// Associate this queue number directly
		queueEntry.setPatientQueueNumber(queueNumber);
		
		// Create Visit Attribute using generated queue number
		VisitAttribute visitAttribute = new VisitAttribute();
		visitAttribute.setAttributeType(visitAttributeType);
		visitAttribute.setValueReferenceInternal(queueNumber);
		visit.addAttribute(visitAttribute);
	}
	
	private VisitAttributeType getVisitAttributeType() {
		String gpName = "queue.visitQueueNumberAttributeUuid";
		String attributeType = adminService.getGlobalPropertyValue(gpName, "");
		if (StringUtils.isNotEmpty(attributeType)) {
			return visitService.getVisitAttributeTypeByUuid(attributeType);
		}
		throw new APIException("Visit Attribute Type is required.  Please configure global property: " + gpName);
	}
}
