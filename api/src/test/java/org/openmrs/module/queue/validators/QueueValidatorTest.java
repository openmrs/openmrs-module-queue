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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.SpringTestConfiguration;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringTestConfiguration.class, inheritLocations = false)
public class QueueValidatorTest extends BaseModuleContextSensitiveTest {
	
	private static final String LOCATION_INITIAL_DATASET_XML = "org/openmrs/module/queue/api/dao/QueueDaoTest_locationInitialDataset.xml";
	
	private static final String BAD_LOCATION_UUID = "60938432-1691-11df-97a5-7038c0lk";
	
	private static final String LOCATION_UUID = "d0938432-1691-11df-97a5-7038c098";
	
	@Autowired
	private QueueValidator validator;
	
	@Before
	public void setup() {
		executeDataSet(LOCATION_INITIAL_DATASET_XML);
	}
	
	@Test
	public void shouldTrueForValidLocation() {
		Location location = Context.getLocationService().getLocationByUuid(LOCATION_UUID);
		assertTrue(validator.isValidLocation(location));
	}
	
	@Test
	public void shouldFalseForBadLocation() {
		Location location = Context.getLocationService().getLocationByUuid(BAD_LOCATION_UUID);
		assertFalse(validator.isValidLocation(location));
	}
}
