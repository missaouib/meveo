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
package org.meveo.service.billing.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.RatingException;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.model.billing.ApplicationTypeEnum;
import org.meveo.model.billing.ProductChargeInstance;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.catalog.ProductChargeTemplate;
import org.meveo.model.rating.RatingResult;
import org.meveo.service.base.BusinessService;

/**
 * Product charge instance service
 * 
 * @author akadid abdelmounaim
 * @lastModifiedVersion 5.1 Candidate
 */
@Stateless
public class ProductChargeInstanceService extends BusinessService<ProductChargeInstance> {

    @EJB
    private WalletService walletService;

    @EJB
    private WalletOperationService walletOperationService;

    @Inject
    private RatingService ratingService;

    /**
     * @param code code of product charge instance
     * @param userAccountId id of user account
     * @return product charge instance.
     */
    public ProductChargeInstance findByCodeAndSubsription(String code, Long userAccountId) {
        ProductChargeInstance productChargeInstance = null;
        try {
            log.debug("start of find {} by code (code={}, userAccountId={}) ..", new Object[] { "ProductChargeInstance", code, userAccountId });
            QueryBuilder qb = new QueryBuilder(ProductChargeInstance.class, "c");
            qb.addCriterion("c.code", "=", code, true);
            qb.addCriterion("c.userAccount.id", "=", userAccountId, true);
            productChargeInstance = (ProductChargeInstance) qb.getQuery(getEntityManager()).getSingleResult();
            log.debug("end of find {} by code (code={}, userAccountId={}). Result found={}.", new Object[] { "ProductChargeInstance", code, userAccountId, productChargeInstance != null });
        } catch (NoResultException nre) {
            log.debug("findByCodeAndSubsription : aucune charge ponctuelle n'a ete trouvee");
        } catch (Exception e) {
            log.error("failed to find productChargeInstance by Code and subsription", e);
        }
        return productChargeInstance;
    }

    /**
     * Apply product charge instance v5.1 Candidate apply rating filter to product charge instance
     * 
     * @param productChargeInstance product charge instance
     * @param isVirtual indicates that it is virtual operation
     * @return list of wallet operations
     * @throws BusinessException business exception.
     * @throws RatingException Failed to rate a charge due to lack of funds, data validation, inconsistency or other rating related failure
     * @lastModifiedVersion 5.1 Candidate
     */
    public List<WalletOperation> applyProductChargeInstance(ProductChargeInstance productChargeInstance, boolean isVirtual) throws BusinessException, RatingException {

        List<WalletOperation> walletOperations = null;
        ProductChargeTemplate chargeTemplate = productChargeInstance.getProductChargeTemplate();

        if (!walletOperationService.isChargeMatch(productChargeInstance, chargeTemplate.getFilterExpression())) {
            log.debug("not rating productChargeInstance with code={}, filter expression not evaluated to true", productChargeInstance.getCode());
            return new ArrayList<WalletOperation>();
        }

        UserAccount userAccount = productChargeInstance.getUserAccount();
        Subscription subscription = productChargeInstance.getSubscription();
        log.debug("Apply product charge. User account {}, subscription {}, charge {}, quantity {}, date {}", userAccount != null ? userAccount.getCode() : null, subscription != null ? subscription.getCode() : null,
            chargeTemplate.getCode(), productChargeInstance.getQuantity(), productChargeInstance.getChargeDate());

        RatingResult ratingResult = ratingService.rateChargeAndTriggerEDRs(productChargeInstance, ApplicationTypeEnum.PUNCTUAL, productChargeInstance.getChargeDate(), productChargeInstance.getQuantity(), null, null,
            null, null, null, null, false, isVirtual);

        WalletOperation walletOperation = ratingResult.getWalletOperation();
        if (!isVirtual) {
            walletOperations = walletOperationService.chargeWalletOperation(walletOperation);
        } else {
            walletOperations = new ArrayList<>();
            walletOperations.add(walletOperation);
        }
        return walletOperations;
    }

    @SuppressWarnings("unchecked")
    public List<ProductChargeInstance> findBySubscriptionId(Long subscriptionId) {
        QueryBuilder qb = new QueryBuilder(ProductChargeInstance.class, "c", Arrays.asList("chargeTemplate"));
        qb.addCriterion("c.subscription.id", "=", subscriptionId, true);
        return qb.getQuery(getEntityManager()).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<ProductChargeInstance> findByUserAccountId(Long userAccountId) {
        QueryBuilder qb = new QueryBuilder(ProductChargeInstance.class, "c", Arrays.asList("chargeTemplate"));
        qb.addCriterion("c.userAccount.id", "=", userAccountId, true);
        qb.addSql("c.subscription is null");
        return qb.getQuery(getEntityManager()).getResultList();
    }
}