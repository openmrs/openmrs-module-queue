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
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Provider;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "room_provider_map")
public class RoomProviderMap extends BaseOpenmrsData {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "room_provider_map_id")
	private Integer roomProviderMapId;
	
	@ManyToOne
	@JoinColumn(name = "queue_room_id", nullable = false)
	private QueueRoom queueRoom;
	
	@ManyToOne
	@JoinColumn(name = "provider_id", nullable = false)
	private Provider provider;
	
	@Override
	public Integer getId() {
		return getRoomProviderMapId();
	}
	
	@Override
	public void setId(Integer integer) {
		setRoomProviderMapId(integer);
	}
}
