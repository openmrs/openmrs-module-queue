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

import java.util.Date;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openmrs.BaseChangeableOpenmrsData;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Setter
@Getter
@ToString
@Entity
@Table(name = "queue_entry")
public class QueueEntry extends BaseChangeableOpenmrsData {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "queue_entry_id")
	private Integer queueEntryId;
	
	@ToString.Exclude
	@ManyToOne
	@JoinColumn(name = "queue_id", nullable = false)
	private Queue queue;
	
	@ManyToOne
	@JoinColumn(name = "patient_id", nullable = false)
	private Patient patient;
	
	@ManyToOne
	@JoinColumn(name = "priority", referencedColumnName = "concept_id", nullable = false)
	private Concept priority;
	
	@Column(name = "priority_comment")
	private String priorityComment;
	
	@ManyToOne
	@JoinColumn(name = "status", referencedColumnName = "concept_id", nullable = false)
	private Concept status;
	
	@Column(name = "sort_weight")
	private Double sortWeight;
	
	//The Location the patient is waiting for, if any.
	@ManyToOne
	@JoinColumn(name = "location_waiting_for", referencedColumnName = "location_id")
	private Location locationWaitingFor;
	
	//The Provider the patient is waiting for, if any.
	@ManyToOne
	@JoinColumn(name = "provider_waiting_for", referencedColumnName = "provider_id")
	private Provider providerWaitingFor;
	
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
