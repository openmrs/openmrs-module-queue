/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.web.resources.custom.response;

import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.resource.api.Converter;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenericSingleObjectResult implements PageableResult {
	
	private List<PropValue> propValues;
	
	@Override
	public SimpleObject toSimpleObject(Converter<?> converter) throws ResponseException {
		SimpleObject ret = new SimpleObject();
		this.propValues.forEach(propValue -> ret.add(propValue.getProperty(), propValue.getValue()));
		return ret;
	}
	
	public void add(@NotNull String property, @NotNull Object value) {
		if (propValues == null) {
			this.propValues = new ArrayList<>();
		}
		this.propValues.add(new PropValue(property, value));
	}
}
