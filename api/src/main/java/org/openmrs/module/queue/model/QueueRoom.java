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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openmrs.BaseChangeableOpenmrsMetadata;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "queue_room")
public class QueueRoom extends BaseChangeableOpenmrsMetadata {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "queue_room_id")
	private Integer queueRoom;
	
	@ManyToOne
	@JoinColumn(name = "queue_id", nullable = false)
	private Queue queue;
	
	@Override
	public Integer getId() {
		return getQueueRoom();
	}
	
	@Override
	public void setId(Integer integer) {
		setQueueRoom(integer);
	}
}
