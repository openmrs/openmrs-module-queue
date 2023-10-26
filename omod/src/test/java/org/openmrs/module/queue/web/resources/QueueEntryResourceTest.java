/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.web.resources;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_ENDED_ON_OR_AFTER;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_ENDED_ON_OR_BEFORE;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_HAS_VISIT;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_INCLUDE_VOIDED;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_IS_ENDED;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_LOCATION;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_LOCATION_WAITING_FOR;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_PATIENT;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_PRIORITY;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_PROVIDER_WAITING_FOR;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_QUEUE;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_QUEUE_COMING_FROM;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_SERVICE;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_STARTED_ON_OR_AFTER;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_STARTED_ON_OR_BEFORE;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_STATUS;
import static org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser.SEARCH_PARAM_VISIT;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import javax.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.QueueRoomService;
import org.openmrs.module.queue.api.QueueService;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.RoomProviderMapService;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.utils.QueueEntrySearchCriteria;
import org.openmrs.module.queue.utils.QueueUtils;
import org.openmrs.module.queue.web.QueueEntrySearchCriteriaParser;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, RestUtil.class })
public class QueueEntryResourceTest extends BaseQueueResourceTest<QueueEntry, QueueEntryResource> {
	
	private static final String QUEUE_ENTRY_UUID = "6hje567a-fca0-11e5-9e59-08002719a7";
	
	private QueueEntryResource resource;
	
	private QueueEntry queueEntry;
	
	@Mock
	private QueueService queueService;
	
	@Mock
	private QueueEntryService queueEntryService;
	
	@Mock
	private QueueRoomService queueRoomService;
	
	@Mock
	private RoomProviderMapService roomProviderMapService;
	
	@Mock
	private ConceptService conceptService;
	
	@Mock
	private LocationService locationService;
	
	@Mock
	private PatientService patientService;
	
	@Mock
	private QueueServicesWrapper queueServicesWrapper;
	
	RequestContext requestContext;
	
	HttpServletRequest request;
	
	Map<String, String[]> parameterMap;
	
	ArgumentCaptor<QueueEntrySearchCriteria> queueEntryArgumentCaptor;
	
	@Before
	public void prepareMocks() {
		mockStatic(RestUtil.class);
		mockStatic(Context.class);
		when(queueServicesWrapper.getQueueService()).thenReturn(queueService);
		when(queueServicesWrapper.getQueueEntryService()).thenReturn(queueEntryService);
		when(queueServicesWrapper.getQueueRoomService()).thenReturn(queueRoomService);
		when(queueServicesWrapper.getRoomProviderMapService()).thenReturn(roomProviderMapService);
		when(queueServicesWrapper.getConceptService()).thenReturn(conceptService);
		when(queueServicesWrapper.getLocationService()).thenReturn(locationService);
		when(queueServicesWrapper.getPatientService()).thenReturn(patientService);
		
		//By pass authentication
		when(Context.isAuthenticated()).thenReturn(true);
		
		when(Context.getRegisteredComponents(QueueServicesWrapper.class))
		        .thenReturn(Collections.singletonList(queueServicesWrapper));
		
		QueueEntrySearchCriteriaParser searchCriteriaParser = new QueueEntrySearchCriteriaParser(queueServicesWrapper);
		when(Context.getRegisteredComponents(QueueEntrySearchCriteriaParser.class))
		        .thenReturn(Collections.singletonList(searchCriteriaParser));
		
		resource = new QueueEntryResource();
		setResource(resource);
		queueEntry = new QueueEntry();
		queueEntry.setUuid(QUEUE_ENTRY_UUID);
		setObject(queueEntry);
		
		requestContext = mock(RequestContext.class);
		request = mock(HttpServletRequest.class);
		when(requestContext.getRequest()).thenReturn(request);
		parameterMap = new HashMap<>();
		when(request.getParameterMap()).thenReturn(parameterMap);
		queueEntryArgumentCaptor = ArgumentCaptor.forClass(QueueEntrySearchCriteria.class);
	}
	
	@Test
	public void shouldReturnDefaultRepresentation() {
		verifyDefaultRepresentation("uuid", "queue", "status", "visit", "priority", "priorityComment", "sortWeight",
		    "patient", "locationWaitingFor", "providerWaitingFor", "startedAt", "endedAt", "display");
	}
	
	@Test
	public void shouldReturnFullRepresentation() {
		verifyFullRepresentation("queue", "status", "priority", "priorityComment", "sortWeight", "patient",
		    "locationWaitingFor", "providerWaitingFor", "startedAt", "endedAt", "display", "uuid", "display", "auditInfo");
	}
	
	@Test
	public void shouldReturnNullForCustomRepresentation() {
		CustomRepresentation customRepresentation = new CustomRepresentation("custom-representation");
		assertThat(getResource().getRepresentationDescription(customRepresentation), is(nullValue()));
	}
	
