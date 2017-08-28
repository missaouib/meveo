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
package org.meveo.admin.action.catalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.action.CustomFieldBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.web.interceptor.ActionMethod;
import org.meveo.api.dto.CustomFieldsDto;
import org.meveo.api.dto.catalog.ServiceConfigurationDto;
import org.meveo.export.EntityExportImportService;
import org.meveo.export.ExportTemplate;
import org.meveo.model.catalog.BusinessOfferModel;
import org.meveo.model.catalog.LifeCycleStatusEnum;
import org.meveo.model.catalog.OfferProductTemplate;
import org.meveo.model.catalog.OfferServiceTemplate;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.catalog.ProductOffering;
import org.meveo.model.catalog.ProductTemplate;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.model.crm.custom.CustomFieldValue;
import org.meveo.service.api.EntityToDtoConverter;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.catalog.impl.BusinessOfferModelService;
import org.meveo.service.catalog.impl.OfferTemplateService;
import org.meveo.service.catalog.impl.ServiceTemplateService;
import org.meveo.service.crm.impl.CustomFieldInstanceService;
import org.primefaces.model.DualListModel;

/**
 * Standard backing bean for {@link OfferTemplate} (extends {@link BaseBean} that provides almost all common methods to handle entities filtering/sorting in datatable, their
 * create, edit, view, delete operations). It works with Manaty custom JSF components. s
 * 
 */
@Named
@ViewScoped
public class OfferTemplateBean extends CustomFieldBean<OfferTemplate> {

    private static final long serialVersionUID = 1L;

    @Inject
    private SubscriptionService subscriptionService;

    /**
     * Injected @{link OfferTemplate} service. Extends {@link PersistenceService}.
     */
    @Inject
    protected OfferTemplateService offerTemplateService;

    @Inject
    private ServiceTemplateService serviceTemplateService;

    @Inject
    protected CustomFieldInstanceService customFieldInstanceService;

    @Inject
    private BusinessOfferModelService businessOfferModelService;

    @Inject
    private EntityToDtoConverter entityToDtoConverter;

    @Inject
    private EntityExportImportService entityExportImportService;

    private Long bomId;

    private boolean newVersion;
    private boolean duplicateOffer;

    private DualListModel<ServiceTemplate> incompatibleServices;
    private OfferServiceTemplate offerServiceTemplate;
    private OfferProductTemplate offerProductTemplate;
    private BusinessOfferModel businessOfferModel;
    private List<ProductTemplate> productTemplatesLookup;
    private List<OfferServiceTemplate> sortedOfferServiceTemplates;

    /**
     * Constructor. Invokes super constructor and provides class type of this bean for {@link BaseBean}.
     */
    public OfferTemplateBean() {
        super(OfferTemplate.class);
    }

    @Override
    public OfferTemplate initEntity() {
        super.initEntity();

        if (bomId != null) {
            businessOfferModel = businessOfferModelService.findById(bomId);
            productTemplatesLookup = new ArrayList<>();
            if (entity.getOfferProductTemplates() != null && !entity.getOfferProductTemplates().isEmpty()) {
                for (OfferProductTemplate opt : entity.getOfferProductTemplates()) {
                    productTemplatesLookup.add(opt.getProductTemplate());
                }
            }
            setOverrideImageOnUpload(false);
        }

        // Load service templates
        for (OfferServiceTemplate offerServiceTemplate : entity.getOfferServiceTemplates()) {
            offerServiceTemplate.getServiceTemplate().getCode();
            for (ServiceTemplate serviceTemplate : offerServiceTemplate.getIncompatibleServices()) {
                serviceTemplate.getCode();
            }
        }

        // Load product templates
        for (OfferProductTemplate offerProductTemplate : entity.getOfferProductTemplates()) {
            offerProductTemplate.getProductTemplate();
        }

        if (newVersion) {
            instantiateNewVersion();
            setObjectId(entity.getId());
            newVersion = false;
        }

        if (duplicateOffer) {
            duplicateOfferOnly();
            setObjectId(entity.getId());
            duplicateOffer = false;
        }

        return entity;
    }

    /**
     * @see org.meveo.admin.action.BaseBean#getPersistenceService()
     */
    @Override
    protected IPersistenceService<OfferTemplate> getPersistenceService() {
        return offerTemplateService;
    }

    /**
     * @see org.meveo.admin.action.BaseBean#getFormFieldsToFetch()
     */
    protected List<String> getFormFieldsToFetch() {
        return Arrays.asList("offerTemplateCategories", "channels", "businessAccountModels");
    }

