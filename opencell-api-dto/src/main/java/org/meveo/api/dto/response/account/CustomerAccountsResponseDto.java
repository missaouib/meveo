package org.meveo.api.dto.response.account;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.meveo.api.dto.account.CustomerAccountsDto;
import org.meveo.api.dto.response.BaseResponse;

/**
 * The Class CustomerAccountsResponseDto.
 *
 * @author Edward P. Legaspi
 */
@XmlRootElement(name = "CustomerAccountsResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class CustomerAccountsResponseDto extends BaseResponse {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 7705676034964165327L;

    /** The customer accounts. */
    private CustomerAccountsDto customerAccounts = new CustomerAccountsDto();

    /**
     * Gets the customer accounts.
     *
     * @return the customer accounts
     */
    public CustomerAccountsDto getCustomerAccounts() {
        return customerAccounts;
    }

    /**
     * Sets the customer accounts.
     *
     * @param customerAccounts the new customer accounts
     */
    public void setCustomerAccounts(CustomerAccountsDto customerAccounts) {
        this.customerAccounts = customerAccounts;
    }

    @Override
    public String toString() {
        return "ListCustomerAccountResponseDto [customerAccounts=" + customerAccounts + ", toString()=" + super.toString() + "]";
    }
}