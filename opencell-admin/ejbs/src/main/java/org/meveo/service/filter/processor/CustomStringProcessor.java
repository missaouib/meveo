/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */

package org.meveo.service.filter.processor;

import java.util.Map;

import org.meveo.admin.exception.FilterException;
import org.meveo.commons.utils.FilteredQueryBuilder;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.filter.FilterParameterTypeEnum;
import org.meveo.model.filter.PrimitiveFilterCondition;

public class CustomStringProcessor extends StringProcessor {

    @Override
    public boolean canProccessCondition(PrimitiveFilterCondition condition) {
        return isPrefixInOperand(condition, FilterParameterTypeEnum.STRING.getPrefix());
    }

    @Override
    public void process(FilteredQueryBuilder queryBuilder, String alias, PrimitiveFilterCondition condition) throws FilterException {
        Map.Entry<CustomFieldTemplate, Object> customFieldEntry = fetchCustomFieldEntry(queryBuilder.getParameterMap(), condition.getOperand());
        if(customFieldEntry != null){
            buildQuery(queryBuilder, alias, condition, String.valueOf(customFieldEntry.getValue()));
        } else if(!StringUtils.isBlank(condition.getOperand())) {
			buildQuery(queryBuilder, alias, condition, getParameterValue(condition.getOperand()));
        }
    }
    
    private String getParameterValue(String operand) {
    	return operand.substring(FilterParameterTypeEnum.STRING.getPrefix().length() + 1);    	
    }
}
