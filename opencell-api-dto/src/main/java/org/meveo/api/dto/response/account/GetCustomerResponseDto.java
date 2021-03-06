package org.meveo.api.dto.response.account;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.meveo.api.dto.account.CustomerDto;
import org.meveo.api.dto.response.BaseResponse;

/**
 * The Class GetCustomerResponseDto.
 *
 * @author Edward P. Legaspi
 */
@XmlRootElement(name = "GetCustomerResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetCustomerResponseDto extends BaseResponse {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1127961080391738415L;

    /** The customer. */
    private CustomerDto customer;

    /**
     * Gets the customer.
     *
     * @return the customer
     */
    public CustomerDto getCustomer() {
        return customer;
    }

    /**
     * Sets the customer.
     *
     * @param customer the new customer
     */
    public void setCustomer(CustomerDto customer) {
        this.customer = customer;
    }

    @Override
    public String toString() {
        return "GetCustomerResponse [customer=" + customer + ", toString()=" + super.toString() + "]";
    }
}