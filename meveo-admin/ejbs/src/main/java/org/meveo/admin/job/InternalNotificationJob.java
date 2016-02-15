/*
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.admin.job;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.admin.User;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.crm.impl.CustomFieldInstanceService;
import org.meveo.service.job.Job;

@Startup
@Singleton
public class InternalNotificationJob extends Job {

    @Inject
    private InternalNotificationJobBean internalNotificationJobBean;
    
    @Inject
    private CustomFieldInstanceService customFieldInstanceService;

    @Override
    protected void execute(JobExecutionResultImpl result, JobInstance jobInstance, User currentUser) throws BusinessException {
        String filterCode = (String) customFieldInstanceService.getCFValue(jobInstance, "InternalNotificationJob_filterCode", currentUser);
        String notificationCode = (String) customFieldInstanceService.getCFValue(jobInstance, "InternalNotificationJob_notificationCode", currentUser);
        internalNotificationJobBean.execute(filterCode,notificationCode,result, currentUser);
    }

    @Override
    public JobCategoryEnum getJobCategory() {
        return JobCategoryEnum.UTILS;
    }
    
    @Override
    public Map<String, CustomFieldTemplate> getCustomFields() {
        Map<String, CustomFieldTemplate> result = new HashMap<String, CustomFieldTemplate>();

        CustomFieldTemplate filterCode = new CustomFieldTemplate();
        filterCode.setCode("InternalNotificationJob_filterCode");
        filterCode.setAppliesTo("JOB_InternalNotificationJob");
        filterCode.setActive(true);
        filterCode.setDescription("Filter (sql query)");
        filterCode.setFieldType(CustomFieldTypeEnum.STRING);
        filterCode.setValueRequired(true);
        filterCode.setMaxValue(50L);
        result.put("InternalNotificationJob_filterCode", filterCode);

        CustomFieldTemplate notificationCode = new CustomFieldTemplate();
        notificationCode.setCode("InternalNotificationJob_notificationCode");
        notificationCode.setAppliesTo("JOB_InternalNotificationJob");
        notificationCode.setActive(true);
        notificationCode.setDescription("Notification code");
        notificationCode.setFieldType(CustomFieldTypeEnum.STRING);
        notificationCode.setValueRequired(true);
        notificationCode.setMaxValue(50L);
        result.put("InternalNotificationJob_notificationCode", notificationCode);

        return result;
    }
}