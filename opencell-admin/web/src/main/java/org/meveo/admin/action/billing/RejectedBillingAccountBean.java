/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.admin.action.billing;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.RejectedBillingAccount;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.RejectedBillingAccountService;
import org.primefaces.model.LazyDataModel;

/**
 * Standard backing bean for {@link RejectedBillingAccount} (extends {@link BaseBean} that provides almost all common methods to handle entities filtering/sorting in datatable,
 * their create, edit, view, delete operations). It works with Manaty custom JSF components.
 */
@Named
@ViewScoped
public class RejectedBillingAccountBean extends BaseBean<RejectedBillingAccount> {

    private static final long serialVersionUID = 1L;

    /**
     * Injected @{link RejectedBillingAccount} service. Extends {@link PersistenceService}
     */
    @Inject
    private RejectedBillingAccountService rejectedBillingAccountService;

    /**
     * Constructor. Invokes super constructor and provides class type of this bean for {@link BaseBean}.
     */
    public RejectedBillingAccountBean() {
        super(RejectedBillingAccount.class);
    }

    @Override
    protected IPersistenceService<RejectedBillingAccount> getPersistenceService() {
        return rejectedBillingAccountService;
    }

    @Override
    protected String getDefaultSort() {
        return "billingAccount.code";
    }

    /**
     * Method, that is invoked in billing run screen. This method returns billingAccountRejected associated with current Billing Run.
     * 
     * @param br Billing run
     * @return Data model for Primefaces data list component
     */
    public LazyDataModel<RejectedBillingAccount> getBArejected(BillingRun br) {
        if (br == null) {
            log.warn("billingRun is null");
        } else {
            filters.put("billingRun", br);
            return getLazyDataModel();
        }

        return null;
    }
}