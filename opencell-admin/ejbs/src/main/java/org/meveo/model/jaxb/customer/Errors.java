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
 * along with this program.  If not, see <http://www.gnu.org/licenses/&gt;.
 */
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.2-147 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.02.01 at 08:25:37 PM WET 
//


package org.meveo.model.jaxb.customer;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{}errorCustomer" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{}errorSeller" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{}errorCustomerAccount" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	    "errorSeller",
    "errorCustomer",
    "errorCustomerAccount"
})
@XmlRootElement(name = "errors")
public class Errors {

    protected List<ErrorCustomer> errorCustomer;
    protected List<ErrorSeller> errorSeller;
    protected List<ErrorCustomerAccount> errorCustomerAccount;

    /**
     * Gets the value of the errorCustomer property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the errorCustomer property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getErrorCustomer().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ErrorCustomer }
     * @return list of error customer.
     * 
     */
    public List<ErrorCustomer> getErrorCustomer() {
        if (errorCustomer == null) {
            errorCustomer = new ArrayList<ErrorCustomer>();
        }
        return this.errorCustomer;
    }

    public List<ErrorSeller> getErrorSeller() {
        if (errorSeller == null) {
        	errorSeller = new ArrayList<ErrorSeller>();
        }
        return this.errorSeller;
    }
    
    /**
     * Gets the value of the errorCustomerAccount property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the errorCustomerAccount property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getErrorCustomerAccount().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ErrorCustomerAccount }
     * @return list of error customer account.
     * 
     */
    public List<ErrorCustomerAccount> getErrorCustomerAccount() {
        if (errorCustomerAccount == null) {
            errorCustomerAccount = new ArrayList<ErrorCustomerAccount>();
        }
        return this.errorCustomerAccount;
    }

}
