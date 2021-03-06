package org.meveo.service.catalog.impl;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.CustomFieldDto;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.model.catalog.BusinessProductModel;
import org.meveo.model.catalog.BusinessServiceModel;
import org.meveo.model.catalog.ChargeTemplate;
import org.meveo.model.catalog.PricePlanMatrix;
import org.meveo.model.catalog.ProductTemplate;
import org.meveo.service.admin.impl.GenericModuleService;
import org.meveo.service.script.product.ProductModelScriptService;
import org.meveo.service.script.product.ProductScriptInterface;

/**
 * @author Edward P. Legaspi
 */
@Stateless
public class BusinessProductModelService extends GenericModuleService<BusinessProductModel> {

    @Inject
    private CatalogHierarchyBuilderService catalogHierarchyBuilderService;

    @Inject
    private ProductModelScriptService productModelScriptService;

    public BusinessServiceModel findByBPMAndProductTemplate(String bsm, String productTemplateCode) {
        QueryBuilder qb = new QueryBuilder(BusinessProductModel.class, "b");
        qb.addCriterion("b.code", "=", bsm, true);
        qb.addCriterion("b.productTemplate.code", "=", productTemplateCode, true);

        try {
            return (BusinessServiceModel) qb.getQuery(getEntityManager()).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Use by GUI. Prefix is null, as ID of the new product is used.
     * 
     * @param entity product template
     * @param bpm business product model
     * @return product template
     * @throws BusinessException business exception.
     */
    public ProductTemplate instantiateBPM(ProductTemplate entity, BusinessProductModel bpm) throws BusinessException {
        return instantiateBPM(null, entity, bpm, null);
    }

    /**
     * Instantiates a product from a given BusinessProductModel.
     * 
     * @param prefix - if empty, productTemplate.id is used for child entities
     * @param productTemplate - source template
     * @param bpm - if productTemplate is null, bpm.productTemplate is used
     * @param customFields custom fields.
     * @return product template
     * @throws BusinessException business exception.
     */
    public ProductTemplate instantiateBPM(String prefix, ProductTemplate productTemplate, BusinessProductModel bpm, List<CustomFieldDto> customFields) throws BusinessException {

        ProductScriptInterface productScript = null;
        if (bpm.getScript() != null) {
            try {
                productScript = productModelScriptService.beforeCreate(customFields, bpm.getScript().getCode());
            } catch (BusinessException e) {
                log.error("Failed to execute a script {}", bpm.getScript().getCode(), e);
            }
        }

        // 2 - instantiate
        List<PricePlanMatrix> pricePlansInMemory = new ArrayList<>();
        List<ChargeTemplate> chargeTemplateInMemory = new ArrayList<>();

        if (productTemplate == null) {
            productTemplate = bpm.getProductTemplate();
        }

        ProductTemplate newProductTemplate = new ProductTemplate();
        catalogHierarchyBuilderService.duplicateProductTemplate(prefix, null, productTemplate, newProductTemplate, pricePlansInMemory, chargeTemplateInMemory, null);

        if (productScript != null) {
            try {
                productModelScriptService.afterCreate(productTemplate, customFields, productScript);
            } catch (BusinessException e) {
                log.error("Failed to execute a script {}", bpm.getScript().getCode(), e);
            }
        }

        return newProductTemplate;
    }

    @SuppressWarnings("unchecked")
    public List<BusinessProductModel> listByProductTemplate(ProductTemplate productTemplate) {
        QueryBuilder qb = new QueryBuilder(BusinessProductModel.class, "t");
        qb.addCriterionEntity("productTemplate", productTemplate);

        List<BusinessProductModel> result = (List<BusinessProductModel>) qb.getQuery(getEntityManager()).getResultList();
        return (result == null || result.isEmpty()) ? null : result;
    }
    
    /**
     * Returns the count of all installed BPM
     * @param productTemplate productTemplate of BPM
     * @return count of install BPM
     */
    public Long countByProductTemplate(ProductTemplate productTemplate) {
        QueryBuilder qb = new QueryBuilder(BusinessProductModel.class, "t");
        qb.addCriterionEntity("productTemplate", productTemplate);
        qb.addCriterion("installed", "=", true, true);

        return qb.count(getEntityManager());
    }

}
