package org.meveo.api.dto.account;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.meveo.api.dto.invoice.InvoiceDto;

/**
 * @author Edward P. Legaspi
 **/
@XmlType(name = "BillingAccount")
@XmlAccessorType(XmlAccessType.FIELD)
public class BillingAccountDto extends AccountDto {

	private static final long serialVersionUID = 8701417481481359155L;

	@XmlElement(required = true)
	private String customerAccount;

	@XmlElement(required = true)
	private String billingCycle;

	@XmlElement(required = true)
	private String country;

	@XmlElement(required = true)
	private String language;

	@XmlElement(required = true)
	private String paymentMethod;

	private Date nextInvoiceDate;
	private Date subscriptionDate;
	private Date terminationDate;
	private String paymentTerms;
	private Boolean electronicBilling;
	private String status;
	private String terminationReason;
	private String email;
	private BankCoordinatesDto bankCoordinates = new BankCoordinatesDto();
	private List<InvoiceDto> invoices = new ArrayList<InvoiceDto>();
	/**
	 * Use for GET / LIST only.
	 */
	private UserAccountsDto userAccounts = new UserAccountsDto();

	public BillingAccountDto() {

	}

	public String getCustomerAccount() {
		return customerAccount;
	}

	public void setCustomerAccount(String customerAccount) {
		this.customerAccount = customerAccount;
	}

	public String getBillingCycle() {
		return billingCycle;
	}

	public void setBillingCycle(String billingCycle) {
		this.billingCycle = billingCycle;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public Date getNextInvoiceDate() {
		return nextInvoiceDate;
	}

	public void setNextInvoiceDate(Date nextInvoiceDate) {
		this.nextInvoiceDate = nextInvoiceDate;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getPaymentTerms() {
		return paymentTerms;
	}

	public void setPaymentTerms(String paymentTerms) {
		this.paymentTerms = paymentTerms;
	}

	public Boolean getElectronicBilling() {
		return electronicBilling;
	}

	public void setElectronicBilling(Boolean electronicBilling) {
		this.electronicBilling = electronicBilling;
	}

	@Override
	public String toString() {
		return "BillingAccountDto [customerAccount=" + customerAccount + ", billingCycle=" + billingCycle + ", country=" + country + ", language=" + language + ", paymentMethod="
				+ paymentMethod + ", nextInvoiceDate=" + nextInvoiceDate + ", subscriptionDate=" + subscriptionDate + ", terminationDate=" + terminationDate + ", paymentTerms="
				+ paymentTerms + ", electronicBilling=" + electronicBilling + ", status=" + status + ", terminationReason=" + terminationReason + ", email=" + email
				+ ", bankCoordinates=" + bankCoordinates + ", userAccounts=" + userAccounts + "]";
	}

	public Date getSubscriptionDate() {
		return subscriptionDate;
	}

	public void setSubscriptionDate(Date subscriptionDate) {
		this.subscriptionDate = subscriptionDate;
	}

	public Date getTerminationDate() {
		return terminationDate;
	}

	public void setTerminationDate(Date terminationDate) {
		this.terminationDate = terminationDate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTerminationReason() {
		return terminationReason;
	}

	public void setTerminationReason(String terminationReason) {
		this.terminationReason = terminationReason;
	}

	public UserAccountsDto getUserAccounts() {
		return userAccounts;
	}

	public void setUserAccounts(UserAccountsDto userAccounts) {
		this.userAccounts = userAccounts;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public BankCoordinatesDto getBankCoordinates() {
		return bankCoordinates;
	}

	public void setBankCoordinates(BankCoordinatesDto bankCoordinates) {
		this.bankCoordinates = bankCoordinates;
	}

	public List<InvoiceDto> getInvoices() {
		return invoices;
	}

	public void setInvoices(List<InvoiceDto> invoices) {
		this.invoices = invoices;
	}

}
