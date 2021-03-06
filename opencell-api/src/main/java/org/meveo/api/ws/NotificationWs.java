package org.meveo.api.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.notification.EmailNotificationDto;
import org.meveo.api.dto.notification.ScriptNotificationDto;
import org.meveo.api.dto.notification.WebHookDto;
import org.meveo.api.dto.response.notification.GetEmailNotificationResponseDto;
import org.meveo.api.dto.response.notification.GetScriptNotificationResponseDto;
import org.meveo.api.dto.response.notification.GetWebHookNotificationResponseDto;
import org.meveo.api.dto.response.notification.InboundRequestsResponseDto;
import org.meveo.api.dto.response.notification.NotificationHistoriesResponseDto;

/**
 * @author Edward P. Legaspi
 **/
@WebService
public interface NotificationWs extends IBaseWs {

    // notification

    @WebMethod
    ActionStatus createNotification(@WebParam(name = "notification") ScriptNotificationDto postData);

    @WebMethod
    ActionStatus updateNotification(@WebParam(name = "notification") ScriptNotificationDto postData);

    @WebMethod
    GetScriptNotificationResponseDto findNotification(@WebParam(name = "notificationCode") String notificationCode);

    @WebMethod
    ActionStatus removeNotification(@WebParam(name = "notificationCode") String notificationCode);

    @WebMethod
    ActionStatus createOrUpdateNotification(@WebParam(name = "notification") ScriptNotificationDto postData);

    /**
     * Enable a Script type notification by its code
     * 
     * @param code Script type notification code
     * @return Request processing status
     */
    @WebMethod
    ActionStatus enableNotification(@WebParam(name = "code") String code);

    /**
     * Disable a Script type notification by its code
     * 
     * @param code Script type notification code
     * @return Request processing status
     */
    @WebMethod
    ActionStatus disableNotification(@WebParam(name = "code") String code);

    // webHook

    @WebMethod
    ActionStatus createWebHookNotification(@WebParam(name = "notification") WebHookDto postData);

    @WebMethod
    ActionStatus updateWebHookNotification(@WebParam(name = "notification") WebHookDto postData);

    @WebMethod
    GetWebHookNotificationResponseDto findWebHookNotification(@WebParam(name = "notificationCode") String notificationCode);

    @WebMethod
    ActionStatus removeWebHookNotification(@WebParam(name = "notificationCode") String notificationCode);

    @WebMethod
    ActionStatus createOrUpdateWebHookNotification(@WebParam(name = "notification") WebHookDto postData);

    /**
     * Enable a Webhook notification by its code
     * 
     * @param code Webhook notification code
     * @return Request processing status
     */
    @WebMethod
    ActionStatus enableWebHookNotification(@WebParam(name = "code") String code);

    /**
     * Disable a Webhook notification by its code
     * 
     * @param code Webhook notification code
     * @return Request processing status
     */
    @WebMethod
    ActionStatus disableWebHookNotification(@WebParam(name = "code") String code);

    // email

    @WebMethod
    ActionStatus createEmailNotification(@WebParam(name = "notification") EmailNotificationDto postData);

    @WebMethod
    ActionStatus updateEmailNotification(@WebParam(name = "notification") EmailNotificationDto postData);

    @WebMethod
    GetEmailNotificationResponseDto findEmailNotification(@WebParam(name = "notificationCode") String notificationCode);

    @WebMethod
    ActionStatus removeEmailNotification(@WebParam(name = "notificationCode") String notificationCode);

    @WebMethod
    ActionStatus createOrUpdateEmailNotification(@WebParam(name = "notification") EmailNotificationDto postData);

    /**
     * Enable a Email notification by its code
     * 
     * @param code Email notification code
     * @return Request processing status
     */
    @WebMethod
    ActionStatus enableEmailNotification(@WebParam(name = "code") String code);

    /**
     * Disable a Email notification by its code
     * 
     * @param code Email notification code
     * @return Request processing status
     */
    @WebMethod
    ActionStatus disableEmailNotification(@WebParam(name = "code") String code);

    // history

    @WebMethod
    NotificationHistoriesResponseDto listNotificationHistory();

    @WebMethod
    InboundRequestsResponseDto listInboundRequest();

}
