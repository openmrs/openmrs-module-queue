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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class PatientWithQueueEntriesVoidHandlerTest extends BaseModuleContextSensitiveTest {
	
	private static final List<String> INITIAL_DATASET_XML = Arrays.asList(
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_conceptsInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_patientInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/VisitQueueEntryDaoTest_visitInitialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_initialDataset.xml",
	    "org/openmrs/module/queue/validators/QueueEntryValidatorTest_globalPropertyInitialDataset.xml");
	
	private static final String OVERLAPPING_ENTRIES_DATASET_XML = "org/openmrs/module/queue/api/dao/QueueEntryDaoTest_overlappingEntriesInitialDataset.xml";
	
	// Patient 100 owns queue entries 1 (ended), 2 (active), 3 (active) and 10 (already voided)
	private static final int PATIENT_ID = 100;
	
	// Patient 2 owns queue entry 4 and must be unaffected
	private static final int OTHER_PATIENT_ID = 2;
	
	@Autowired
	@Qualifier("queue.QueueEntryService")
	private QueueEntryService queueEntryService;
	
	@Autowired
	private PatientService patientService;
	
	@Autowired
	private VisitService visitService;
	
	private Patient patient;
	
	@BeforeEach
	public void setup() {
		INITIAL_DATASET_XML.forEach(this::executeDataSet);
		patient = patientService.getPatient(PATIENT_ID);
	}
	
	@Test
	public void shouldVoidQueueEntriesWhenPatientIsVoided() {
		QueueEntrySearchCriteria activeForPatient = new QueueEntrySearchCriteria();
		activeForPatient.setPatient(patient);
		activeForPatient.setIsEnded(false);
		assertThat(queueEntryService.getQueueEntries(activeForPatient), hasSize(2));
		
		String voidReason = "for testing";
		patient = patientService.voidPatient(patient, voidReason);
		assertThat(patient.getVoided(), is(true));
		assertThat(patient.getDateVoided(), notNullValue());
		
		patient = patientService.getPatient(PATIENT_ID);
		
		for (int id : new int[] { 1, 2, 3 }) {
			QueueEntry qe = queueEntryService.getQueueEntryById(id).get();
			assertThat("queue entry " + id + " should be voided", qe.getVoided(), is(true));
			assertThat(qe.getVoidReason(), equalTo(voidReason));
			assertThat(qe.getDateVoided().getTime(), equalTo(patient.getDateVoided().getTime()));
			assertThat(qe.getVoidedBy(), equalTo(patient.getVoidedBy()));
		}
		
		activeForPatient.setIncludedVoided(true);
		List<QueueEntry> remaining = queueEntryService.getQueueEntries(activeForPatient);
		assertThat(remaining.stream().map(QueueEntry::getId).collect(Collectors.toList()), containsInAnyOrder(2, 3, 10));
		assertThat(remaining.stream().allMatch(QueueEntry::getVoided), is(true));
	}
	
	@Test
	public void shouldVoidOverlappingQueueEntriesInTheSameQueue() {
		executeDataSet(OVERLAPPING_ENTRIES_DATASET_XML);
		
		patient = patientService.voidPatient(patient, "for testing");
		
		assertThat(queueEntryService.getQueueEntryById(3).get().getVoided(), is(true));
		assertThat(queueEntryService.getQueueEntryById(12).get().getVoided(), is(true));
		assertThat(patientService.getPatient(PATIENT_ID).getVoided(), is(true));
	}
	
	@Test
	public void shouldNotVoidQueueEntriesOfOtherPatients() {
		patientService.voidPatient(patient, "for testing");
		QueueEntry other = queueEntryService.getQueueEntryById(4).get();
		assertThat(other.getPatient().getPatientId(), equalTo(OTHER_PATIENT_ID));
		assertThat(other.getVoided(), is(false));
	}
	
	@Test
	public void shouldNotChangeAlreadyVoidedQueueEntries() {
		QueueEntry alreadyVoided = queueEntryService.getQueueEntryById(10).get();
		assertThat(alreadyVoided.getVoided(), is(true));
		Date originalDateVoided = alreadyVoided.getDateVoided();
		String originalVoidReason = alreadyVoided.getVoidReason();
		
		patientService.voidPatient(patient, "for testing");
		
		alreadyVoided = queueEntryService.getQueueEntryById(10).get();
		assertThat(alreadyVoided.getVoided(), is(true));
		assertThat(alreadyVoided.getDateVoided(), equalTo(originalDateVoided));
		assertThat(alreadyVoided.getVoidReason(), equalTo(originalVoidReason));
	}
	
	@Test
	public void shouldDoNothingForPatientWithoutQueueEntries() {
		// Patient 6 comes from the standard test dataset and has no queue entries
		Patient noEntries = patientService.getPatient(6);
		QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
		criteria.setPatient(noEntries);
		criteria.setIncludedVoided(true);
		assertThat(queueEntryService.getQueueEntries(criteria), hasSize(0));
		
		Patient voided = patientService.voidPatient(noEntries, "for testing");
		
		assertThat(voided.getVoided(), is(true));
		assertThat(queueEntryService.getQueueEntries(criteria), hasSize(0));
	}
	
	@Test
	public void shouldVoidQueueEntriesWhenPatientIsMergedIntoAnother() throws Exception {
		// Core moves the non-preferred patient's visits to the preferred patient, then voids the non-preferred one
		Patient preferred = patientService.getPatient(OTHER_PATIENT_ID);
		patientService.mergePatients(preferred, patient);
		
		Context.flushSession();
		Context.clearSession();
		
		assertThat(patientService.getPatient(PATIENT_ID).getVoided(), is(true));
		for (int id : new int[] { 1, 2, 3 }) {
			QueueEntry qe = queueEntryService.getQueueEntryById(id).get();
			assertThat("queue entry " + id + " should be voided", qe.getVoided(), is(true));
			assertThat(qe.getVoidReason(), equalTo("Merged with patient #" + OTHER_PATIENT_ID));
		}
	}
	
	@Test
	public void shouldUnvoidQueueEntriesWhenPatientIsUnvoided() {
		patientService.voidPatient(patient, "for testing");
		Context.flushSession();
		Context.clearSession();
		patient = patientService.getPatient(PATIENT_ID);
		assertThat(patient.getVoided(), is(true));
		for (int id : new int[] { 1, 2, 3 }) {
			assertThat(queueEntryService.getQueueEntryById(id).get().getVoided(), is(true));
		}
		
		patient = patientService.unvoidPatient(patient);
		Context.flushSession();
		Context.clearSession();
		
		assertThat(patientService.getPatient(PATIENT_ID).getVoided(), is(false));
		for (int id : new int[] { 1, 2, 3 }) {
			QueueEntry qe = queueEntryService.getQueueEntryById(id).get();
			assertThat("queue entry " + id + " should be unvoided", qe.getVoided(), is(false));
			assertThat(qe.getVoidReason(), nullValue());
			assertThat(qe.getDateVoided(), nullValue());
			assertThat(qe.getVoidedBy(), nullValue());
		}
		
		// Entries are visible again in the default search
		QueueEntrySearchCriteria activeForPatient = new QueueEntrySearchCriteria();
		activeForPatient.setPatient(patientService.getPatient(PATIENT_ID));
		activeForPatient.setIsEnded(false);
		assertThat(
		    queueEntryService.getQueueEntries(activeForPatient).stream().map(QueueEntry::getId).collect(Collectors.toList()),
		    containsInAnyOrder(2, 3));
	}
	
	@Test
	public void shouldNotUnvoidQueueEntriesThatWereVoidedIndependently() {
		// Entry 10 was voided on its own in 2022, long before the patient is voided here
		patientService.voidPatient(patient, "for testing");
		Context.flushSession();
		Context.clearSession();
		
		patientService.unvoidPatient(patientService.getPatient(PATIENT_ID));
		Context.flushSession();
		Context.clearSession();
		
		QueueEntry independentlyVoided = queueEntryService.getQueueEntryById(10).get();
		assertThat(independentlyVoided.getVoided(), is(true));
		assertThat(independentlyVoided.getDateVoided(), notNullValue());
	}
	
	@Test
	public void shouldUnvoidQueueEntriesThatWouldNoLongerPassValidation() {
		patientService.voidPatient(patient, "for testing");
		Context.flushSession();
		Context.clearSession();
		
		// Stop visit 102 while entry 3 stays open on it, as AutoCloseVisitsTask does when it bypasses the
		// visit handler. An open entry on a stopped visit fails QueueEntryValidator.
		Visit visit = visitService.getVisit(102);
		visit.setStopDatetime(new Date());
		visitService.saveVisit(visit);
		Context.flushSession();
		Context.clearSession();
		assertThat(queueEntryService.getQueueEntryById(3).get().getEndedAt(), nullValue());
		
		// Let any exception escape: the handler flips voided=false on the managed entity before saving, so
		// asserting on the entry alone would pass even if the unvoid blew up
		Patient unvoided = patientService.unvoidPatient(patientService.getPatient(PATIENT_ID));
		Context.flushSession();
		Context.clearSession();
		
		assertThat(unvoided.getVoided(), is(false));
		assertThat(patientService.getPatient(PATIENT_ID).getVoided(), is(false));
		assertThat(queueEntryService.getQueueEntryById(3).get().getVoided(), is(false));
	}
	
	@Test
	public void shouldNotUnvoidQueueEntriesWhoseVisitIsStillVoided() {
		// REST deletes a patient by voiding every visit first, so the visit cascade voids the entries and
		// the patient cascade finds nothing left to do
		for (int visitId : new int[] { 101, 102 }) {
			Visit visit = visitService.getVisit(visitId);
			visit.setVoided(true);
			visit.setVoidReason("for testing");
			visitService.saveVisit(visit);
		}
		patientService.voidPatient(patient, "for testing");
		Context.flushSession();
		Context.clearSession();
		patient = patientService.getPatient(PATIENT_ID);
		
		// Both stamps are taken within the same request and the column holds whole seconds, so in
		// production they read back equal; force that here rather than depend on the clock
		for (int id : new int[] { 1, 2, 3 }) {
			QueueEntry qe = queueEntryService.getQueueEntryById(id).get();
			assertThat(qe.getVoided(), is(true));
			qe.setDateVoided(patient.getDateVoided());
			qe.setVoidedBy(patient.getVoidedBy());
			queueEntryService.saveQueueEntry(qe);
		}
		Context.flushSession();
		Context.clearSession();
		
		patientService.unvoidPatient(patientService.getPatient(PATIENT_ID));
		Context.flushSession();
		Context.clearSession();
		
		assertThat(patientService.getPatient(PATIENT_ID).getVoided(), is(false));
		for (int id : new int[] { 1, 2, 3 }) {
			QueueEntry qe = queueEntryService.getQueueEntryById(id).get();
			assertThat("queue entry " + id + " should stay voided with its visit", qe.getVoided(), is(true));
			assertThat(qe.getVisit().getVoided(), is(true));
		}
		QueueEntrySearchCriteria active = new QueueEntrySearchCriteria();
		active.setPatient(patientService.getPatient(PATIENT_ID));
		active.setIsEnded(false);
		assertThat(queueEntryService.getQueueEntries(active), hasSize(0));
	}
	
	@Test
	public void shouldVoidQueueEntriesForUserWithoutQueuePrivileges() {
		asUserWithoutQueuePrivileges(() -> patientService.voidPatient(patient, "for testing"),
		    org.openmrs.util.PrivilegeConstants.DELETE_PATIENTS, org.openmrs.util.PrivilegeConstants.GET_PATIENTS,
		    org.openmrs.util.PrivilegeConstants.GET_ENCOUNTERS, org.openmrs.util.PrivilegeConstants.GET_USERS,
		    org.openmrs.util.PrivilegeConstants.GET_PATIENT_COHORTS);
		
		assertThat(patientService.getPatient(PATIENT_ID).getVoided(), is(true));
		for (int id : new int[] { 1, 2, 3 }) {
			assertThat(queueEntryService.getQueueEntryById(id).get().getVoided(), is(true));
		}
	}
	
	@Test
	public void shouldUnvoidQueueEntriesForUserWithoutQueuePrivileges() {
		patientService.voidPatient(patient, "for testing");
		Context.flushSession();
		Context.clearSession();
		Patient voided = patientService.getPatient(PATIENT_ID);
		
		// core re-validates the patient on unvoid, and its validators read global properties
		asUserWithoutQueuePrivileges(() -> patientService.unvoidPatient(voided),
		    org.openmrs.util.PrivilegeConstants.DELETE_PATIENTS, org.openmrs.util.PrivilegeConstants.GET_PATIENTS,
		    org.openmrs.util.PrivilegeConstants.GET_ENCOUNTERS, org.openmrs.util.PrivilegeConstants.GET_ORDERS,
		    org.openmrs.util.PrivilegeConstants.GET_USERS, org.openmrs.util.PrivilegeConstants.EDIT_COHORTS,
		    org.openmrs.util.PrivilegeConstants.EDIT_PATIENTS, org.openmrs.util.PrivilegeConstants.GET_GLOBAL_PROPERTIES);
		
		assertThat(patientService.getPatient(PATIENT_ID).getVoided(), is(false));
		for (int id : new int[] { 1, 2, 3 }) {
			assertThat(queueEntryService.getQueueEntryById(id).get().getVoided(), is(false));
		}
	}
	
	@Test
	public void shouldDoNothingWithoutQueuePrivilegesForPatientWithoutQueueEntries() {
		Patient noEntries = patientService.getPatient(6);
		asUserWithoutQueuePrivileges(() -> patientService.voidPatient(noEntries, "for testing"),
		    org.openmrs.util.PrivilegeConstants.DELETE_PATIENTS, org.openmrs.util.PrivilegeConstants.GET_PATIENTS,
		    org.openmrs.util.PrivilegeConstants.GET_ENCOUNTERS, org.openmrs.util.PrivilegeConstants.GET_USERS,
		    org.openmrs.util.PrivilegeConstants.GET_PATIENT_COHORTS);
		assertThat(patientService.getPatient(6).getVoided(), is(true));
	}
	
	/**
	 * Runs the action as "butch" from the standard test dataset, who has the Provider role and no
	 * privileges, proxying only the given core privileges. Queue privileges are deliberately absent, so
	 * the handlers must obtain them on their own.
	 */
	private void asUserWithoutQueuePrivileges(Runnable action, String... corePrivileges) {
		Context.becomeUser("3-4");
		for (String privilege : corePrivileges) {
			Context.addProxyPrivilege(privilege);
		}
		try {
			assertThat(Context.hasPrivilege(org.openmrs.module.queue.utils.PrivilegeConstants.GET_QUEUE_ENTRIES), is(false));
			assertThat(Context.hasPrivilege(org.openmrs.module.queue.utils.PrivilegeConstants.MANAGE_QUEUE_ENTRIES),
			    is(false));
			action.run();
		}
		finally {
			for (String privilege : corePrivileges) {
				Context.removeProxyPrivilege(privilege);
			}
			authenticate();
		}
	}
}