	@Test
	public void shouldNOTReturnNullForDefaultRepresentation() {
		assertThat(getResource().getRepresentationDescription(new DefaultRepresentation()), is(notNullValue()));
	}
	
	@Test
	public void shouldNOTReturnNullForFullRepresentation() {
		assertThat(getResource().getRepresentationDescription(new FullRepresentation()), is(notNullValue()));
	}
	
	@Test
	public void shouldNOTReturnNullForRefRepresentation() {
		assertThat(getResource().getRepresentationDescription(new RefRepresentation()), is(notNullValue()));
	}
	
	@Test
	public void shouldGetResourceByUniqueUuid() {
		when(queueEntryService.getQueueEntryByUuid(QUEUE_ENTRY_UUID)).thenReturn(Optional.of(queueEntry));
		QueueEntry result = getResource().getByUniqueId(QUEUE_ENTRY_UUID);
		assertThat(result, notNullValue());
		assertThat(result.getUuid(), is(QUEUE_ENTRY_UUID));
	}
	
	@Test
	public void shouldCreateNewResource() {
		when(queueEntryService.createQueueEntry(getObject())).thenReturn(getObject());
		QueueEntry newlyCreatedObject = getResource().save(getObject());
		assertThat(newlyCreatedObject, notNullValue());
		assertThat(newlyCreatedObject.getUuid(), is(QUEUE_ENTRY_UUID));
	}
	