    public List<OfferTemplate> listActiveByDate(Date date) {
        return offerTemplateService.listActiveByDate(date);
    }

    @Override
    protected String getDefaultSort() {
        return "code";
    }

    public List<ProductTemplate> getProductTemplatesLookup() {
        return productTemplatesLookup;
    }

    public void setProductTemplatesLookup(List<ProductTemplate> productTemplatesLookup) {
        this.productTemplatesLookup = productTemplatesLookup;
    }

    public boolean isDuplicateOffer() {
        return duplicateOffer;
    }

    public void setDuplicateOffer(boolean duplicateOffer) {
        this.duplicateOffer = duplicateOffer;
    }

    @ActionMethod
    public void duplicate() {
        if (entity != null && entity.getId() != null) {
            try {
                offerTemplateService.duplicate(entity);
                messages.info(new BundleKey("messages", "duplicate.successfull"));
            } catch (BusinessException e) {
                log.error("Error encountered duplicating offer template entity: {}", entity.getCode(), e);
                messages.error(new BundleKey("messages", "error.duplicate.unexpected"));
            }
        }
    }

    /**
     * @param givenEntity entity to check
     * @return true/false
     */
    public boolean isUsedInSubscription(OfferTemplate givenEntity) {
        return (givenEntity != null && !givenEntity.isTransient() && (subscriptionService.findByOfferTemplate(givenEntity) != null)
                && subscriptionService.findByOfferTemplate(givenEntity).size() > 0) ? true : false;
    }

    /**
     * delete all entities related to Offer( used only for Marketing Manager)
     */
    @ActionMethod
    public void deleteCatalogHierarchy(OfferTemplate entity) {
        if (!this.isUsedInSubscription(entity)) {
            if (entity != null && entity.getId() != null) {
                try {
                    offerTemplateService.delete(entity);
                    messages.info(new BundleKey("messages", "delete.successful"));
                } catch (BusinessException e) {
                    log.error("Error encountered while deleting offer template entity: {}: {}", entity.getCode(), e);
                    messages.error(new BundleKey("messages", "error.delete.unexpected"));
                }
            }
        } else {
            log.error("This entity is already in subscription: {}", entity.getCode());
            messages.error(new BundleKey("messages", "error.delete.entityUsed "));
        }
    }

    @ActionMethod
    public void instantiateNewVersion() {
        if (entity != null && entity.getId() != null) {
            try {
                entity = offerTemplateService.instantiateNewVersion(entity);
                messages.info(new BundleKey("messages", "newVersion.successful"));
            } catch (BusinessException e) {
                log.error("Error encountered instantiating new offer template entity version: {}", entity.getCode(), e);
                messages.error(new BundleKey("messages", "error.newVersion.unsuccessful"));
            }
        }
    }

    public boolean isUsedInSubscription() {
        return (getEntity() != null && !getEntity().isTransient() && (subscriptionService.findByOfferTemplate(getEntity()) != null)
                && subscriptionService.findByOfferTemplate(getEntity()).size() > 0) ? true : false;
    }

    /**
     * 
     * @return sorted offer services templates
     */
	public List<OfferServiceTemplate> getSortedOfferServiceTemplates() {
		if (sortedOfferServiceTemplates == null) {
			if (entity != null) {
				sortedOfferServiceTemplates = new ArrayList<>();
				sortedOfferServiceTemplates.addAll(entity.getOfferServiceTemplates());
				Collections.sort(sortedOfferServiceTemplates, new DescriptionComparator());
			}
		}

		return sortedOfferServiceTemplates;
	}

	public void resortOfferServiceTemplates() {
		if (sortedOfferServiceTemplates != null && !sortedOfferServiceTemplates.isEmpty()) {
//			Collections.sort(sortedOfferServiceTemplates, new DescriptionComparator());
		}
	}

