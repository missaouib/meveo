package org.meveo.api;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.meveo.api.dto.CustomFieldTemplateDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.model.admin.User;
import org.meveo.model.catalog.Calendar;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.Provider;
import org.meveo.model.crm.custom.CustomFieldMapKeyEnum;
import org.meveo.model.crm.custom.CustomFieldStorageTypeEnum;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.service.catalog.impl.CalendarService;
import org.meveo.service.crm.impl.CustomFieldTemplateService;

/**
 * @author Edward P. Legaspi
 **/
@Stateless
public class CustomFieldTemplateApi extends BaseApi {

    @Inject
    private CalendarService calendarService;

    @Inject
    private CustomFieldTemplateService customFieldTemplateService;

    public void create(CustomFieldTemplateDto postData, String appliesTo, User currentUser) throws MeveoApiException {

        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(postData.getDescription())) {
            missingParameters.add("description");
        }
        if (appliesTo == null && StringUtils.isBlank(postData.getAccountLevel()) && StringUtils.isBlank(postData.getAppliesTo())) {
            missingParameters.add("appliesTo");
        }
        if (postData.getFieldType() == null) {
            missingParameters.add("fieldType");
        }
        if (postData.getStorageType() == null) {
            missingParameters.add("storageType");
        }

        if (!missingParameters.isEmpty()) {
            throw new MissingParameterException(getMissingParametersExceptionMessage());
        }

        if (appliesTo != null) {
            postData.setAppliesTo(appliesTo);

        } else {

            // Support for old API
            if (postData.getAppliesTo() == null && postData.getAccountLevel() != null) {
                appliesTo = postData.getAccountLevel();
            } else {
                appliesTo = postData.getAppliesTo();
            }
        }

        if (customFieldTemplateService.findByCodeAndAppliesToNoCache(postData.getCode(), appliesTo, currentUser.getProvider()) != null) {
            throw new EntityAlreadyExistsException(CustomFieldTemplate.class, postData.getCode());
        }

