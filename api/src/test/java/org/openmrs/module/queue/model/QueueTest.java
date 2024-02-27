/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Date;

import org.junit.Test;

public class QueueTest {
	
	Date now = new Date();
	
	@Test
	public void addQueueRoom_shouldAddANewQueueRoomEvenIfQueueRoomsIsNull() {
		Queue queue = new Queue();
		queue.setQueueRooms(null);
		QueueRoom queueRoom = new QueueRoom();
		queue.addQueueRoom(queueRoom);
		assertThat(queue.getQueueRooms().size(), equalTo(1));
	}
	
	@Test
	public void getActiveQueueRooms_shouldNotReturnRetiredQueueRooms() {
		Queue queue = new Queue();
		
		QueueRoom queueRoom = new QueueRoom();
		queue.addQueueRoom(queueRoom);
		
		assertThat(queue.getQueueRooms(), contains(queueRoom));
		assertThat(queue.getActiveQueueRooms(), contains(queueRoom));
		queueRoom.setRetired(true);
		assertThat(queue.getQueueRooms(), contains(queueRoom));
		assertThat(queue.getActiveQueueRooms(), not(contains(queueRoom)));
	}
}