    @Override
    @ActionMethod
    public String saveOrUpdate(boolean killConversation) throws BusinessException {
        if (bomId != null && businessOfferModel != null) {
            Map<String, List<CustomFieldValue>> cfValues = customFieldDataEntryBean.getFieldValueHolderByUUID(entity.getUuid()).getValuesByCode();
            CustomFieldsDto cfsDto = entityToDtoConverter.getCustomFieldsDTO(entity, cfValues);

            List<ServiceConfigurationDto> servicesConfigurations = new ArrayList<>();
            // process the services
            for (OfferServiceTemplate ost : entity.getOfferServiceTemplates()) {
                ServiceTemplate st = ost.getServiceTemplate();
                if (st.isSelected()) {
                    Map<String, List<CustomFieldValue>> stCfValues = customFieldDataEntryBean.getFieldValueHolderByUUID(st.getUuid()).getValuesByCode();
                    CustomFieldsDto stCfsDto = entityToDtoConverter.getCustomFieldsDTO(st, stCfValues);

                    ServiceConfigurationDto serviceConfigurationDto = new ServiceConfigurationDto();
                    serviceConfigurationDto.setCode(st.getCode());
                    serviceConfigurationDto.setDescription(st.getDescription());
                    serviceConfigurationDto.setMandatory(ost.isMandatory());
                    servicesConfigurations.add(serviceConfigurationDto);
                    if (stCfsDto != null) {
                        serviceConfigurationDto.setCustomFields(stCfsDto.getCustomField());
                    }
                }
            }

            List<ServiceConfigurationDto> productsConfigurations = new ArrayList<>();
            // process products
            for (OfferProductTemplate opt : entity.getOfferProductTemplates()) {
                ProductTemplate pt = opt.getProductTemplate();
                ServiceConfigurationDto serviceConfigurationDto = new ServiceConfigurationDto();
                serviceConfigurationDto.setCode(pt.getCode());
                serviceConfigurationDto.setDescription(pt.getDescription());
                productsConfigurations.add(serviceConfigurationDto);
            }

            OfferTemplate newOfferTemplate = businessOfferModelService.createOfferFromBOM(businessOfferModel, cfsDto != null ? cfsDto.getCustomField() : null, entity.getCode(),
                entity.getName(), entity.getDescription(), servicesConfigurations, productsConfigurations, entity.getChannels(), entity.getBusinessAccountModels(),
                entity.getOfferTemplateCategories(), entity.getLifeCycleStatus(), entity.getImagePath(), entity.getValidityRaw() != null ? entity.getValidityRaw().getFrom() : null,
                entity.getValidityRaw() != null ? entity.getValidityRaw().getTo() : null, getLanguageMessagesMap());

            // populate service custom fields
            for (OfferServiceTemplate ost : entity.getOfferServiceTemplates()) {
                ServiceTemplate serviceTemplate = ost.getServiceTemplate();
                if (serviceTemplate.isSelected()) {
                    for (OfferServiceTemplate newOst : newOfferTemplate.getOfferServiceTemplates()) {
                        ServiceTemplate newServiceTemplate = newOst.getServiceTemplate();
                        String serviceTemplateCode = newOfferTemplate.getId() + "_" + serviceTemplate.getCode();
                        if (serviceTemplateCode.equals(newServiceTemplate.getCode())) {
                            Map<String, List<CustomFieldValue>> stCustomFieldInstances = customFieldDataEntryBean.getFieldValueHolderByUUID(serviceTemplate.getUuid())
                                .getValuesByCode();
                            if (stCustomFieldInstances != null) {
                                // populate offer cf
                                customFieldDataEntryBean.saveCustomFieldsToEntity(newServiceTemplate, serviceTemplate.getUuid(), true, false, true);
                                break;
                            }
                        }
                    }
                }
            }

            // populate offer cf
            customFieldDataEntryBean.saveCustomFieldsToEntity(newOfferTemplate, entity.getUuid(), true, false);

            // detach
            offerTemplateService.detach(entity);
            entity.setId(null);

            return back();
        } else {
            boolean isNewEntity = (entity.getId() == null);

            String outcome = super.saveOrUpdate(killConversation);

            if (outcome != null) {

                if (outcome.equals("mm_offers")) {
                    // populate service custom fields
                    for (OfferServiceTemplate ost : entity.getOfferServiceTemplates()) {
                        ServiceTemplate serviceTemplate = ost.getServiceTemplate();
                        Map<String, List<CustomFieldValue>> stCustomFieldInstances = customFieldDataEntryBean.getFieldValueHolderByUUID(serviceTemplate.getUuid()).getValuesByCode();
                        if (stCustomFieldInstances != null) {
                            // populate offer cf
                            customFieldDataEntryBean.saveCustomFieldsToEntity(serviceTemplate, serviceTemplate.getUuid(), false, false);
                        }
                    }
                }

                return (isNewEntity && !outcome.equals("mm_offers")) ? getEditViewName() : outcome;
            }
        }

        return null;
    }

