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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.utils.QueueValidationUtils;
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
	
	private static final List<String> INITIAL_CONCEPTS_DATASETS = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/validators/QueueEntryValidatorTest_globalPropertyInitialDataset.xml");
	
	private Errors errors;
	
	private QueueEntry queueEntry;
	
	@Autowired
	private QueueEntryValidator validator;
	
	@Before
	public void setup() {
		INITIAL_CONCEPTS_DATASETS.forEach(this::executeDataSet);
		queueEntry = new QueueEntry();
		errors = new BindException(queueEntry, queueEntry.getClass().getName());
	}
	
	@Test
	public void validatorNotNull() {
		assertNotNull(validator);
	}
	
	@Test
	public void shouldSupportQueueEntry() {
		assertTrue(validator.supports(QueueEntry.class));
	}
	
	@Test
	public void shouldReturnTrueForValidStatusConcept() {
		Concept concept = Context.getConceptService().getConceptByUuid(VALID_STATUS_CONCEPT);
		assertThat(concept, notNullValue());
		assertTrue(QueueValidationUtils.isValidStatus(concept));
	}
	
	@Test
	public void shouldReturnFalseForInvalidStatusConcept() {
		Concept concept = Context.getConceptService().getConceptByUuid(INVALID_STATUS_CONCEPT_UUID);
		assertThat(concept, notNullValue());
		assertFalse(QueueValidationUtils.isValidStatus(concept));
	}
	
	@Test
	public void shouldRejectQueueEntryWithNullStatus() {
		validator.validate(queueEntry, errors);
		
		FieldError queueEntryStatusFieldError = errors.getFieldError("status");
		assertNotNull(queueEntryStatusFieldError);
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
		assertNull(queueEntryStatusFieldError);
	}
	
	@Test
	public void shouldRejectQueueEntryWithInValidConceptStatus() {
		Concept InValidStatusConcept = Context.getConceptService().getConceptByUuid(INVALID_STATUS_CONCEPT_UUID);
		queueEntry.setStatus(InValidStatusConcept);
		validator.validate(queueEntry, errors);
		
		FieldError queueEntryStatusFieldError = errors.getFieldError("status");
		assertNotNull(queueEntryStatusFieldError);
		assertThat(queueEntryStatusFieldError.getCode(), is("queueEntry.status.invalid"));
	}
	
	@Test
	public void shouldRejectQueueEntryWithNullStartedAtDate() {
		validator.validate(queueEntry, errors);
		
		FieldError queueEntryStatusFieldError = errors.getFieldError("startedAt");
		assertNotNull(queueEntryStatusFieldError);
		assertThat(queueEntryStatusFieldError.getCode(), is("queueEntry.startedAt.null"));
	}
	
	@Test
	public void shouldRejectQueueEntryIfEndedAtIsBeforeStartedAtDate() {
		queueEntry.setStartedAt(new Date());
		queueEntry.setEndedAt(yesterday());
		validator.validate(queueEntry, errors);
		
		FieldError queueEntryStatusFieldError = errors.getFieldError("endedAt");
		assertNotNull(queueEntryStatusFieldError);
		assertThat(queueEntryStatusFieldError.getCode(), is("queueEntry.endedAt.invalid"));
		assertThat(queueEntryStatusFieldError.getDefaultMessage(),
		    is("Queue entry endedAt should be greater than or equal to startedAt date (endedAt >= startedAt)"));
	}
	
	private Date yesterday() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -1);
		return calendar.getTime();
	}
}
