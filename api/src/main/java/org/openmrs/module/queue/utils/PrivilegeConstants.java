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
	
	@AddOnStartup(description = "Able to get/view queues")
	public static final String GET_QUEUES = "Get Queues";
	
	@AddOnStartup(description = "Able to get/view queue entries")
	public static final String GET_QUEUE_ENTRIES = "Get Queue Entries";
	
	@AddOnStartup(description = "Able to get/view queue rooms")
	public static final String GET_QUEUE_ROOMS = "Get Queue Rooms";
	
	@AddOnStartup(description = "Able to get/view room provider maps")
	public static final String GET_ROOM_PROVIDER_MAPS = "Get Room Provider Maps";
	
	@AddOnStartup(description = "Able to get sort weight generator")
	public static final String GET_SORT_WEIGHT_GENERATOR = "Get Sort Weight Generator";
	
	@AddOnStartup(description = "Able to add/edit/retire queues")
	public static final String MANAGE_QUEUES = "Manage Queues";
	
	@AddOnStartup(description = "Able to add/edit/retire queue entries")
	public static final String MANAGE_QUEUE_ENTRIES = "Manage Queue Entries";
	
	@AddOnStartup(description = "Able to add/edit/retire queue rooms")
	public static final String MANAGE_QUEUE_ROOMS = "Manage Queue Rooms";
	
	@AddOnStartup(description = "Able to add/edit/retire room provider maps")
	public static final String MANAGE_ROOM_PROVIDER_MAPS = "Manage Room Provider Maps";
	
	@AddOnStartup(description = "Able to add/edit/retire sort weight generator")
	public static final String MANAGE_SORT_WEIGHT_GENERATOR = "Manage Sort Weight Generator";
	
	public static final String PURGE_QUEUES = "Purge Queues";
	
	public static final String PURGE_QUEUE_ENTRIES = "Purge Queue Entries";
	
	public static final String PURGE_QUEUE_ROOMS = "Purge Queue Rooms";
	
	public static final String PURGE_ROOM_PROVIDER_MAPS = "Purge Room Provider Maps";
}
