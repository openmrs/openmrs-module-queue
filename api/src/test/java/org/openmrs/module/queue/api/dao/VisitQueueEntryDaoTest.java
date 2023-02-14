/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Visit;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.VisitQueueEntry;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@Slf4j
@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class VisitQueueEntryDaoTest extends BaseModuleContextSensitiveTest {
	
	private static final String VISIT_QUEUE_ENTRY_UUID = "5eb8fe43-2813-4kbc-80dc-2e5d30252cc3";
	
	private static final String NEW_VISIT_QUEUE_ENTRY_UUID = "7eb6fe43-2813-4kbc-80dc-2e5d30252kk9";
	
	private static final String VOIDED_VISIT_QUEUE_ENTRY_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252cc6";
	
	private static final String QUEUE_ENTRY_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252cc6";
	
	private static final String VISIT_UUID = "j848b0c0-1ade-11e1-9c71-00248140a6eb";
	
	private static final String WAITING_FOR_STATUS = "Waiting for service";
	
	private static final String IN_SERVICE_STATUS = "In service";
	
	private static final String TRIAGE_SERVICE = "Triage";
	
	private static final String CONSULTATION_SERVICE = "Consultation";
	
	private static final String LOCATION_UUID = "d0938432-1691-11df-97a5-7038c098";
	
	@Autowired
	@Qualifier("visitQueueEntryDao")
	private VisitQueueEntryDao<VisitQueueEntry> dao;
	
	//the order of the list is important!
	private static final List<String> VISIT_QUEUE_ENTRY_INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/VisitQueueEntryDaoTest_visitInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/visitQueueEntryDaoTest_initialDataset.xml");
	
	@Before
	@SkipBaseSetup
	public void setup() {
		VISIT_QUEUE_ENTRY_INITIAL_DATASET_XML.forEach(this::executeDataSet);
	}
	
	@Test
	public void shouldGetVisitQueueEntryByUuid() {
		Optional<VisitQueueEntry> result = dao.get(VISIT_QUEUE_ENTRY_UUID);
		
		assertThat(result, notNullValue());
		assertThat(result.isPresent(), is(true));
		assertThat(result.get().getUuid(), is(VISIT_QUEUE_ENTRY_UUID));
	}
	
	@Test
	public void shouldReturnNullForVoidedVisitQueueEntry() {
		Optional<VisitQueueEntry> visitQueueEntry = dao.get(VOIDED_VISIT_QUEUE_ENTRY_UUID);
		assertThat(visitQueueEntry.isPresent(), is(false));
	}
	
	@Test
	public void shouldCreateNewVisitQueryEntryRecord() {
		VisitQueueEntry visitQueueEntry = new VisitQueueEntry();
		visitQueueEntry.setUuid(NEW_VISIT_QUEUE_ENTRY_UUID);
		Optional<QueueEntry> queueEntryOptional = Context.getService(QueueEntryService.class)
		        .getQueueEntryByUuid(QUEUE_ENTRY_UUID);
		assertThat(queueEntryOptional.isPresent(), is(true));
		queueEntryOptional.ifPresent(visitQueueEntry::setQueueEntry);
		
		Visit visit = Context.getVisitService().getVisitByUuid(VISIT_UUID);
		assertThat(visit, notNullValue());
		visitQueueEntry.setVisit(visit);
		visitQueueEntry.setDateCreated(new Date());
		
		VisitQueueEntry result = dao.createOrUpdate(visitQueueEntry);
		assertThat(result, notNullValue());
		assertThat(result.getUuid(), is(NEW_VISIT_QUEUE_ENTRY_UUID));
		assertThat(result.getQueueEntry(), notNullValue());
		assertThat(result.getQueueEntry().getUuid(), is(QUEUE_ENTRY_UUID));
	}
	
	@Test
	public void shouldUpdateAnExistingVisitQueueEntryRecord() {
		Optional<VisitQueueEntry> existingVisitQueueEntry = dao.get(VISIT_QUEUE_ENTRY_UUID);
		assertThat(existingVisitQueueEntry.isPresent(), is(true));
		
		//Update the existing visit_queue_entry record
		existingVisitQueueEntry.ifPresent(visitQueueEntry -> {
			visitQueueEntry.setVoided(true);
			visitQueueEntry.setDateVoided(new Date());
			visitQueueEntry.setVoidReason("Testing update operation");
			dao.createOrUpdate(visitQueueEntry);
		});
		
		//Get the updated visit_queue_entry
		Optional<VisitQueueEntry> updatedVisitQueueEntry = dao.get(VISIT_QUEUE_ENTRY_UUID);
		//If false - update was successful
		assertThat(updatedVisitQueueEntry.isPresent(), is(false));
		//Get the voided visit queue entry record
		Optional<VisitQueueEntry> alreadyVoidedVisitQueueEntryRecord = dao.get(existingVisitQueueEntry.get().getId());
		assertThat(alreadyVoidedVisitQueueEntryRecord.isPresent(), is(true));
		assertThat(alreadyVoidedVisitQueueEntryRecord.get().getVoided(), is(true));
		assertThat(alreadyVoidedVisitQueueEntryRecord.get().getVoidReason(), is("Testing update operation"));
	}
	
	@Test
	public void shouldFindAllVisitQueueEntries() {
		Collection<VisitQueueEntry> visitQueueEntries = dao.findAll();
		assertThat(visitQueueEntries.isEmpty(), is(false));
		assertThat(visitQueueEntries, hasSize(2));
	}
	
	@Test
	public void shouldFindAllVisitQueueEntriesIncludingRetired() {
		Collection<VisitQueueEntry> visitQueueEntries = dao.findAll(true);
		assertThat(visitQueueEntries.isEmpty(), is(false));
		assertThat(visitQueueEntries, hasSize(3));
	}
	
	@Test
	public void shouldDeleteVisitQueueEntryByUuid() {
		dao.delete(VISIT_QUEUE_ENTRY_UUID);
		
		Optional<VisitQueueEntry> result = dao.get(VISIT_QUEUE_ENTRY_UUID);
		//verify delete operation
		assertThat(result.isPresent(), is(false));
	}
	
	@Test
	public void shouldDeleteVisitQueueEntryByEntity() {
		dao.get(VISIT_QUEUE_ENTRY_UUID).ifPresent((queueEntry) -> dao.delete(queueEntry));
		
		Optional<VisitQueueEntry> result = dao.get(VISIT_QUEUE_ENTRY_UUID);
		//verify delete operation
		assertThat(result.isPresent(), is(false));
	}
	
	@Test
	public void shouldFindVisitQueueEntriesByWaitingForStatusAndService() {
		Collection<VisitQueueEntry> result = dao.findVisitQueueEntriesByConceptStatusAndConceptService(WAITING_FOR_STATUS,
		    TRIAGE_SERVICE, ConceptNameType.FULLY_SPECIFIED, false, null);
		
		assertThat(result, notNullValue());
		assertThat(result, hasSize(1));
	}
	
	@Test
	public void shouldFindVisitQueueEntriesByInServiceStatusAndService() {
		Collection<VisitQueueEntry> result = dao.findVisitQueueEntriesByConceptStatusAndConceptService(IN_SERVICE_STATUS,
		    CONSULTATION_SERVICE, ConceptNameType.FULLY_SPECIFIED, false, null);
		
		assertThat(result, notNullValue());
		assertThat(result, hasSize(1));
	}
	
	@Test
	public void shouldFilterVisitQueueEntriesByInServiceStatus() {
		Collection<VisitQueueEntry> result = dao.findVisitQueueEntriesByConceptStatusAndConceptService(IN_SERVICE_STATUS,
		    null, ConceptNameType.FULLY_SPECIFIED, false, null);
		
		assertThat(result, notNullValue());
		assertThat(result, hasSize(1));
	}
	
	@Test
	public void shouldFilterVisitQueueEntriesByConsultationService() {
		Collection<VisitQueueEntry> result = dao.findVisitQueueEntriesByConceptStatusAndConceptService(null,
		    CONSULTATION_SERVICE, ConceptNameType.FULLY_SPECIFIED, false, null);
		
		assertThat(result, notNullValue());
		assertThat(result, hasSize(1));
	}
	
	@Test
	public void shouldFilterVisitQueueEntriesByLocation() {
		Collection<VisitQueueEntry> result = dao.findVisitQueueEntriesByConceptStatusAndConceptService(null, null, null,
		    false, LOCATION_UUID);
		
		assertThat(result, notNullValue());
		assertThat(result, hasSize(2));
	}
	
	@Test
	public void shouldNotFilterVisitQueueEntriesByServiceAndStatusIfBothAreNull() {
		Collection<VisitQueueEntry> result = dao.findVisitQueueEntriesByConceptStatusAndConceptService(null, null,
		    ConceptNameType.FULLY_SPECIFIED, false, null);
		
		assertThat(result, notNullValue());
		assertThat(result, hasSize(2));
	}
	
	@Test
	public void shouldGetCountOfVisitQueueEntriesByInServiceStatus() {
		Long result = dao.getVisitQueueEntriesCountByLocationStatusAndService(IN_SERVICE_STATUS, null,
		    ConceptNameType.FULLY_SPECIFIED, false, null);
		
		assertThat(result, notNullValue());
		assertThat(result, is(1L));
	}
	
	@Test
	public void shouldGetCountOfVisitQueueEntriesByConsultationService() {
		Long result = dao.getVisitQueueEntriesCountByLocationStatusAndService(null, CONSULTATION_SERVICE,
		    ConceptNameType.FULLY_SPECIFIED, false, null);
		
		assertThat(result, notNullValue());
		assertThat(result, is(1L));
	}
	
	@Test
	public void shouldGetCountOfVisitQueueEntriesByLocation() {
		Long result = dao.getVisitQueueEntriesCountByLocationStatusAndService(null, null, null, false, LOCATION_UUID);
		
		assertThat(result, notNullValue());
		assertThat(result, is(2L));
	}
	
	@Test
	public void shouldGetCountOfVisitQueueEntriesByServiceAndStatus() {
		Long result = dao.getVisitQueueEntriesCountByLocationStatusAndService(WAITING_FOR_STATUS, CONSULTATION_SERVICE,
		    ConceptNameType.FULLY_SPECIFIED, false, null);
		
		assertThat(result, notNullValue());
		assertThat(result, is(0L));
	}
}
