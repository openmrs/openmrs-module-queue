/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class VisitWithQueueEntriesSaveHandlerTest extends BaseModuleContextSensitiveTest {
	
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
	
	private Date stopDate;
	
	@Autowired
	@Qualifier("queue.QueueEntryService")
	private QueueEntryService queueEntryService;
	
	@Autowired
	private VisitService visitService;
	
	@Before
	public void setup() {
		INITIAL_DATASET_XML.forEach(this::executeDataSet);
		queueEntry = queueEntryService.getQueueEntryById(3).get();
		visit = queueEntry.getVisit();
		stopDate = DateUtils.addHours(visit.getStartDatetime(), 12);
	}
	
	@Test
	public void shouldNotEndQueueEntriesIfVisitIsNotStopped() {
		assertNull(visit.getStopDatetime());
		assertNull(queueEntry.getEndedAt());
		visit = visitService.saveVisit(visit);
		queueEntry = queueEntryService.getQueueEntryById(queueEntry.getId()).get();
		assertNull(visit.getStopDatetime());
		assertNull(queueEntry.getEndedAt());
	}
	
	@Test
	public void shouldEndQueueEntriesIfVisitIsStopped() {
		assertNull(visit.getStopDatetime());
		assertNull(queueEntry.getEndedAt());
		visit.setStopDatetime(stopDate);
		visit = visitService.saveVisit(visit);
		queueEntry = queueEntryService.getQueueEntryById(queueEntry.getId()).get();
		assertThat(visit.getStopDatetime(), equalTo(stopDate));
		assertThat(queueEntry.getEndedAt(), equalTo(stopDate));
	}
	
}
