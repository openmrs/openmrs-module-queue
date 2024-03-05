/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.sort;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class SortWeightGeneratorTest extends BaseModuleContextSensitiveTest {
	
	private static final List<String> INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/VisitQueueEntryDaoTest_visitInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/validators/QueueEntryValidatorTest_globalPropertyInitialDataset.xml");
	
	@Autowired
	private QueueServicesWrapper services;
	
	@Before
	public void setup() {
		INITIAL_DATASET_XML.forEach(this::executeDataSet);
	}
	
	@Test
	public void shouldReturnConfiguredSortWeightGenerator() {
		SortWeightGenerator generator = services.getQueueEntryService().getSortWeightGenerator();
		assertThat(generator, notNullValue());
		assertThat(generator instanceof BasicPrioritySortWeightGenerator, equalTo(true));
	}
	
	@Test
	public void shouldReturnCorrectSortWeights() {
		SortWeightGenerator generator = services.getQueueEntryService().getSortWeightGenerator();
		assertThat(generator, notNullValue());
		
		{
			QueueEntry qe = services.getQueueEntryService().getQueueEntryById(1).get();
			assertThat(qe.getSortWeight(), equalTo(0.0));
			qe = services.getQueueEntryService().saveQueueEntry(qe);
			assertThat(services.getAllowedPriorities(qe.getQueue()).indexOf(qe.getPriority()), equalTo(0));
			assertThat(qe.getSortWeight(), equalTo(0.0));
		}
		{
			QueueEntry qe = services.getQueueEntryService().getQueueEntryById(2).get();
			assertThat(qe.getSortWeight(), equalTo(10.0));
			qe = services.getQueueEntryService().saveQueueEntry(qe);
			assertThat(services.getAllowedPriorities(qe.getQueue()).indexOf(qe.getPriority()), equalTo(1));
			assertThat(qe.getSortWeight(), equalTo(1.0));
		}
		{
			QueueEntry qe = services.getQueueEntryService().getQueueEntryById(3).get();
			assertThat(qe.getSortWeight(), equalTo(20.0));
			qe = services.getQueueEntryService().saveQueueEntry(qe);
			assertThat(services.getAllowedPriorities(qe.getQueue()).indexOf(qe.getPriority()), equalTo(0));
			assertThat(qe.getSortWeight(), equalTo(0.0));
		}
	}
}
