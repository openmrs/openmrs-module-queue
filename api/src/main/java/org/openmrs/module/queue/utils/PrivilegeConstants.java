/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.utils;

import org.openmrs.annotation.AddOnStartup;
import org.openmrs.annotation.HasAddOnStartupPrivileges;

/**
 * Contains all privilege names and their descriptions. Some privilege names may be marked with
 * AddOnStartup annotation.
 *
 * @see org.openmrs.annotation.AddOnStartup
 * @since 2.4.0
 */
@HasAddOnStartupPrivileges
public class PrivilegeConstants {
	
	// Add Privilege Constants
	@AddOnStartup(description = "Able to add/save queues")
	public static final String ADD_QUEUES = "Add Queues";
	
	// Get Privilege Constants
	@AddOnStartup(description = "Able to get/view queues")
	public static final String GET_QUEUES = "Get Queues";
	
	@AddOnStartup(description = "Able to get/view queue entries")
	public static final String GET_QUEUE_ENTRIES = "Get Queue Entries";
	
	// Delete Privilege Constants
	@AddOnStartup(description = "Able to delete/retire queues")
	public static final String DELETE_QUEUES = "Delete Queues";
	
	// Edit Privilege Constants
	@AddOnStartup(description = "Able to edit queues")
	public static final String EDIT_QUEUES = "Edit Queues";
	
	// Manage Privilege Constants
	@AddOnStartup(description = "Able to add/edit/retire queue entries")
	public static final String MANAGE_QUEUE_ENTRIES = "Manage Queue Entries";
	
	// Purge Privilege Constants
	public static final String PURGE_QUEUES = "Purge Queues";
	
	public static final String PURGE_QUEUE_ENTRIES = "Purge Queue Entries";
}