	@Test
	public void shouldSearchQueueEntriesByQueue() {
		List<Queue> vals = Arrays.asList(new Queue(), new Queue());
		String[] refs = new String[] { "ref1", "ref2" };
		parameterMap.put(SEARCH_PARAM_QUEUE, refs);
		when(queueServicesWrapper.getQueues(refs)).thenReturn(vals);
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getQueues(), hasSize(2));
		assertThat(criteria.getQueues(), containsInAnyOrder(vals.get(0), vals.get(1)));
	}
	
	@Test
	public void shouldSearchQueueEntriesByLocation() {
		List<Location> vals = Arrays.asList(new Location(), new Location());
		String[] refs = new String[] { "ref1", "ref2" };
		parameterMap.put(SEARCH_PARAM_LOCATION, refs);
		when(queueServicesWrapper.getLocations(refs)).thenReturn(vals);
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getLocations(), hasSize(2));
		assertThat(criteria.getLocations(), containsInAnyOrder(vals.get(0), vals.get(1)));
	}
	
	@Test
	public void shouldSearchQueueEntriesByService() {
		List<Concept> vals = Arrays.asList(new Concept(), new Concept());
		String[] refs = new String[] { "ref1", "ref2" };
		parameterMap.put(SEARCH_PARAM_SERVICE, refs);
		when(queueServicesWrapper.getConcepts(refs)).thenReturn(vals);
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getServices(), hasSize(2));
		assertThat(criteria.getServices(), containsInAnyOrder(vals.get(0), vals.get(1)));
	}
	
	@Test
	public void shouldSearchQueueEntriesByPatient() {
		Patient val = new Patient();
		String[] refs = new String[] { "ref1" };
		parameterMap.put(SEARCH_PARAM_PATIENT, refs);
		when(queueServicesWrapper.getPatient(refs[0])).thenReturn(val);
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getPatient(), notNullValue());
	}
	
	@Test
	public void shouldSearchQueueEntriesByVisit() {
		Visit val = new Visit();
		String[] refs = new String[] { "ref1" };
		parameterMap.put(SEARCH_PARAM_VISIT, refs);
		when(queueServicesWrapper.getVisit(refs[0])).thenReturn(val);
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getVisit(), notNullValue());
	}
	
	@Test
	public void shouldSearchQueueEntriesByHasVisitTrue() {
		parameterMap.put(SEARCH_PARAM_HAS_VISIT, new String[] { "true" });
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getHasVisit(), equalTo(true));
	}
	
	@Test
	public void shouldSearchQueueEntriesByHasVisitFalse() {
		parameterMap.put(SEARCH_PARAM_HAS_VISIT, new String[] { "false" });
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getHasVisit(), equalTo(false));
	}
	
	@Test
	public void shouldSearchQueueEntriesByPriority() {
		List<Concept> vals = Arrays.asList(new Concept(), new Concept());
		String[] refs = new String[] { "ref1", "ref2" };
		parameterMap.put(SEARCH_PARAM_PRIORITY, refs);
		when(queueServicesWrapper.getConcepts(refs)).thenReturn(vals);
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getPriorities(), hasSize(2));
		assertThat(criteria.getPriorities(), containsInAnyOrder(vals.get(0), vals.get(1)));
	}
	
	@Test
	public void shouldSearchQueueEntriesByStatus() {
		List<Concept> vals = Arrays.asList(new Concept(), new Concept());
		String[] refs = new String[] { "ref1", "ref2" };
		parameterMap.put(SEARCH_PARAM_STATUS, refs);
		when(queueServicesWrapper.getConcepts(refs)).thenReturn(vals);
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getStatuses(), hasSize(2));
		assertThat(criteria.getStatuses(), containsInAnyOrder(vals.get(0), vals.get(1)));
	}
	
	@Test
	public void shouldSearchQueueEntriesByLocationWaitingFor() {
		List<Location> vals = Arrays.asList(new Location(), new Location());
		String[] refs = new String[] { "ref1", "ref2" };
		parameterMap.put(SEARCH_PARAM_LOCATION_WAITING_FOR, refs);
		when(queueServicesWrapper.getLocations(refs)).thenReturn(vals);
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getLocationsWaitingFor(), hasSize(2));
		assertThat(criteria.getLocationsWaitingFor(), containsInAnyOrder(vals.get(0), vals.get(1)));
	}
	
	@Test
	public void shouldSearchQueueEntriesByProviderWaitingFor() {
		List<Provider> vals = Arrays.asList(new Provider(), new Provider());
		String[] refs = new String[] { "ref1", "ref2" };
		parameterMap.put(SEARCH_PARAM_PROVIDER_WAITING_FOR, refs);
		when(queueServicesWrapper.getProviders(refs)).thenReturn(vals);
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getProvidersWaitingFor(), hasSize(2));
		assertThat(criteria.getProvidersWaitingFor(), containsInAnyOrder(vals.get(0), vals.get(1)));
	}
	
	@Test
	public void shouldSearchQueueEntriesByQueueComingFrom() {
		List<Queue> vals = Arrays.asList(new Queue(), new Queue());
		String[] refs = new String[] { "ref1", "ref2" };
		parameterMap.put(SEARCH_PARAM_QUEUE_COMING_FROM, refs);
		when(queueServicesWrapper.getQueues(refs)).thenReturn(vals);
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getQueuesComingFrom(), hasSize(2));
		assertThat(criteria.getQueuesComingFrom(), containsInAnyOrder(vals.get(0), vals.get(1)));
	}
	
	@Test
	public void shouldSearchQueueEntriesByStartedOnOrAfter() {
		String dateStr = "2023-09-10 11:12:13";
		parameterMap.put(SEARCH_PARAM_STARTED_ON_OR_AFTER, new String[] { dateStr });
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getStartedOnOrAfter(), equalTo(QueueUtils.parseDate(dateStr)));
	}
	
	@Test
	public void shouldSearchQueueEntriesByStartedOnOrBefore() {
		String dateStr = "2023-09-10 11:12:13";
		parameterMap.put(SEARCH_PARAM_STARTED_ON_OR_BEFORE, new String[] { dateStr });
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getStartedOnOrBefore(), equalTo(QueueUtils.parseDate(dateStr)));
	}
	
	@Test
	public void shouldSearchQueueEntriesByEndedOnOrAfter() {
		String dateStr = "2023-09-10 11:12:13";
		parameterMap.put(SEARCH_PARAM_ENDED_ON_OR_AFTER, new String[] { dateStr });
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getEndedOnOrAfter(), equalTo(QueueUtils.parseDate(dateStr)));
	}
	
	@Test
	public void shouldSearchQueueEntriesByEndedOnOrBefore() {
		String dateStr = "2023-09-10 11:12:13";
		parameterMap.put(SEARCH_PARAM_ENDED_ON_OR_BEFORE, new String[] { dateStr });
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getEndedOnOrBefore(), equalTo(QueueUtils.parseDate(dateStr)));
	}
	
	@Test
	public void shouldSearchQueueEntriesByIsEndedTrue() {
		parameterMap.put(SEARCH_PARAM_IS_ENDED, new String[] { "true" });
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getIsEnded(), equalTo(true));
	}
	
	@Test
	public void shouldSearchQueueEntriesByIsEndedFalse() {
		parameterMap.put(SEARCH_PARAM_IS_ENDED, new String[] { "false" });
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.getIsEnded(), equalTo(false));
	}
	
	@Test
	public void shouldSearchQueueEntriesByIncludeVoidedTrue() {
		parameterMap.put(SEARCH_PARAM_INCLUDE_VOIDED, new String[] { "true" });
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.isIncludedVoided(), equalTo(true));
	}
	
	@Test
	public void shouldSearchQueueEntriesByIncludeVoidedFalse() {
		parameterMap.put(SEARCH_PARAM_INCLUDE_VOIDED, new String[] { "false" });
		resource.doSearch(requestContext);
		verify(queueEntryService).getQueueEntries(queueEntryArgumentCaptor.capture());
		QueueEntrySearchCriteria criteria = queueEntryArgumentCaptor.getValue();
		assertThat(criteria.isIncludedVoided(), equalTo(false));
	}
	
	@Test
	public void shouldInstantiateNewDelegate() {
		assertThat(getResource().newDelegate(), notNullValue());
	}
	
	@Test
	public void verifyResourceVersion() {
		assertThat(getResource().getResourceVersion(), is("2.3"));
	}
}