        CustomFieldTemplate cft = fromDTO(postData, currentUser, appliesTo, null);
        customFieldTemplateService.create(cft, currentUser, currentUser.getProvider());

    }

    public void update(CustomFieldTemplateDto postData, String appliesTo, User currentUser) throws MeveoApiException {

        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(postData.getDescription())) {
            missingParameters.add("description");
        }
        if (appliesTo == null && StringUtils.isBlank(postData.getAccountLevel()) && StringUtils.isBlank(postData.getAppliesTo())) {
            missingParameters.add("appliesTo");
        }
        if (postData.getFieldType() == null) {
            missingParameters.add("fieldType");
        }
        if (postData.getStorageType() == null) {
            missingParameters.add("storageType");
        }

        if (!missingParameters.isEmpty()) {
            throw new MissingParameterException(getMissingParametersExceptionMessage());
        }

        if (appliesTo != null) {
            postData.setAppliesTo(appliesTo);

        } else {
            // Support for old API
            if (postData.getAppliesTo() == null && postData.getAccountLevel() != null) {
                appliesTo = postData.getAccountLevel();
            } else {
                appliesTo = postData.getAppliesTo();
            }
        }

        CustomFieldTemplate cft = customFieldTemplateService.findByCodeAndAppliesToNoCache(postData.getCode(), appliesTo, currentUser.getProvider());
        if (cft == null) {
            throw new EntityDoesNotExistsException(CustomFieldTemplate.class, postData.getCode());
        }

        cft = fromDTO(postData, currentUser, appliesTo, cft);

        customFieldTemplateService.update(cft, currentUser);

    }

    public void remove(String code, String appliesTo, Provider provider) throws EntityDoesNotExistsException, MissingParameterException {
        if (StringUtils.isBlank(code)) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(appliesTo)) {
            missingParameters.add("appliesTo");
        }
        if (!missingParameters.isEmpty()) {
            throw new MissingParameterException(getMissingParametersExceptionMessage());
        }

        CustomFieldTemplate cft = customFieldTemplateService.findByCodeAndAppliesTo(code, appliesTo, provider);
        if (cft != null) {
            customFieldTemplateService.remove(cft.getId());
        } else {
            throw new EntityDoesNotExistsException(CustomFieldTemplate.class, code);
        }
    }

    public CustomFieldTemplateDto find(String code, String appliesTo, Provider provider) throws EntityDoesNotExistsException, MissingParameterException {
        if (StringUtils.isBlank(code)) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(appliesTo)) {
            missingParameters.add("appliesTo");
        }
        if (!missingParameters.isEmpty()) {
            throw new MissingParameterException(getMissingParametersExceptionMessage());
        }

        CustomFieldTemplate cft = customFieldTemplateService.findByCodeAndAppliesTo(code, appliesTo, provider);

        if (cft == null) {
            throw new EntityDoesNotExistsException(CustomFieldTemplate.class, code);
        }
        return new CustomFieldTemplateDto(cft);
    }

    public void createOrUpdate(CustomFieldTemplateDto postData, String appliesTo, User currentUser) throws MeveoApiException {
        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("code");
        }

        if (appliesTo == null && StringUtils.isBlank(postData.getAccountLevel()) && StringUtils.isBlank(postData.getAppliesTo())) {
            missingParameters.add("appliesTo");
        }

        if (!missingParameters.isEmpty()) {
            throw new MissingParameterException(getMissingParametersExceptionMessage());
        }

        if (appliesTo != null) {
            postData.setAppliesTo(appliesTo);

        } else {
            // Support for old API
            if (postData.getAppliesTo() == null && postData.getAccountLevel() != null) {
                appliesTo = postData.getAccountLevel();
            } else {
                appliesTo = postData.getAppliesTo();
            }
        }

        CustomFieldTemplate customFieldTemplate = customFieldTemplateService.findByCodeAndAppliesToNoCache(postData.getCode(), appliesTo, currentUser.getProvider());
        
        if (customFieldTemplate == null) {
            create(postData, appliesTo, currentUser);
        } else {
            update(postData, appliesTo, currentUser);
        }
    }

    protected CustomFieldTemplate fromDTO(CustomFieldTemplateDto dto, User currentUser, String appliesTo, CustomFieldTemplate cftToUpdate) {

        // Set default values
        if (CustomFieldTypeEnum.STRING.name().equals(dto.getFieldType()) && dto.getMaxValue() == null) {
            dto.setMaxValue(CustomFieldTemplate.DEFAULT_MAX_LENGTH_STRING);
        }

        CustomFieldTemplate cft = new CustomFieldTemplate();
        if (cftToUpdate != null) {
            cft = cftToUpdate;
        }
        cft.setCode(dto.getCode());
        cft.setDescription(dto.getDescription());
        if (appliesTo == null) {

            // Support for old API
            if (dto.getAccountLevel() != null) {
                appliesTo = dto.getAccountLevel();
            } else {
                appliesTo = dto.getAppliesTo();
            }
        }
        cft.setAppliesTo(appliesTo);
        cft.setFieldType(dto.getFieldType());
        cft.setDefaultValue(dto.getDefaultValue());
        cft.setStorageType(dto.getStorageType());
        cft.setValueRequired(dto.isValueRequired());
        cft.setVersionable(dto.isVersionable());
        cft.setTriggerEndPeriodEvent(dto.isTriggerEndPeriodEvent());
        cft.setEntityClazz(org.apache.commons.lang3.StringUtils.trimToNull(dto.getEntityClazz()));
        cft.setAllowEdit(dto.isAllowEdit());
        cft.setHideOnNew(dto.isHideOnNew());
        cft.setMinValue(dto.getMinValue());
        cft.setMaxValue(dto.getMaxValue());
        cft.setCacheValue(dto.isCacheValue());
        cft.setRegExp(dto.getRegExp());
        cft.setCacheValueTimeperiod(dto.getCacheValueTimeperiod());
        cft.setGuiPosition(dto.getGuiPosition());
        cft.setApplicableOnEl(dto.getApplicableOnEl());

        if (cft.getFieldType() == CustomFieldTypeEnum.LIST) {
            cft.setListValues(dto.getListValues());
        }

        cft.setMapKeyType(dto.getMapKeyType());

        if (cft.getStorageType() == CustomFieldStorageTypeEnum.MAP && cft.getMapKeyType() == null) {
            cft.setMapKeyType(CustomFieldMapKeyEnum.STRING);
        }

        if (!StringUtils.isBlank(dto.getCalendar())) {
            Calendar calendar = calendarService.findByCode(dto.getCalendar(), currentUser.getProvider());
            if (calendar != null) {
                cft.setCalendar(calendar);
            }
        }
        return cft;
    }
}
