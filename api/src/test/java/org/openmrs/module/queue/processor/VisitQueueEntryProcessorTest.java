/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.processor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.VisitAttributeType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.customdatatype.datatype.FreeTextDatatype;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.QueueService;
import org.openmrs.module.queue.api.VisitQueueEntryService;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.VisitQueueEntry;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class VisitQueueEntryProcessorTest extends BaseModuleContextSensitiveTest {
	
	private static final List<String> QUEUE_INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/VisitQueueEntryDaoTest_visitInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/visitQueueEntryDaoTest_visitQueueNumberInitialDataset.xml",
	    "org/openmrs/module/queue/validators/QueueEntryValidatorTest_globalPropertyInitialDataset.xml");
	
	@Autowired
	AdministrationService adminService;
	
	@Autowired
	PatientService patientService;
	
	@Autowired
	VisitService visitService;
	
	@Autowired
	@Qualifier("queue.QueueService")
	QueueService queueService;
	
	@Autowired
	@Qualifier("queue.QueueEntryService")
	QueueEntryService queueEntryService;
	
	@Autowired
	@Qualifier("queue.VisitQueueEntryService")
	VisitQueueEntryService visitQueueEntryService;
	
	@Autowired
	TestPatientQueueNumberGenerator testPatientQueueNumberGenerator;
	
	@Autowired
	BasicPatientQueueNumberGenerator basicPatientQueueNumberGenerator;
	
	@Autowired
	VisitAttributeQueueNumberGenerator visitAttributeQueueNumberGenerator;
	
	@Autowired
	ConceptService conceptService;
	
	String visitQueueEntryUuid = "cd1d0825-71e6-11ee-af3b-0242ac120002";
	
	Patient patient;
	
	Visit visit;
	
	Queue queue;
	
	Concept waitingStatus;
	
	Concept emergencyPriority;
	
	@Before
	public void setup() {
		QUEUE_INITIAL_DATASET_XML.forEach(this::executeDataSet);
		patient = patientService.getPatient(100);
		visit = visitService.getVisit(101);
		queue = queueService.getQueueByUuid("3eb7fe43-2813-4kbc-80dc-2e5d30252bb5").get();
		waitingStatus = conceptService.getConcept(3001);
		emergencyPriority = conceptService.getConcept(1001);
		visitQueueEntryService.setVisitQueueEntryProcessor(testPatientQueueNumberGenerator);
		
		VisitAttributeType vat = new VisitAttributeType();
		vat.setName("Test");
		vat.setDatatypeClassname(FreeTextDatatype.class.getName());
		vat = visitService.saveVisitAttributeType(vat);
		adminService.setGlobalProperty("queue.visitQueueNumberAttributeUuid", vat.getUuid());
	}
	
	private VisitQueueEntry newVisitQueueEntry() {
		QueueEntry queueEntry = new QueueEntry();
		queueEntry.setUuid("5kb8fe43-2813-4kbc-80dc-2e5d30252cc87");
		queueEntry.setPatient(patient);
		queueEntry.setQueue(queue);
		queueEntry.setStartedAt(new Date());
		queueEntry.setStatus(waitingStatus);
		queueEntry.setPriority(emergencyPriority);
		VisitQueueEntry visitQueueEntry = new VisitQueueEntry();
		visitQueueEntry.setUuid(visitQueueEntryUuid);
		visitQueueEntry.setQueueEntry(queueEntry);
		visitQueueEntry.setVisit(visit);
		return visitQueueEntry;
	}
	
	@Test
	public void shouldNotGenerateAVisitNumberIfNoProcessorsAreSpecified() {
		testPatientQueueNumberGenerator.setProcessorToUse(null);
		visitQueueEntryService.createVisitQueueEntry(newVisitQueueEntry());
		VisitQueueEntry visitQueueEntry = visitQueueEntryService.getVisitQueueEntryByUuid(visitQueueEntryUuid).get();
		assertThat(visitQueueEntry.getQueueEntry().getPatientQueueNumber(), nullValue());
	}
	
	@Test
	public void shouldNotOverwriteAVisitNumberIfNoProcessorsAreSpecified() {
		testPatientQueueNumberGenerator.setProcessorToUse(null);
		VisitQueueEntry visitQueueEntry = newVisitQueueEntry();
		visitQueueEntry.getQueueEntry().setPatientQueueNumber("A1000");
		visitQueueEntryService.createVisitQueueEntry(visitQueueEntry);
		visitQueueEntry = visitQueueEntryService.getVisitQueueEntryByUuid(visitQueueEntryUuid).get();
		assertThat(visitQueueEntry.getQueueEntry().getPatientQueueNumber(), equalTo("A1000"));
	}
	
	@Test
	public void shouldGenerateABasicVisitNumberIfProcessorIsSpecified() {
		testPatientQueueNumberGenerator.setProcessorToUse(basicPatientQueueNumberGenerator);
		visitQueueEntryService.createVisitQueueEntry(newVisitQueueEntry());
		VisitQueueEntry visitQueueEntry = visitQueueEntryService.getVisitQueueEntryByUuid(visitQueueEntryUuid).get();
		assertThat(visitQueueEntry.getQueueEntry().getPatientQueueNumber(),
		    equalTo(Integer.toString(visitQueueEntry.getId())));
	}
	
	@Test
	public void shouldGenerateAVisitAttributeVisitNumberIfProcessorIsSpecified() {
		testPatientQueueNumberGenerator.setProcessorToUse(visitAttributeQueueNumberGenerator);
		visitQueueEntryService.createVisitQueueEntry(newVisitQueueEntry());
		VisitQueueEntry visitQueueEntry = visitQueueEntryService.getVisitQueueEntryByUuid(visitQueueEntryUuid).get();
		assertThat(visitQueueEntry.getQueueEntry().getPatientQueueNumber(), equalTo("TRI-002"));
		assertThat(visitQueueEntry.getVisit().getAttributes().iterator().next().getValueReference(), equalTo("TRI-002"));
	}
	
	@Test
	public void shouldGenerateVisitAttributeVisitNumberForConsultation() {
		QueueEntry queueEntry = queueEntryService.getQueueEntryByUuid("7ub8fe43-2813-4kbc-80dc-2e5d30252cc5").get();
		assertThat(queueEntry.getQueue().getLocation().getUuid(), equalTo("d0938432-1691-11df-97a5-7038c098"));
		Visit visit = new Visit();
		visit.setPatient(queueEntry.getPatient());
		VisitQueueEntry visitQueueEntry = new VisitQueueEntry();
		visitQueueEntry.setQueueEntry(queueEntry);
		visitQueueEntry.setVisit(visit);
		visitAttributeQueueNumberGenerator.beforeSaveVisitQueueEntry(visitQueueEntry);
		String queueNumber = visitQueueEntry.getVisit().getAttributes().iterator().next().getValueReference();
		assertThat(queueNumber, notNullValue());
		assertThat(queueNumber, equalTo("CON-001"));
	}
}
