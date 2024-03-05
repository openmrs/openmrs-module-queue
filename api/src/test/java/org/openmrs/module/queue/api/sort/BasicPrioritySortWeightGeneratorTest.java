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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.model.QueueEntry;

@RunWith(MockitoJUnitRunner.class)
public class BasicPrioritySortWeightGeneratorTest {
	
	@Mock
	private QueueServicesWrapper services;
	
	@Mock
	Concept concept1;
	
	@Mock
	Concept concept2;
	
	@Mock
	Concept concept3;
	
	QueueEntry queueEntry;
	
	BasicPrioritySortWeightGenerator generator;
	
	@Before
	public void setupMocks() {
		MockitoAnnotations.openMocks(this);
		when(services.getAllowedPriorities(any())).thenReturn(Arrays.asList(concept1, concept2, concept3));
		queueEntry = new QueueEntry();
		generator = new BasicPrioritySortWeightGenerator(services);
	}
	
	@Test
	public void shouldReturnCorrectSortWeight() {
		assertThat(generator.generateSortWeight(queueEntry), equalTo(0.0));
		queueEntry.setPriority(concept1);
		assertThat(generator.generateSortWeight(queueEntry), equalTo(0.0));
		queueEntry.setPriority(concept2);
		assertThat(generator.generateSortWeight(queueEntry), equalTo(1.0));
		queueEntry.setPriority(concept3);
		assertThat(generator.generateSortWeight(queueEntry), equalTo(2.0));
	}
}
