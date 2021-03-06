/**
 * 
 */
package org.meveo.api.dto.payment;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.meveo.api.dto.response.SearchResponse;

/**
 * The Class PaymentGatewayResponseDto.
 *
 * @author anasseh
 */

@XmlRootElement(name = "PaymentGatewayResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class PaymentGatewayResponseDto extends SearchResponse {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -3151651854190686940L;

    /** The payment gateways. */
    @XmlElementWrapper(name = "paymentGateways")
    @XmlElement(name = "paymentGateway")
    private List<PaymentGatewayDto> paymentGateways = new ArrayList<PaymentGatewayDto>();

    /**
     * Instantiates a new payment gateway response dto.
     */
    public PaymentGatewayResponseDto() {

    }

    /**
     * Gets the payment gateways.
     *
     * @return the paymentGateways
     */
    public List<PaymentGatewayDto> getPaymentGateways() {
        return paymentGateways;
    }

    /**
     * Sets the payment gateways.
     *
     * @param paymentGateways the paymentGateways to set
     */
    public void setPaymentGateways(List<PaymentGatewayDto> paymentGateways) {
        this.paymentGateways = paymentGateways;
    }
}
