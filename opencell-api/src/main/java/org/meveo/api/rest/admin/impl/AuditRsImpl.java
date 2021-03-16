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

package org.meveo.api.rest.admin.impl;

import org.meveo.api.admin.AuditApi;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.api.rest.admin.AuditRs;
import org.meveo.api.rest.impl.BaseRs;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

/**
 * @author Mounir Bahije
 * @lastModifiedVersion 5.1
 *
 */
@RequestScoped
@Interceptors({ WsRestApiInterceptor.class })
public class AuditRsImpl extends BaseRs implements AuditRs {

    @Inject
    private AuditApi auditApi;

    @Override
    public ActionStatus enableORdisableAudit(String enableORdisable) {

        ActionStatus result = new ActionStatus();

        try {

            switch (enableORdisable) {
            case "enable":
                auditApi.auditActive(true);
                break;
            case "disable":
                auditApi.auditActive(false);
                break;
            default:
                break;
            }

        } catch (Exception e) {
            log.error("error = {}", e);
        }

        return result;
    }

}