    @ActionMethod
    public void saveOfferServiceTemplate() {
        log.info("saveOfferServiceTemplate getObjectId={}", getObjectId());

        try {
            if (offerServiceTemplate != null && offerServiceTemplate.getServiceTemplate() == null) {
                messages.error(new BundleKey("messages", "save.unsuccessful"));
            }

            offerServiceTemplate.setIncompatibleServices(serviceTemplateService.refreshOrRetrieve(incompatibleServices.getTarget()));

            if (offerServiceTemplate.getId() != null) {
                messages.info(new BundleKey("messages", "offerTemplate.serviceTemplate.update.successful"));

            } else {

                // Validate that such service was not added earlier
                if (entity.containsServiceTemplate(offerServiceTemplate.getServiceTemplate())) {
                    messages.error(new BundleKey("messages", "offerTemplate.alreadyContainsService"), offerServiceTemplate.getServiceTemplate().getDescriptionOrCode());
                    return;
                }

                offerServiceTemplate.setOfferTemplate(entity);
                entity.addOfferServiceTemplate(offerServiceTemplate);
                messages.info(new BundleKey("messages", "offerTemplate.serviceTemplate.create.successful"));
            }

        } catch (Exception e) {
            log.error("exception when saving offer service template !", e.getMessage());
            messages.error(new BundleKey("messages", "save.unsuccessful"));
        }

        offerServiceTemplate = null;
    }

    @ActionMethod
    public void deleteOfferServiceTemplate(OfferServiceTemplate offerServiceTemplate) {
        try {

            entity.getOfferServiceTemplates().remove(offerServiceTemplate);
            this.offerServiceTemplate = null;

            messages.info(new BundleKey("messages", "offerTemplate.serviceTemplate.delete.successful"));

        } catch (Exception e) {
            messages.error(new BundleKey("messages", "error.delete.unexpected"));
        }
    }

    @ActionMethod
    public void deleteOfferProductTemplate(OfferProductTemplate offerProductTemplate) throws BusinessException {
        try {
            entity.getOfferProductTemplates().remove(offerProductTemplate);
            this.offerProductTemplate = null;

            messages.info(new BundleKey("messages", "offerTemplate.productTemplate.delete.successful"));

        } catch (Exception e) {
            messages.error(new BundleKey("messages", "error.delete.unexpected"));
        }
    }

    public void editOfferServiceTemplate(OfferServiceTemplate offerServiceTemplate) {
        this.offerServiceTemplate = offerServiceTemplate;
        setIncompatibleServices(null);
    }

    public void cancelOfferServiceTemplateEdit() {
        this.offerServiceTemplate = null;
    }

    public void newOfferProductTemplate() {
        this.offerProductTemplate = new OfferProductTemplate();
    }

    public void editOfferProductTemplate(OfferProductTemplate offerProductTemplate) {
        this.offerProductTemplate = offerProductTemplate;
    }

    public void cancelOfferProductTemplateEdit() {
        this.offerProductTemplate = null;
    }

    @ActionMethod
    public void saveOfferProductTemplate() {
        log.info("saveOfferProductTemplate getObjectId={}", getObjectId());

        try {
            if (offerProductTemplate != null && offerProductTemplate.getProductTemplate() == null) {
                messages.error(new BundleKey("messages", "save.unsuccessful"));
            }

            if (offerProductTemplate.getId() != null) {
                messages.info(new BundleKey("messages", "offerTemplate.productTemplate.update.successful"));

            } else {

                // Validate that such service was not added earlier
                if (entity.containsProductTemplate(offerProductTemplate.getProductTemplate())) {
                    messages.error(new BundleKey("messages", "offerTemplate.alreadyContainsProduct"), offerProductTemplate.getProductTemplate().getDescriptionOrCode());
                    return;
                }

                offerProductTemplate.setOfferTemplate(entity);
                entity.addOfferProductTemplate(offerProductTemplate);
                messages.info(new BundleKey("messages", "offerTemplate.productTemplate.create.successful"));
            }

        } catch (Exception e) {
            log.error("Failed to save offer product template !", e.getMessage());
            messages.error(new BundleKey("messages", "save.unsuccessful"));
        }

        offerProductTemplate = null;
    }

    public void newOfferServiceTemplate() {
        this.offerServiceTemplate = new OfferServiceTemplate();
        this.incompatibleServices = null;
    }

    public OfferServiceTemplate getOfferServiceTemplate() {
        return offerServiceTemplate;
    }

    public void setOfferServiceTemplate(OfferServiceTemplate offerServiceTemplate) {
        this.offerServiceTemplate = offerServiceTemplate;
    }

