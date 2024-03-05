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
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.queue.model.QueueEntry;

@RunWith(MockitoJUnitRunner.class)
public class ExistingValueSortWeightGeneratorTest {
	
	QueueEntry queueEntry;
	
	ExistingValueSortWeightGenerator generator;
	
	@Before
	public void setupMocks() {
		MockitoAnnotations.openMocks(this);
		queueEntry = new QueueEntry();
		generator = new ExistingValueSortWeightGenerator();
	}
	
	@Test
	public void shouldReturnCorrectSortWeight() {
		for (double d = 0.0; d < 20.0; d += 1) {
			queueEntry.setSortWeight(d);
			assertThat(generator.generateSortWeight(queueEntry), equalTo(d));
		}
	}
}
