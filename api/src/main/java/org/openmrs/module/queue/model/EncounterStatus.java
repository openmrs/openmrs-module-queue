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
import javax.persistence.Table;

import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openmrs.BaseOpenmrsMetadata;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.Visit;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
@Entity
@Table(name = "encounter_status")
public class EncounterStatus extends BaseOpenmrsMetadata {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "encounter_status_id")
	private Integer encounterStatusId;
	
	@Column(name = "patient_id")
	private Patient patient;
	
	@Column(name = "encounter_type_id")
	private EncounterType encounterType;
	
	@Column(name = "encounter_id")
	private Encounter encounter;
	
	@Column(name = "encounter_datetime")
	private Date encounterDatetime;
	
	/**
	 * Note: Visit_id will be used to help unambiguously identify the encounter to which the status
	 * belongs not meant to assign status directly to a visit.
	 */
	@Column(name = "visit_id")
	private Visit visit;
	
	@Column(name = "status", nullable = false)
	private Concept status;
	
	@Override
	public Integer getId() {
		return getEncounterStatusId();
	}
	
	@Override
	public void setId(Integer id) {
		this.setEncounterStatusId(id);
	}
}
