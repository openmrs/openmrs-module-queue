/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.web.dto;

import java.util.Date;

/**
 * A lightweight DTO representing only the fields the Service Queues UI actually needs for each
 * active queue entry.
 * <p>
 * Replaces the heavyweight custom representation pattern:
 * {@code v=custom:(uuid,display,patient:(uuid,display,person:(age,gender)),
 * queue:(uuid,display,location),status:(uuid,display),priority:(uuid,display),
 * startedAt,sortWeight,visitQueueNumber)}
 * <p>
 * This DTO eliminates deep graph traversal (Patient → Person → Concept...) and returns only what
 * the frontend renders.
 */
public class QueueEntryMetricsResponse {
	
	// ── Queue entry identity ────────────────────────────────────────────────
	private String uuid;
	
	// ── Patient (flat — no nested object graph) ─────────────────────────────
	private String patientUuid;
	
	private String patientName;
	
	private String patientIdentifier; // e.g. OpenMRS ID shown in the table
	
	private Integer patientAge;
	
	private String patientGender;
	
	// ── Queue info ───────────────────────────────────────────────────────────
	private String queueUuid;
	
	private String queueName;
	
	private String locationUuid;
	
	private String locationName;
	
	// ── Entry status & priority (display names only) ─────────────────────────
	private String statusUuid;
	
	private String statusDisplay;
	
	private String priorityUuid;
	
	private String priorityDisplay;
	
	// ── Timing ───────────────────────────────────────────────────────────────
	private Date startedAt;
	
	/** Wait time in minutes, computed server-side to avoid extra round-trips */
	private Long waitTimeMinutes;
	
	// ── Ordering ─────────────────────────────────────────────────────────────
	private Double sortWeight;
	
	// ── Visit queue number (ticket shown to patient) ─────────────────────────
	private String visitQueueNumber;
	
	// ── Constructors ──────────────────────────────────────────────────────────
	
	public QueueEntryMetricsResponse() {
	}
	
	// ── Getters & Setters ─────────────────────────────────────────────────────
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getPatientUuid() {
		return patientUuid;
	}
	
	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}
	
	public String getPatientName() {
		return patientName;
	}
	
	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}
	
	public String getPatientIdentifier() {
		return patientIdentifier;
	}
	
	public void setPatientIdentifier(String patientIdentifier) {
		this.patientIdentifier = patientIdentifier;
	}
	
	public Integer getPatientAge() {
		return patientAge;
	}
	
	public void setPatientAge(Integer patientAge) {
		this.patientAge = patientAge;
	}
	
	public String getPatientGender() {
		return patientGender;
	}
	
	public void setPatientGender(String patientGender) {
		this.patientGender = patientGender;
	}
	
	public String getQueueUuid() {
		return queueUuid;
	}
	
	public void setQueueUuid(String queueUuid) {
		this.queueUuid = queueUuid;
	}
	
	public String getQueueName() {
		return queueName;
	}
	
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	
	public String getLocationUuid() {
		return locationUuid;
	}
	
	public void setLocationUuid(String locationUuid) {
		this.locationUuid = locationUuid;
	}
	
	public String getLocationName() {
		return locationName;
	}
	
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}
	
	public String getStatusUuid() {
		return statusUuid;
	}
	
	public void setStatusUuid(String statusUuid) {
		this.statusUuid = statusUuid;
	}
	
	public String getStatusDisplay() {
		return statusDisplay;
	}
	
	public void setStatusDisplay(String statusDisplay) {
		this.statusDisplay = statusDisplay;
	}
	
	public String getPriorityUuid() {
		return priorityUuid;
	}
	
	public void setPriorityUuid(String priorityUuid) {
		this.priorityUuid = priorityUuid;
	}
	
	public String getPriorityDisplay() {
		return priorityDisplay;
	}
	
	public void setPriorityDisplay(String priorityDisplay) {
		this.priorityDisplay = priorityDisplay;
	}
	
	public Date getStartedAt() {
		return startedAt;
	}
	
	public void setStartedAt(Date startedAt) {
		this.startedAt = startedAt;
	}
	
	public Long getWaitTimeMinutes() {
		return waitTimeMinutes;
	}
	
	public void setWaitTimeMinutes(Long waitTimeMinutes) {
		this.waitTimeMinutes = waitTimeMinutes;
	}
	
	public Double getSortWeight() {
		return sortWeight;
	}
	
	public void setSortWeight(Double sortWeight) {
		this.sortWeight = sortWeight;
	}
	
	public String getVisitQueueNumber() {
		return visitQueueNumber;
	}
	
	public void setVisitQueueNumber(String visitQueueNumber) {
		this.visitQueueNumber = visitQueueNumber;
	}
}
