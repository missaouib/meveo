package org.meveo.service.notification.sms;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import org.meveo.api.dto.response.notification.SMSInfoResponseDTO;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.model.crm.Customer;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.shared.ContactInformation;
import org.meveo.service.crm.impl.CustomerService;

import org.meveo.sms.SMSProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class SMSService {

    private static Logger log = LoggerFactory.getLogger(SMSService.class);

    @Inject
    private CustomerService customerService;

    public SMSInfoResponseDTO send(Communication communication) {
        SMS sms = buildSmsInfo(communication);
        SMSGateWay smsGateWay = SMSProviderFactory.create();
        MessageResponse response = smsGateWay.send(sms);
        log.info(format("Account SID %s sent SMS to %s with status %s",
                response.getSid(), response.getSentTo(), response.getStatus()));
        return toResponseDto(response);
    }

    private SMS buildSmsInfo(Communication communication) {
        String phoneNumberTo = fromCustomer(communication.getCustomerCode());
        return new SMS(phoneNumberTo, communication.getMessage());
    }

    private String fromCustomer(String code) {
        Customer customer = ofNullable(customerService.findByCode(code))
                .orElseThrow(() -> new MeveoApiException("Customer not found"));
        return customer.getCustomerAccounts()
                .stream()
                .findFirst()
                .map(CustomerAccount::getContactInformation)
                .map(ContactInformation::getMobile)
                .orElseThrow(() -> new MeveoApiException("Customer contact information is missing"));
    }

    private SMSInfoResponseDTO toResponseDto(MessageResponse message) {
        SMSInfoResponseDTO response = new SMSInfoResponseDTO();
        response.setStatus(message.getStatus());
        response.setBody(message.getBody());
        response.setUri(message.getUri());
        response.setSentTo(message.getSentTo());
        response.setPrice(message.getPrice());
        ofNullable(message.getErrorCode()).ifPresent(response::setErrorCode);
        ofNullable(message.getErrorMessage()).ifPresent(response::setErrorMessage);
        return response;
    }
}