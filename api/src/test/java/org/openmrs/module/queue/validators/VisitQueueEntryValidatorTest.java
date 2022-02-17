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

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.utils.QueueValidationUtils;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { SpringTestConfiguration.class }, inheritLocations = false)
public class VisitQueueEntryValidatorTest extends BaseModuleContextSensitiveTest {
	
	private static final String VALID_CONCEPT_STATUS_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252cc6";
	
	private static final String INVALID_STATUS_CONCEPT_UUID = "67b910bd-298c-4ecf-a632-661ae2f446op";
	
	//the order of the list is important!
	private static final List<String> VISIT_QUEUE_ENTRY_INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/VisitQueueEntryDaoTest_visitInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/visitQueueEntryDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/validators/QueueEntryValidatorTest_globalPropertyInitialDataset.xml");
	
	@Autowired
	private VisitQueueEntryValidator validator;
	
	@Before
	public void setup() {
		VISIT_QUEUE_ENTRY_INITIAL_DATASET_XML.forEach(this::executeDataSet);
	}
	
	@Test
	public void validatorNotNull() {
		assertNotNull(validator);
	}
	
	@Test
	public void shouldReturnTrueForValidStatusConcept() {
		Optional<QueueEntry> queueEntry = Context.getService(QueueEntryService.class)
		        .getQueueEntryByUuid(VALID_CONCEPT_STATUS_UUID);
		assertTrue(queueEntry.isPresent());
		QueueValidationUtils.isValidStatus(queueEntry.get().getStatus());
	}
	
	@Test
	public void shouldReturnFalseForInvalidStatusConcept() {
		Concept concept = Context.getConceptService().getConceptByUuid(INVALID_STATUS_CONCEPT_UUID);
		assertThat(concept, notNullValue());
		assertFalse(QueueValidationUtils.isValidStatus(concept));
	}
}
