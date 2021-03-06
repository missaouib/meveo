package org.meveo.api.notification;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.BaseCrudApi;
import org.meveo.api.dto.notification.InboundRequestDto;
import org.meveo.api.dto.notification.InboundRequestsDto;
import org.meveo.api.dto.notification.NotificationHistoriesDto;
import org.meveo.api.dto.notification.NotificationHistoryDto;
import org.meveo.api.dto.notification.ScriptNotificationDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.InvalidParameterException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.catalog.CounterTemplate;
import org.meveo.model.notification.InboundRequest;
import org.meveo.model.notification.Notification;
import org.meveo.model.notification.NotificationHistory;
import org.meveo.model.notification.ScriptNotification;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.service.catalog.impl.CounterTemplateService;
import org.meveo.service.notification.InboundRequestService;
import org.meveo.service.notification.NotificationHistoryService;
import org.meveo.service.notification.ScriptNotificationService;
import org.meveo.service.script.ScriptInstanceService;

/**
 * @author Edward P. Legaspi
 * @author Andrius Karpavicius
 * @author akadid abdelmounaim
 * @lastModifiedVersion 5.0
 **/
@Stateless
public class ScriptNotificationApi extends BaseCrudApi<ScriptNotification, ScriptNotificationDto> {

    @Inject
    private ScriptNotificationService scriptNotificationService;

    @Inject
    private CounterTemplateService counterTemplateService;

    @Inject
    private NotificationHistoryService notificationHistoryService;

    @Inject
    private ScriptInstanceService scriptInstanceService;

    @Inject
    private InboundRequestService inboundRequestService;

    @Override
    public ScriptNotification create(ScriptNotificationDto postData) throws MeveoApiException, BusinessException {
        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(postData.getClassNameFilter())) {
            missingParameters.add("classNameFilter");
        }
        if (StringUtils.isBlank(postData.getEventTypeFilter())) {
            missingParameters.add("eventTypeFilter");
        }
        if (StringUtils.isBlank(postData.getScriptInstanceCode())) {
            missingParameters.add("scriptInstanceCode");
        }

        handleMissingParametersAndValidate(postData);

        if (scriptNotificationService.findByCode(postData.getCode()) != null) {
            throw new EntityAlreadyExistsException(Notification.class, postData.getCode());
        }
        ScriptInstance scriptInstance = null;
        if (!StringUtils.isBlank(postData.getScriptInstanceCode())) {
            scriptInstance = scriptInstanceService.findByCode(postData.getScriptInstanceCode());
            if (scriptInstance == null) {
                throw new EntityDoesNotExistsException(ScriptInstance.class, postData.getScriptInstanceCode());
            }
        }
        // check class
        try {
            Class.forName(postData.getClassNameFilter());
        } catch (Exception e) {
            throw new InvalidParameterException("classNameFilter", postData.getClassNameFilter());
        }

        CounterTemplate counterTemplate = null;
        if (!StringUtils.isBlank(postData.getCounterTemplate())) {
            counterTemplate = counterTemplateService.findByCode(postData.getCounterTemplate());
            if (counterTemplate == null) {
                throw new EntityDoesNotExistsException(CounterTemplate.class, postData.getCounterTemplate());
            }
        }

        ScriptNotification notif = new ScriptNotification();
        notif.setCode(postData.getCode());
        notif.setClassNameFilter(postData.getClassNameFilter());
        notif.setEventTypeFilter(postData.getEventTypeFilter());
        notif.setScriptInstance(scriptInstance);
        notif.setParams(postData.getScriptParams());
        notif.setElFilter(postData.getElFilter());
        notif.setCounterTemplate(counterTemplate);
        notif.setPriority(postData.getPriority());
        if (postData.isActive() != null) {
            notif.setActive(postData.isActive());
        } else if (postData.isDisabled() != null) {
            notif.setDisabled(postData.isDisabled());
        }

        scriptNotificationService.create(notif);

        return notif;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.meveo.api.ApiService#find(java.lang.String)
     */
    @Override
    public ScriptNotificationDto find(String notificationCode) throws EntityDoesNotExistsException, MissingParameterException, InvalidParameterException, MeveoApiException {
        ScriptNotificationDto result = new ScriptNotificationDto();

        if (!StringUtils.isBlank(notificationCode)) {
            ScriptNotification notif = scriptNotificationService.findByCode(notificationCode);

            if (notif == null) {
                throw new EntityDoesNotExistsException(Notification.class, notificationCode);
            }

            result = new ScriptNotificationDto(notif);
        } else {
            missingParameters.add("code");

            handleMissingParameters();
        }

        return result;
    }

    @Override
    public ScriptNotification update(ScriptNotificationDto postData) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(postData.getClassNameFilter())) {
            missingParameters.add("classNameFilter");
        }
        if (StringUtils.isBlank(postData.getEventTypeFilter())) {
            missingParameters.add("eventTypeFilter");
        }
        if (StringUtils.isBlank(postData.getScriptInstanceCode())) {
            missingParameters.add("scriptInstanceCode");
        }

        handleMissingParametersAndValidate(postData);

        ScriptNotification notif = scriptNotificationService.findByCode(postData.getCode());
        if (notif == null) {
            throw new EntityDoesNotExistsException(Notification.class, postData.getCode());
        }
        ScriptInstance scriptInstance = null;
        if (!StringUtils.isBlank(postData.getScriptInstanceCode())) {
            scriptInstance = scriptInstanceService.findByCode(postData.getScriptInstanceCode());
            if (scriptInstance == null) {
                throw new EntityDoesNotExistsException(ScriptInstance.class, postData.getScriptInstanceCode());
            }
        }
        // check class
        try {
            Class.forName(postData.getClassNameFilter());
        } catch (Exception e) {
            throw new InvalidParameterException("classNameFilter", postData.getClassNameFilter());
        }

        CounterTemplate counterTemplate = null;
        if (!StringUtils.isBlank(postData.getCounterTemplate())) {
            counterTemplate = counterTemplateService.findByCode(postData.getCounterTemplate());
            if (counterTemplate == null) {
                throw new EntityDoesNotExistsException(CounterTemplate.class, postData.getCounterTemplate());
            }
        }
        notif.setCode(StringUtils.isBlank(postData.getUpdatedCode()) ? postData.getCode() : postData.getUpdatedCode());
        notif.setClassNameFilter(postData.getClassNameFilter());
        notif.setEventTypeFilter(postData.getEventTypeFilter());
        notif.setScriptInstance(scriptInstance);
        notif.setElFilter(postData.getElFilter());
        notif.setCounterTemplate(counterTemplate);
        notif.setParams(postData.getScriptParams());
        notif.setPriority(postData.getPriority());

        notif = scriptNotificationService.update(notif);

        return notif;
    }

    public NotificationHistoriesDto listNotificationHistory() throws MeveoApiException {
        NotificationHistoriesDto result = new NotificationHistoriesDto();

        List<NotificationHistory> notificationHistories = notificationHistoryService.list();
        if (notificationHistories != null) {
            for (NotificationHistory nh : notificationHistories) {
                result.getNotificationHistory().add(new NotificationHistoryDto(nh));
            }
        }

        return result;
    }

    public InboundRequestsDto listInboundRequest() throws MeveoApiException {
        InboundRequestsDto result = new InboundRequestsDto();

        List<InboundRequest> inboundRequests = inboundRequestService.list();
        if (inboundRequests != null) {
            for (InboundRequest ir : inboundRequests) {
                result.getInboundRequest().add(new InboundRequestDto(ir));
            }
        }

        return result;
    }
}