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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import java.util.Date;
import java.util.UUID;

import lombok.Data;
import org.hibernate.search.annotations.Field;
import org.openmrs.Auditable;
import org.openmrs.Retireable;
import org.openmrs.User;
import org.openmrs.Visit;

@Data
@Entity
@Table(name = "visit_queue_entries")
public class VisitQueueEntry implements Auditable, Retireable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "visit_queue_entry_id")
	private Integer id;
	
	//confirm relationship
	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "visit_id", nullable = false)
	private Visit visit;
	
	//confirm relationship
	@Column(name = "queue_entry_id", nullable = false)
	private QueueEntry queueEntry;
	
	@ManyToOne(optional = false)
	@JoinColumn(name = "creator", updatable = false)
	protected User creator;
	
	@Column(name = "date_created", nullable = false, updatable = false)
	private Date dateCreated;
	
	@ManyToOne
	@JoinColumn(name = "changed_by")
	private User changedBy;
	
	@Column(name = "date_changed")
	private Date dateChanged;
	
	@Column(name = "retired", nullable = false)
	@Field
	private Boolean retired = Boolean.FALSE;
	
	@Column(name = "date_retired")
	private Date dateRetired;
	
	@ManyToOne
	@JoinColumn(name = "retired_by")
	private User retiredBy;
	
	@Column(name = "retire_reason")
	private String retireReason;
	
	@Column(name = "uuid", unique = true, nullable = false, length = 38)
	private String uuid = UUID.randomUUID().toString();
	
	@Override
	@Deprecated
	public Boolean isRetired() {
		return retired;
	}
}
