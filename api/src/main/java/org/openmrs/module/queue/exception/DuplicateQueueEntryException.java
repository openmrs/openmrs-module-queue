/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.exception;

import org.openmrs.api.APIException;

public class DuplicateQueueEntryException extends APIException {
	
	private static final long serialVersionUID = 1L;
	
	public DuplicateQueueEntryException() {
	}
	
	/**
	 * @param message
	 */
	public DuplicateQueueEntryException(String message) {
		super(message);
	}
	
	/**
	 * @param message
	 * @param cause
	 */
	public DuplicateQueueEntryException(String message, Throwable cause) {
		super(message, cause);
	}
	
	/**
	 * @param cause
	 */
	public DuplicateQueueEntryException(Throwable cause) {
		super(cause);
	}
}
