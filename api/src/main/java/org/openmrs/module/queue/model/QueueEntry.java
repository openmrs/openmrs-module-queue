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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Date;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openmrs.BaseChangeableOpenmrsData;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;

@NoArgsConstructor
@Setter
@Getter
@ToString
@Entity
@Table(name = "queue_entry")
public class QueueEntry extends BaseChangeableOpenmrsData {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "queue_entry_id")
	private Integer queueEntryId;
	
	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "queue_id", nullable = false)
	private Queue queue;
	
	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "patient_id", nullable = false)
	private Patient patient;
	
	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "visit_id")
	private Visit visit;
	
	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "priority", referencedColumnName = "concept_id", nullable = false)
	private Concept priority;
	
	@Column(name = "priority_comment")
	private String priorityComment;
	
	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "status", referencedColumnName = "concept_id", nullable = false)
	private Concept status;
	
	// Provides a means to indicate the relative order within a queue.  Higher weight reflects higher priority.
	@Column(name = "sort_weight", nullable = false)
	private Double sortWeight = 0.0;
	
	//The Location the patient is waiting for, if any.
	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_waiting_for", referencedColumnName = "location_id")
	private Location locationWaitingFor;
	
	//The Provider the patient is waiting for, if any.
	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "provider_waiting_for", referencedColumnName = "provider_id")
	private Provider providerWaitingFor;
	
	//The queue the patient is coming from, if any.
	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "queue_coming_from", referencedColumnName = "queue_id")
	private Queue queueComingFrom;
	
	@Column(name = "started_at", nullable = false)
	private Date startedAt;
	
	@Column(name = "ended_at")
	private Date endedAt;
	
	@Override
	public Integer getId() {
		return getQueueEntryId();
	}
	
	@Override
	public void setId(Integer id) {
		this.setQueueEntryId(id);
	}
}
