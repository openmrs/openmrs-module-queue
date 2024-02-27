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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.BooleanUtils;
import org.openmrs.BaseChangeableOpenmrsMetadata;
import org.openmrs.Concept;
import org.openmrs.Location;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "queue")
public class Queue extends BaseChangeableOpenmrsMetadata {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "queue_id")
	private Integer queueId;
	
	@ManyToOne
	@JoinColumn(name = "location_id", nullable = false)
	private Location location;
	
	@ManyToOne
	@JoinColumn(name = "service", referencedColumnName = "concept_id", nullable = false)
	private Concept service;
	
	@ManyToOne
	@JoinColumn(name = "priority_concept_set", referencedColumnName = "concept_id")
	private Concept priorityConceptSet;
	
	@ManyToOne
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
