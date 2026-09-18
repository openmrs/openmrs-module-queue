/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.tasks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.openmrs.module.queue.QueueModuleConstants.AUTO_CLOSE_QUEUE_ENTRIES_AT_TIME;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.QueueService;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.scheduler.Task;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.scheduler.TaskFactory;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

/**
 * Runs the task the way the scheduler does and checks the entry it ends reaches the database. The
 * unit tests stub the services the task talks through, so this is what covers the search criteria
 * filtering as they assume and the write reaching the row.
 */
@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class AutoCloseQueueEntryTaskIntegrationTest extends BaseModuleContextSensitiveTest {
	
	private static final List<String> INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/VisitQueueEntryDaoTest_visitInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/validators/QueueEntryValidatorTest_globalPropertyInitialDataset.xml");
	
	private static final String PATIENT_UUID = "90b38324-e2fd-4feb-95b7-9e9a2a8876fg";
	
	private static final String STATUS_CONCEPT_UUID = "56b910bd-298c-4ecf-a632-661ae2f7865y";
	
	private static final String PRIORITY_CONCEPT_UUID = "90b910bd-298c-4ecf-a632-661ae2f446op";
	
	private static final String TEST_QUEUE_UUID = "5ob8gj90-9090-4kbc-80dc-2e5d30252bb3";
	
	@Autowired
	@Qualifier("queue.QueueEntryService")
	private QueueEntryService queueEntryService;
	
	@Autowired
	@Qualifier("queue.QueueService")
	private QueueService queueService;
	
	@Autowired
	private PatientService patientService;
	
	@Autowired
	private ConceptService conceptService;
	
	@Before
	public void setup() {
		INITIAL_DATASET_XML.forEach(this::executeDataSet);
	}
	
	@Test
	public void shouldEndAnActiveQueueEntryOnceTheCloseTimeHasPassed() throws Exception {
		Integer queueEntryId = activeQueueEntryStartedHoursAgo(3);
		Date closeTime = setCloseTimeToHoursAgo(1);
		
		task().execute();
		
		assertThat(reloadedEndedAt(queueEntryId), equalTo(closeTime));
	}
	
	@Test
	public void shouldLeaveAQueueEntryEndedByAnotherSessionAfterItWasLoadedAlone() throws Exception {
		Integer queueEntryId = activeQueueEntryStartedHoursAgo(3);
		Context.flushSession();
		Context.clearSession();
		// the copy a task holds, loaded before it works through its list
		QueueEntry loadedByTheTask = queueEntryService.getQueueEntryById(queueEntryId).get();
		// another session ends the entry the way a transition does: ended_at only, date_changed untouched
		Date transitionTime = DateUtils.truncate(DateUtils.addHours(new Date(), -2), Calendar.SECOND);
		Context.getAdministrationService()
		        .executeSQL("UPDATE queue_entry SET ended_at = '"
		                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(transitionTime) + "' WHERE queue_entry_id = "
		                + queueEntryId,
		            false);
		
		boolean closed = queueEntryService.closeQueueEntry(loadedByTheTask, DateUtils.addHours(new Date(), -1));
		
		assertThat(closed, is(false));
		assertThat(reloadedEndedAt(queueEntryId), equalTo(transitionTime));
	}
	
	@Test
	public void shouldNotEndAQueueEntryAfterItsVisitStopped() throws Exception {
		Date visitStoppedAt = DateUtils.truncate(DateUtils.addHours(new Date(), -2), Calendar.SECOND);
		Visit visit = Context.getVisitService().getVisit(101);
		Integer queueEntryId = activeQueueEntryStartedHoursAgo(3, visit);
		// stop the visit behind the module's save handler, as core's own stopVisits does
		Context.getAdministrationService()
		        .executeSQL("UPDATE visit SET date_stopped = '"
		                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(visitStoppedAt) + "' WHERE visit_id = 101",
		            false);
		Context.flushSession();
		Context.clearSession();
		setCloseTimeToHoursAgo(1);
		
		task().execute();
		
		assertThat(reloadedEndedAt(queueEntryId), equalTo(visitStoppedAt));
	}
	
	@Test
	public void shouldLeaveQueueEntriesAloneWhenNoCloseTimeIsConfigured() throws Exception {
		Integer queueEntryId = activeQueueEntryStartedHoursAgo(3);
		Context.getAdministrationService().setGlobalProperty(AUTO_CLOSE_QUEUE_ENTRIES_AT_TIME, "");
		
		task().execute();
		
		assertThat(reloadedEndedAt(queueEntryId), nullValue());
	}
	
	@Test
	public void shouldNotEndAQueueEntryAfterItsVisitStoppedWhileTheTaskWasRunning() throws Exception {
		Date visitStoppedAt = DateUtils.truncate(DateUtils.addHours(new Date(), -2), Calendar.SECOND);
		Visit visit = Context.getVisitService().getVisit(101);
		Integer queueEntryId = activeQueueEntryStartedHoursAgo(3, visit);
		Context.flushSession();
		Context.clearSession();
		// the copy a task holds, with its visit loaded into the session while the visit was still open
		QueueEntry loadedByTheTask = queueEntryService.getQueueEntryById(queueEntryId).get();
		// another session stops the visit while the task works through its list
		Context.getAdministrationService()
		        .executeSQL("UPDATE visit SET date_stopped = '"
		                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(visitStoppedAt) + "' WHERE visit_id = 101",
		            false);
		
		queueEntryService.closeQueueEntry(loadedByTheTask, DateUtils.addHours(new Date(), -1));
		
		assertThat(reloadedEndedAt(queueEntryId), equalTo(visitStoppedAt));
	}
	
	/**
	 * @return the task built the way the scheduler builds it, from the class name on the definition
	 */
	private Task task() throws Exception {
		TaskDefinition taskDefinition = new TaskDefinition();
		taskDefinition.setName("Auto Close Queue Entries");
		taskDefinition.setTaskClass(AutoCloseQueueEntryTask.class.getName());
		taskDefinition.setRepeatInterval(60L);
		Task task = TaskFactory.getInstance().createInstance(taskDefinition);
		task.initialize(taskDefinition);
		return task;
	}
	
	/**
	 * @return the close time the task will work from, truncated to the minute the property holds
	 */
	private Date setCloseTimeToHoursAgo(int hours) {
		Date closeTime = DateUtils.truncate(DateUtils.addHours(new Date(), -hours), Calendar.MINUTE);
		Context.getAdministrationService().setGlobalProperty(AUTO_CLOSE_QUEUE_ENTRIES_AT_TIME,
		    new SimpleDateFormat("HH:mm").format(closeTime));
		return closeTime;
	}
	
	private Integer activeQueueEntryStartedHoursAgo(int hours) {
		return activeQueueEntryStartedHoursAgo(hours, null);
	}
	
	private Integer activeQueueEntryStartedHoursAgo(int hours, Visit visit) {
		Queue queue = queueService.getQueueByUuid(TEST_QUEUE_UUID).orElse(null);
		Patient patient = patientService.getPatientByUuid(PATIENT_UUID);
		Concept status = conceptService.getConceptByUuid(STATUS_CONCEPT_UUID);
		Concept priority = conceptService.getConceptByUuid(PRIORITY_CONCEPT_UUID);
		
		QueueEntry queueEntry = new QueueEntry();
		queueEntry.setQueue(queue);
		queueEntry.setPatient(patient);
		queueEntry.setStatus(status);
		queueEntry.setPriority(priority);
		queueEntry.setStartedAt(DateUtils.addHours(new Date(), -hours));
		queueEntry.setVisit(visit);
		return queueEntryService.saveQueueEntry(queueEntry).getQueueEntryId();
	}
	
	/**
	 * @return the endedAt read back from the database rather than from the session the task used
	 */
	private Date reloadedEndedAt(Integer queueEntryId) {
		Context.flushSession();
		Context.clearSession();
		Date endedAt = queueEntryService.getQueueEntryById(queueEntryId).get().getEndedAt();
		// Hibernate hands back a java.sql.Timestamp, which is never equal to a java.util.Date
		return endedAt == null ? null : new Date(endedAt.getTime());
	}
}
