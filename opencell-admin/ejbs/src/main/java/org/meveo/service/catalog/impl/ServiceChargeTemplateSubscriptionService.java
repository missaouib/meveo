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
package org.meveo.service.catalog.impl;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;

import org.meveo.commons.utils.QueryBuilder;
import org.meveo.model.catalog.OneShotChargeTemplate;
import org.meveo.model.catalog.ServiceChargeTemplateSubscription;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.model.catalog.WalletTemplate;
import org.meveo.service.base.PersistenceService;

@Stateless
public class ServiceChargeTemplateSubscriptionService extends PersistenceService<ServiceChargeTemplateSubscription> {

    @SuppressWarnings("unchecked")
    public List<ServiceChargeTemplateSubscription> findBySubscriptionChargeTemplate(OneShotChargeTemplate chargeTemplate) {

        QueryBuilder qb = new QueryBuilder(ServiceChargeTemplateSubscription.class, "a");
        qb.addCriterionEntity("chargeTemplate", chargeTemplate);
        

        return (List<ServiceChargeTemplateSubscription>) qb.getQuery(getEntityManager()).getResultList();
    }

    public void removeByServiceTemplate(ServiceTemplate serviceTemplate) {
        Query query = getEntityManager().createQuery("DELETE ServiceChargeTemplateSubscription t WHERE t.serviceTemplate=:serviceTemplate ");
        query.setParameter("serviceTemplate", serviceTemplate);
        query.executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public List<ServiceChargeTemplateSubscription> findByWalletTemplate(WalletTemplate walletTemplate) {
        QueryBuilder qb = new QueryBuilder(ServiceChargeTemplateSubscription.class, "s");
        qb.addCriterionEntity("walletTemplate", walletTemplate);
        return qb.find(getEntityManager());
    }
}