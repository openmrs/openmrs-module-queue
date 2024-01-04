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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class QueueValidatorTest extends BaseModuleContextSensitiveTest {
	
	private static final String[] QUEUE_VALIDATOR_INITIAL_DATASET_XML = {
	        "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	        "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	        "org/openmrs/module/queue/validators/QueueEntryValidatorTest_globalPropertyInitialDataset.xml" };
	
	private static final String BAD_LOCATION_UUID = "60938432-1691-11df-97a5-7038c0lk";
	
	private static final String LOCATION_UUID = "d0938432-1691-11df-97a5-7038c098";
	
	private static final String VALID_SERVICE_CONCEPT = "67b910bd-298c-4ecf-a632-661ae2f446op";
	
	private static final String INVALID_SERVICE_CONCEPT_UUID = "91b910bd-298c-4ecf-a632-661ae2f909ut";
	
	@Autowired
	private QueueValidator validator;
	
	private Queue queue;
	
	private Errors errors;
	
	@Before
	public void setup() {
		for (String dataset : QUEUE_VALIDATOR_INITIAL_DATASET_XML) {
			executeDataSet(dataset);
		}
		queue = new Queue();
		errors = new BindException(queue, queue.getClass().getName());
	}
	
	@Test
	public void shouldSucceedForValidServiceConcept() {
		queue.setName("Test Queue");
		queue.setLocation(Context.getLocationService().getLocationByUuid(LOCATION_UUID));
		queue.setService(Context.getConceptService().getConceptByUuid(VALID_SERVICE_CONCEPT));
		validator.validate(queue, errors);
		assertThat(errors.getAllErrors().size(), equalTo(0));
	}
	
	@Test
	public void shouldFailForInvalidServiceConcept() {
		queue.setName("Test Queue");
		queue.setLocation(Context.getLocationService().getLocationByUuid(LOCATION_UUID));
		queue.setService(Context.getConceptService().getConceptByUuid(INVALID_SERVICE_CONCEPT_UUID));
		validator.validate(queue, errors);
		assertThat(errors.getAllErrors().size(), equalTo(1));
	}
	
	@Test
	public void shouldSucceedForValidLocation() {
		queue.setName("Test Queue");
		queue.setLocation(Context.getLocationService().getLocationByUuid(LOCATION_UUID));
		queue.setService(Context.getConceptService().getConceptByUuid(VALID_SERVICE_CONCEPT));
		validator.validate(queue, errors);
		assertThat(errors.getAllErrors().size(), equalTo(0));
	}
	
	@Test
	public void shouldFailForInvalidLocation() {
		queue.setName("Test Queue");
		queue.setLocation(Context.getLocationService().getLocationByUuid(BAD_LOCATION_UUID));
		queue.setService(Context.getConceptService().getConceptByUuid(VALID_SERVICE_CONCEPT));
		validator.validate(queue, errors);
		assertThat(errors.getAllErrors().size(), equalTo(1));
	}
}
