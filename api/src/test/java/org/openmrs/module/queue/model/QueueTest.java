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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

public class QueueTest {
	
	Date now = new Date();
	
	@Test
	public void addQueueEntry_shouldAddANewQueueEntryEvenIfQueueEntriesIsNull() {
		Queue queue = new Queue();
		queue.setQueueEntries(null);
		QueueEntry queueEntry = new QueueEntry();
		queue.addQueueEntry(queueEntry);
		assertThat(queue.getQueueEntries().size(), equalTo(1));
	}
	
	@Test
	public void addQueueRoom_shouldAddANewQueueRoomEvenIfQueueRoomsIsNull() {
		Queue queue = new Queue();
		queue.setQueueRooms(null);
		QueueRoom queueRoom = new QueueRoom();
		queue.addQueueRoom(queueRoom);
		assertThat(queue.getQueueRooms().size(), equalTo(1));
	}
	
	@Test
	public void getActiveQueueEntries_shouldReturnAnEmptyListIfNoQueueEntriesExist() {
		Queue queue = new Queue();
		queue.setQueueEntries(null);
		assertThat(queue.getQueueEntries(), nullValue());
		assertThat(queue.getActiveQueueEntries(), notNullValue());
		assertThat(queue.getActiveQueueEntries().size(), equalTo(0));
	}
	
	@Test
	public void getActiveQueueEntries_shouldReturnOnlyNonVoidedEntries() {
		Queue queue = new Queue();
		
		QueueEntry queueEntry = new QueueEntry();
		queueEntry.setStartedAt(DateUtils.addHours(now, -1));
		queueEntry.setEndedAt(null);
		queue.addQueueEntry(queueEntry);
		
		assertThat(queue.getQueueEntries(), contains(queueEntry));
		assertThat(queue.getActiveQueueEntries(), contains(queueEntry));
		queueEntry.setVoided(true);
		assertThat(queue.getQueueEntries(), contains(queueEntry));
		assertThat(queue.getActiveQueueEntries(), not(contains(queueEntry)));
	}
	
	@Test
	public void getActiveQueueEntries_shouldNotReturnFutureQueueEntries() {
		Queue queue = new Queue();
		
		QueueEntry queueEntry = new QueueEntry();
		queueEntry.setStartedAt(DateUtils.addHours(now, -1));
		queueEntry.setEndedAt(null);
		queue.addQueueEntry(queueEntry);
		
		assertThat(queue.getQueueEntries(), contains(queueEntry));
		assertThat(queue.getActiveQueueEntries(), contains(queueEntry));
		queueEntry.setStartedAt(DateUtils.addHours(now, 1));
		assertThat(queue.getQueueEntries(), contains(queueEntry));
		assertThat(queue.getActiveQueueEntries(), not(contains(queueEntry)));
	}
	
	@Test
	public void getActiveQueueEntries_shouldNotReturnEndedQueueEntries() {
		Queue queue = new Queue();
		
		QueueEntry queueEntry = new QueueEntry();
		queueEntry.setStartedAt(DateUtils.addHours(now, -2));
		queueEntry.setEndedAt(null);
		queue.addQueueEntry(queueEntry);
		
		assertThat(queue.getQueueEntries(), contains(queueEntry));
		assertThat(queue.getActiveQueueEntries(), contains(queueEntry));
		queueEntry.setEndedAt(DateUtils.addHours(now, 1));
		assertThat(queue.getQueueEntries(), contains(queueEntry));
		assertThat(queue.getActiveQueueEntries(), not(contains(queueEntry)));
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