    public DualListModel<ServiceTemplate> getIncompatibleServices() {

        if (incompatibleServices == null) {
            List<ServiceTemplate> source = null;
            if (offerServiceTemplate == null || offerServiceTemplate.isTransient()) {
                source = serviceTemplateService.listActive();
            } else {
                source = serviceTemplateService.listAllActiveExcept(offerServiceTemplate.getServiceTemplate());
            }

            List<ServiceTemplate> target = new ArrayList<ServiceTemplate>();

            if (offerServiceTemplate != null && offerServiceTemplate.getIncompatibleServices() != null && offerServiceTemplate.getIncompatibleServices().size() > 0) {
                target.addAll(offerServiceTemplate.getIncompatibleServices());
            }
            source.removeAll(target);
            incompatibleServices = new DualListModel<ServiceTemplate>(source, target);
        }
        return incompatibleServices;
    }

    public void setIncompatibleServices(DualListModel<ServiceTemplate> incompatibleServices) {
        this.incompatibleServices = incompatibleServices;
    }

    public OfferProductTemplate getOfferProductTemplate() {
        return offerProductTemplate;
    }

    public void setOfferProductTemplate(OfferProductTemplate offerProductTemplate) {
        this.offerProductTemplate = offerProductTemplate;
    }

    public Long getBomId() {
        return bomId;
    }

    public void setBomId(Long bomId) {
        this.bomId = bomId;
    }

    public void setNewVersion(boolean newVersion) {
        this.newVersion = newVersion;
    }

    public boolean isNewVersion() {
        return newVersion;
    }

    public ExportTemplate getMarketingCatalogExportTemplate() {
        return entityExportImportService.getExportImportTemplate("Offers");
    }

    public BusinessOfferModel getBusinessOfferModel() {
        return businessOfferModel;
    }

    public void setBusinessOfferModel(BusinessOfferModel businessOfferModel) {
        this.businessOfferModel = businessOfferModel;
    }

    public void onNameChange() {
        if (StringUtils.isEmpty(entity.getCode())) {
            entity.setCode(entity.getName());
        }
    }

    public void onCodeChange() {
        if (StringUtils.isEmpty(entity.getName())) {
            entity.setName(entity.getCode());
        }
    }

    @ActionMethod
    public void addProductTemplateToOffer(ProductTemplate productTemplate) {

        if (entity.containsProductTemplate(productTemplate)) {
            messages.error(new BundleKey("messages", "offerTemplate.alreadyContainsProduct"), productTemplate.getDescriptionOrCode());
            return;
        }

        OfferProductTemplate opt = new OfferProductTemplate();
        opt.setProductTemplate(productTemplate);
        opt.setOfferTemplate(entity);

        entity.addOfferProductTemplate(opt);

        messages.info(new BundleKey("messages", "offerTemplate.productTemplate.create.successful"));
    }

    public boolean displayStatus(OfferTemplate offer) {

        if ((Arrays.asList(LifeCycleStatusEnum.ACTIVE, LifeCycleStatusEnum.LAUNCHED, LifeCycleStatusEnum.IN_TEST).contains(offer.getLifeCycleStatus()))) {
            return offer.getValidityRaw() == null || offer.getValidityRaw().isCorrespondsToPeriod(new Date());
        }

        return false;
    }

    public boolean validateUniqueVersion(FacesContext context, List<UIInput> components, List<Object> values) {

        if (values.size() != 3) {
            throw new RuntimeException("Please bind validator to two components in the following order: offer/product/bundle template code, dateFrom, dateTo");
        }

        String code = (String) values.get(0);
        Date from = (Date) values.get(1);
        Date to = (Date) values.get(2);

        List<ProductOffering> matchedVersions = offerTemplateService.getMatchingVersions(code, from, to, entity.getId(), true);

        if (!matchedVersions.isEmpty()) {
            messages.error(new BundleKey("messages", "offerTemplate.version.exists"),
                matchedVersions.get(0).getValidityRaw() == null ? " / " : matchedVersions.get(0).getValidityRaw().toString(paramBean.getDateFormat()));
            return false;
        }

        return true;
    }

    @ActionMethod
    public void duplicateOfferOnly() {
        if (entity != null && entity.getId() != null) {
            try {
                entity = offerTemplateService.duplicateOfferOnly(entity);
                messages.info(new BundleKey("messages", "message.duplicate.ok"));
            } catch (BusinessException e) {
                log.error("Error encountered instantiating new offer template entity version: {}", entity.getCode(), e);
                messages.error(new BundleKey("messages", "message.duplicate.ok"));
            }
        }
    }
	
}