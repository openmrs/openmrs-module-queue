/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.web.dto;

import lombok.Getter;

@Getter
public class QueueEntryTransitionRequest {
	
	private String queueEntryToTransition;
	
	private String transitionDate;
	
	private String newQueue;
	
	private String newStatus;
	
	private String newPriority;
	
	private String newPriorityComment;
	
	private String newLocationWaitingFor;
	
	private String newProviderWaitingFor;

	public static final String NEW_LOCATION_WAITING_FOR_FIELD = "newLocationWaitingFor";

	public static final String NEW_PROVIDER_WAITING_FOR_FIELD = "newProviderWaitingFor";
}
