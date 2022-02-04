/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.exceptions;

public class QueueException extends RuntimeException {
	
	private static final long serialVersionUID = 12345L;
	
	private final Throwable clothedThrowable;
	
	public QueueException(final String exceptionMessage) {
		super(exceptionMessage);
		clothedThrowable = null;
	}
	
	/**
	 * @param exceptionMessage The message to register.
	 * @param clothedThrowable A throwable object that caused the Exception.
	 */
	public QueueException(final String exceptionMessage, final Throwable clothedThrowable) {
		super(exceptionMessage, clothedThrowable);
		this.clothedThrowable = clothedThrowable;
	}
	
	/**
	 * @param clothedThrowable A throwable object that caused the Exception.
	 */
	public QueueException(final Throwable clothedThrowable) {
		super(clothedThrowable);
		this.clothedThrowable = clothedThrowable;
	}
	
	/**
	 * returns the wrapped Throwable that caused this MethodInvocationException to be thrown
	 *
	 * @return Throwable thrown by method invocation
	 */
	public Throwable getWrappedThrowable() {
		return clothedThrowable;
	}
}
