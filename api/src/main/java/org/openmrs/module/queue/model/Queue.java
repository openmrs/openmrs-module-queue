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

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;
import org.openmrs.BaseChangeableOpenmrsMetadata;
import org.openmrs.Concept;
import org.openmrs.Location;

@EqualsAndHashCode(callSuper = true)
@Data
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
	
	@OneToMany(mappedBy = "queue", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@Where(clause = "voided = 0 and (started_at <= current_timestamp() and ended_at is null)")
	private List<QueueEntry> queueEntries;
	
	@OneToMany(mappedBy = "queue", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@Where(clause = "retired = 0")
	private List<QueueRoom> queueRooms;
	
	@Override
	public Integer getId() {
		return getQueueId();
	}
	
	@Override
	public void setId(Integer id) {
		this.setQueueId(id);
	}
}
