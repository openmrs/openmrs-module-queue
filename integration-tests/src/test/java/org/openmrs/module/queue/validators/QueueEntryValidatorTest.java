/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class QueueEntryValidatorTest extends BaseModuleContextSensitiveTest {
	
	private static final String VALID_STATUS_CONCEPT = "31b910bd-298c-4ecf-a632-661ae2f4460y";
	
	private static final String INVALID_STATUS_CONCEPT_UUID = "67b910bd-298c-4ecf-a632-661ae2f446op";
	
	private static final String VALID_PRIORITY_CONCEPT = "90b910bd-298c-4ecf-a632-661ae2f446op";
	
	private static final String INVALID_PRIORITY_CONCEPT_UUID = "67b910bd-298c-4ecf-a632-661ae2f446op";
	
	private static final String QUEUE_UUID = "3eb7fe43-2813-4kbc-80dc-2e5d30252bb5";
	
	private static final String PATIENT_UUID = "90b38324-e2fd-4feb-95b7-9e9a2a8876fg";
	
	private static final List<String> INITIAL_CONCEPTS_DATASETS = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/validators/QueueEntryValidatorTest_globalPropertyInitialDataset.xml");
	
	private Errors errors;
	
	private QueueEntry queueEntry;
	
	private Visit visit;
	
	@Autowired
	private QueueEntryValidator validator;
	
	@Autowired
	private QueueServicesWrapper services;
	
	@Before
	public void setup() {
		INITIAL_CONCEPTS_DATASETS.forEach(this::executeDataSet);
		Queue queue = services.getQueueService().getQueueByUuid(QUEUE_UUID).orElse(null);
		Patient patient = Context.getPatientService().getPatientByUuid(PATIENT_UUID);
		queueEntry = new QueueEntry();
		queueEntry.setQueue(queue);
		visit = new Visit();
		visit.setPatient(patient);
		visit.setStartDatetime(DateUtils.addHours(new Date(), -2));
		visit.setStopDatetime(DateUtils.addHours(new Date(), -1));
		errors = new BindException(queueEntry, queueEntry.getClass().getName());
	}
	
	@Test
	public void validatorNotNull() {
		assertThat(validator, is(notNullValue()));
	}
	
	@Test
	public void shouldSupportQueueEntry() {
		assertThat(validator.supports(QueueEntry.class), is(true));
	}
	
	@Test
	public void shouldRejectQueueEntryWithNullStatus() {
		validator.validate(queueEntry, errors);
		
		FieldError queueEntryStatusFieldError = errors.getFieldError("status");
		assertThat(queueEntryStatusFieldError, is(notNullValue()));
		assertThat(queueEntryStatusFieldError.getField(), is("status"));
		assertThat(queueEntryStatusFieldError.getCode(), is("queueEntry.status.null"));
		assertThat(queueEntryStatusFieldError.getDefaultMessage(), is("The property status should not be null"));
	}
	
	@Test
	public void shouldNotRejectQueueEntryWithValidConceptStatus() {
		Concept validStatusConcept = Context.getConceptService().getConceptByUuid(VALID_STATUS_CONCEPT);
		queueEntry.setStatus(validStatusConcept);
		validator.validate(queueEntry, errors);
		
		FieldError queueEntryStatusFieldError = errors.getFieldError("status");
		assertThat(queueEntryStatusFieldError, is(nullValue()));
	}
	
	@Test
	public void shouldRejectQueueEntryWithInValidConceptStatus() {
		Concept InValidStatusConcept = Context.getConceptService().getConceptByUuid(INVALID_STATUS_CONCEPT_UUID);
		queueEntry.setStatus(InValidStatusConcept);
		validator.validate(queueEntry, errors);
		
		FieldError queueEntryStatusFieldError = errors.getFieldError("status");
		assertThat(queueEntryStatusFieldError, is(notNullValue()));
		assertThat(queueEntryStatusFieldError.getCode(), is("queueEntry.status.invalid"));
	}
	
	@Test
	public void shouldRejectQueueEntryWithNullStartedAtDate() {
		validator.validate(queueEntry, errors);
		
		FieldError queueEntryStatusFieldError = errors.getFieldError("startedAt");
		assertThat(queueEntryStatusFieldError, is(notNullValue()));
		assertThat(queueEntryStatusFieldError.getCode(), is("queueEntry.startedAt.null"));
	}
	
	@Test
	public void shouldRejectQueueEntryIfEndedAtIsBeforeStartedAtDate() {
		queueEntry.setStartedAt(new Date());
		queueEntry.setEndedAt(yesterday());
		validator.validate(queueEntry, errors);
		
		FieldError queueEntryStatusFieldError = errors.getFieldError("endedAt");
		assertThat(queueEntryStatusFieldError, is(notNullValue()));
		assertThat(queueEntryStatusFieldError.getCode(), is("queueEntry.endedAt.invalid"));
	}
	
	@Test
	public void shouldNotRejectIfQueueEntryStartedAndEndedDuringVisit() {
		queueEntry.setVisit(visit);
		queueEntry.setStartedAt(visit.getStartDatetime());
		queueEntry.setEndedAt(visit.getStopDatetime());
		validator.validate(queueEntry, errors);
		FieldError startedAtFieldError = errors.getFieldError("startedAt");
		assertThat(startedAtFieldError, is(nullValue()));
		FieldError endedAtFieldError = errors.getFieldError("endedAt");
		assertThat(endedAtFieldError, is(nullValue()));
	}
	
	@Test
	public void shouldRejectIfQueueEntryStartedBeforeVisitStartDate() {
		queueEntry.setVisit(visit);
		queueEntry.setStartedAt(DateUtils.addMilliseconds(visit.getStartDatetime(), -1));
		validator.validate(queueEntry, errors);
		FieldError startedAtFieldError = errors.getFieldError("startedAt");
		assertThat(startedAtFieldError, is(notNullValue()));
		assertThat(startedAtFieldError.getCode(), is("queue.entry.error.cannotStartBeforeVisitStartDate"));
	}
	
	@Test
	public void shouldRejectIfQueueEntryStartedAfterVisitEndDate() {
		queueEntry.setVisit(visit);
		queueEntry.setStartedAt(DateUtils.addMilliseconds(visit.getStopDatetime(), 1));
		validator.validate(queueEntry, errors);
		FieldError startedAtFieldError = errors.getFieldError("startedAt");
		assertThat(startedAtFieldError, is(notNullValue()));
		assertThat(startedAtFieldError.getCode(), is("queue.entry.error.cannotStartAfterVisitStopDate"));
	}
	
	@Test
	public void shouldRejectIfQueueEntryEndedAfterVisitEndDate() {
		queueEntry.setVisit(visit);
		queueEntry.setStartedAt(visit.getStartDatetime());
		queueEntry.setEndedAt(DateUtils.addHours(visit.getStopDatetime(), 1));
		validator.validate(queueEntry, errors);
		FieldError endedAtFieldError = errors.getFieldError("endedAt");
		assertThat(endedAtFieldError, is(notNullValue()));
		assertThat(endedAtFieldError.getCode(), is("queue.entry.error.cannotEndAfterVisitStopDate"));
	}
	
	@Test
	public void shouldRejectIfQueueEntryNotEndedWhenVisitStopped() {
		queueEntry.setVisit(visit);
		queueEntry.setStartedAt(visit.getStartDatetime());
		validator.validate(queueEntry, errors);
		FieldError endedAtFieldError = errors.getFieldError("endedAt");
		assertThat(endedAtFieldError, is(notNullValue()));
		assertThat(endedAtFieldError.getCode(), is("queue.entry.error.cannotEndAfterVisitStopDate"));
	}
	
	@Test
	public void shouldRejectQueueEntryWithNullQueue() {
		queueEntry.setQueue(null);
		validator.validate(queueEntry, errors);
		
		FieldError queueFieldError = errors.getFieldError("queue");
		assertThat(queueFieldError, is(notNullValue()));
		assertThat(queueFieldError.getCode(), is("queueEntry.queue.null"));
	}
	
	@Test
	public void shouldRejectQueueEntryWithNullPatient() {
		validator.validate(queueEntry, errors);
		
		FieldError patientFieldError = errors.getFieldError("patient");
		assertThat(patientFieldError, is(notNullValue()));
		assertThat(patientFieldError.getCode(), is("queueEntry.patient.null"));
	}
	
	@Test
	public void shouldRejectQueueEntryWithNullPriority() {
		validator.validate(queueEntry, errors);
		
		FieldError priorityFieldError = errors.getFieldError("priority");
		assertThat(priorityFieldError, is(notNullValue()));
		assertThat(priorityFieldError.getCode(), is("queueEntry.priority.null"));
		assertThat(priorityFieldError.getDefaultMessage(), is("The property priority should not be null"));
	}
	
	@Test
	public void shouldNotRejectQueueEntryWithValidConceptPriority() {
		Concept validPriorityConcept = Context.getConceptService().getConceptByUuid(VALID_PRIORITY_CONCEPT);
		queueEntry.setPriority(validPriorityConcept);
		validator.validate(queueEntry, errors);
		
		FieldError priorityFieldError = errors.getFieldError("priority");
		assertThat(priorityFieldError, is(nullValue()));
	}
	
	@Test
	public void shouldRejectQueueEntryWithInvalidConceptPriority() {
		Concept invalidPriorityConcept = Context.getConceptService().getConceptByUuid(INVALID_PRIORITY_CONCEPT_UUID);
		queueEntry.setPriority(invalidPriorityConcept);
		validator.validate(queueEntry, errors);
		
		FieldError priorityFieldError = errors.getFieldError("priority");
		assertThat(priorityFieldError, is(notNullValue()));
		assertThat(priorityFieldError.getCode(), is("queueEntry.priority.invalid"));
	}
	
	@Test
	public void shouldRejectDuplicateQueueEntry() {
		Queue queue = services.getQueueService().getQueueByUuid(QUEUE_UUID).orElse(null);
		assertThat(queue, is(notNullValue()));
		Patient patient = Context.getPatientService().getPatientByUuid(PATIENT_UUID);
		assertThat(patient, is(notNullValue()));
		Concept validStatus = Context.getConceptService().getConceptByUuid(VALID_STATUS_CONCEPT);
		Concept validPriority = Context.getConceptService().getConceptByUuid(VALID_PRIORITY_CONCEPT);
		
		QueueEntry existingEntry = new QueueEntry();
		existingEntry.setQueue(queue);
		existingEntry.setPatient(patient);
		existingEntry.setStatus(validStatus);
		existingEntry.setPriority(validPriority);
		existingEntry.setStartedAt(new Date());
		services.getQueueEntryService().saveQueueEntry(existingEntry);
		
		QueueEntry duplicateEntry = new QueueEntry();
		duplicateEntry.setQueue(queue);
		duplicateEntry.setPatient(patient);
		duplicateEntry.setStatus(validStatus);
		duplicateEntry.setPriority(validPriority);
		duplicateEntry.setStartedAt(new Date());
		
		Errors duplicateErrors = new BindException(duplicateEntry, duplicateEntry.getClass().getName());
		validator.validate(duplicateEntry, duplicateErrors);
		
		assertThat(duplicateErrors.hasGlobalErrors(), is(true));
		assertThat(duplicateErrors.getGlobalError().getCode(), is("queue.entry.error.duplicate"));
	}
	
	@Test
	public void shouldNotRejectSameQueueEntryOnUpdate() {
		Queue queue = services.getQueueService().getQueueByUuid(QUEUE_UUID).orElse(null);
		assertThat(queue, is(notNullValue()));
		Patient patient = Context.getPatientService().getPatientByUuid(PATIENT_UUID);
		assertThat(patient, is(notNullValue()));
		Concept validStatus = Context.getConceptService().getConceptByUuid(VALID_STATUS_CONCEPT);
		Concept validPriority = Context.getConceptService().getConceptByUuid(VALID_PRIORITY_CONCEPT);
		
		QueueEntry entry = new QueueEntry();
		entry.setQueue(queue);
		entry.setPatient(patient);
		entry.setStatus(validStatus);
		entry.setPriority(validPriority);
		entry.setStartedAt(new Date());
		QueueEntry savedEntry = services.getQueueEntryService().saveQueueEntry(entry);
		
		Errors updateErrors = new BindException(savedEntry, savedEntry.getClass().getName());
		validator.validate(savedEntry, updateErrors);
		
		assertThat(updateErrors.hasGlobalErrors(), is(false));
	}
	
	@Test
	public void shouldNotRejectNonOverlappingQueueEntriesForSamePatientAndQueue() {
		Queue queue = services.getQueueService().getQueueByUuid(QUEUE_UUID).orElse(null);
		assertThat(queue, is(notNullValue()));
		Patient patient = Context.getPatientService().getPatientByUuid(PATIENT_UUID);
		assertThat(patient, is(notNullValue()));
		Concept validStatus = Context.getConceptService().getConceptByUuid(VALID_STATUS_CONCEPT);
		Concept validPriority = Context.getConceptService().getConceptByUuid(VALID_PRIORITY_CONCEPT);
		
		Date now = new Date();
		QueueEntry firstEntry = new QueueEntry();
		firstEntry.setQueue(queue);
		firstEntry.setPatient(patient);
		firstEntry.setStatus(validStatus);
		firstEntry.setPriority(validPriority);
		firstEntry.setStartedAt(DateUtils.addHours(now, -2));
		firstEntry.setEndedAt(DateUtils.addHours(now, -1));
		services.getQueueEntryService().saveQueueEntry(firstEntry);
		
		QueueEntry secondEntry = new QueueEntry();
		secondEntry.setQueue(queue);
		secondEntry.setPatient(patient);
		secondEntry.setStatus(validStatus);
		secondEntry.setPriority(validPriority);
		secondEntry.setStartedAt(now);
		
		Errors secondEntryErrors = new BindException(secondEntry, secondEntry.getClass().getName());
		validator.validate(secondEntry, secondEntryErrors);
		
		assertThat(secondEntryErrors.hasGlobalErrors(), is(false));
	}
	
	private Date yesterday() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -1);
		return calendar.getTime();
	}
}
