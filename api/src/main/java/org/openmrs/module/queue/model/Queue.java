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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.openmrs.BaseChangeableOpenmrsMetadata;
import org.openmrs.Concept;
import org.openmrs.Location;

@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "queue")
public class Queue extends BaseChangeableOpenmrsMetadata {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "queue_id")
	private Integer queueId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id", nullable = false)
	private Location location;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "service", referencedColumnName = "concept_id", nullable = false)
	private Concept service;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "priority_concept_set", referencedColumnName = "concept_id")
	private Concept priorityConceptSet;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "status_concept_set", referencedColumnName = "concept_id")
	private Concept statusConceptSet;
	
	@OneToMany(mappedBy = "queue", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<QueueRoom> queueRooms;
	
	/**
	 * @return all non-retired QueueRooms
	 */
	public List<QueueRoom> getActiveQueueRooms() {
		if (queueRooms == null) {
			return new ArrayList<>();
		}
		return queueRooms.stream().filter(r -> BooleanUtils.isNotTrue(r.getRetired())).collect(Collectors.toList());
	}
	
	/**
	 * @param queueRoom the QueueRoom to add
	 */
	public void addQueueRoom(QueueRoom queueRoom) {
		if (queueRooms == null) {
			queueRooms = new ArrayList<>();
		}
		queueRooms.add(queueRoom);
	}
	
	@Override
	public Integer getId() {
		return getQueueId();
	}
	
	@Override
	public void setId(Integer id) {
		this.setQueueId(id);
	}
}
