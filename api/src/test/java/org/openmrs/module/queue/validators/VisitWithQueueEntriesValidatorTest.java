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
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Visit;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class VisitWithQueueEntriesValidatorTest extends BaseModuleContextSensitiveTest {
	
	private static final List<String> INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/VisitQueueEntryDaoTest_visitInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/validators/QueueEntryValidatorTest_globalPropertyInitialDataset.xml");
	
	private Visit visit;
	
	private QueueEntry queueEntry;
	
	private Errors errors;
	
	@Autowired
	@Qualifier("queue.QueueEntryService")
	private QueueEntryService queueEntryService;
	
	@Autowired
	private VisitWithQueueEntriesValidator validator;
	
	@Before
	public void setup() {
		INITIAL_DATASET_XML.forEach(this::executeDataSet);
		queueEntry = queueEntryService.getQueueEntryById(3).get();
		visit = queueEntry.getVisit();
		errors = new BindException(visit, visit.getClass().getName());
	}
	
	@Test
	public void validatorNotNull() {
		assertNotNull(validator);
	}
	
	@Test
	public void shouldSupportVisit() {
		assertTrue(validator.supports(Visit.class));
	}
	
	@Test
	public void shouldNotRejectQueueEntryByDefault() {
		validator.validate(visit, errors);
		assertFalse(errors.hasErrors());
	}
	
	@Test
	public void shouldNotRejectIfQueueEntryStartedAtEqualsVisitStartDate() {
		visit.setStartDatetime(queueEntry.getStartedAt());
		validator.validate(visit, errors);
		assertFalse(errors.hasErrors());
	}
	
	@Test
	public void shouldNotRejectIfQueueEntryStartedAtEqualsVisitEndDate() {
		queueEntry.setEndedAt(queueEntry.getStartedAt());
		queueEntryService.saveQueueEntry(queueEntry);
		visit.setStopDatetime(queueEntry.getStartedAt());
		validator.validate(visit, errors);
		assertFalse(errors.hasErrors());
	}
	
	@Test
	public void shouldNotRejectIfQueueEntryEndedAtAtEqualsVisitEndDate() {
		queueEntry.setEndedAt(queueEntry.getStartedAt());
		queueEntryService.saveQueueEntry(queueEntry);
		visit.setStopDatetime(queueEntry.getEndedAt());
		validator.validate(visit, errors);
		assertFalse(errors.hasErrors());
	}
	
	@Test
	public void shouldRejectIfQueueEntryStartedBeforeVisitStartDate() {
		visit.setStartDatetime(DateUtils.addMilliseconds(queueEntry.getStartedAt(), 1));
		validator.validate(visit, errors);
		FieldError startDatetimeFieldError = errors.getFieldError("startDatetime");
		assertNotNull(startDatetimeFieldError);
		assertThat(startDatetimeFieldError.getCode(), is("queue.entry.error.cannotStartBeforeVisitStartDate"));
	}
	
	@Test
	public void shouldRejectIfQueueEntryStartedAfterVisitEndDate() {
		visit.setStopDatetime(DateUtils.addMilliseconds(queueEntry.getStartedAt(), -1));
		validator.validate(visit, errors);
		FieldError stopDatetimeFieldError = errors.getFieldError("stopDatetime");
		assertNotNull(stopDatetimeFieldError);
		assertThat(stopDatetimeFieldError.getCode(), is("queue.entry.error.cannotStartAfterVisitStopDate"));
	}
	
	@Test
	public void shouldRejectIfQueueEntryEndedAfterVisitEndDate() {
		queueEntry.setEndedAt(DateUtils.addHours(queueEntry.getStartedAt(), 1));
		queueEntryService.saveQueueEntry(queueEntry);
		visit.setStopDatetime(DateUtils.addMilliseconds(queueEntry.getEndedAt(), -1));
		validator.validate(visit, errors);
		FieldError stopDatetimeFieldError = errors.getFieldError("stopDatetime");
		assertNotNull(stopDatetimeFieldError);
		assertThat(stopDatetimeFieldError.getCode(), is("queue.entry.error.cannotEndAfterVisitStopDate"));
	}
	
	@Test
	public void shouldRejectIfQueueEntryNotEndedWhenVisitStopped() {
		visit.setStopDatetime(DateUtils.addHours(queueEntry.getStartedAt(), 1));
		validator.validate(visit, errors);
		FieldError stopDatetimeFieldError = errors.getFieldError("stopDatetime");
		assertNotNull(stopDatetimeFieldError);
		assertThat(stopDatetimeFieldError.getCode(), is("queue.entry.error.cannotEndAfterVisitStopDate"));
	}
	
}
