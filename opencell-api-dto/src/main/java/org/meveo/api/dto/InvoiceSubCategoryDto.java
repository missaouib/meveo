package org.meveo.api.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.meveo.model.billing.InvoiceSubCategory;

/**
 * The Class InvoiceSubCategoryDto.
 *
 * @author Edward P. Legaspi
 * @lastModifiedVersion 5.0
 */
@XmlRootElement(name = "InvoiceSubCategory")
@XmlAccessorType(XmlAccessType.FIELD)
public class InvoiceSubCategoryDto extends BusinessEntityDto {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1832246068609179546L;

    /** The invoice category. */
    @XmlElement(required = true)
    private String invoiceCategory;

    /** The accounting code. */
    @XmlElement(required = true)
    private String accountingCode;

    /** The language descriptions. */
    private List<LanguageDescriptionDto> languageDescriptions;

    /** The custom fields. */
    private CustomFieldsDto customFields;
    
    private String taxScriptScode;

    /** The occ template code. */
    @XmlElement(required = true)
    private String occTemplateCode;

    /** The occ template negative code. */
    private String occTemplateNegativeCode;   
    
    /**
     * Instantiates a new invoice sub category dto.
     */
    public InvoiceSubCategoryDto() {

    }

    /**
     * Instantiates a new invoice sub category dto.
     *
     * @param invoiceSubCategory the invoice sub category
     * @param customFieldInstances the custom field instances
     */
    public InvoiceSubCategoryDto(InvoiceSubCategory invoiceSubCategory, CustomFieldsDto customFieldInstances) {
        super(invoiceSubCategory);
        invoiceCategory = invoiceSubCategory.getInvoiceCategory().getCode();
        if (invoiceSubCategory.getAccountingCode() != null) {
            accountingCode = invoiceSubCategory.getAccountingCode().getCode();
        }
        if(invoiceSubCategory.getTaxScript() != null) {
            taxScriptScode = invoiceSubCategory.getTaxScript().getCode();
        }
        customFields = customFieldInstances;
        setLanguageDescriptions(LanguageDescriptionDto.convertMultiLanguageFromMapOfValues(invoiceSubCategory.getDescriptionI18n()));
        
        if(invoiceSubCategory.getOccTemplate() != null) {
            occTemplateCode = invoiceSubCategory.getOccTemplate().getCode();
        }
        
        if(invoiceSubCategory.getOccTemplateNegative() != null) {
            occTemplateNegativeCode = invoiceSubCategory.getOccTemplateNegative().getCode();
        }
    }

    /**
     * Gets the invoice category.
     *
     * @return the invoice category
     */
    public String getInvoiceCategory() {
        return invoiceCategory;
    }

    /**
     * Sets the invoice category.
     *
     * @param invoiceCategory the new invoice category
     */
    public void setInvoiceCategory(String invoiceCategory) {
        this.invoiceCategory = invoiceCategory;
    }

    /**
     * Gets the language descriptions.
     *
     * @return the language descriptions
     */
    public List<LanguageDescriptionDto> getLanguageDescriptions() {
        return languageDescriptions;
    }

    /**
     * Sets the language descriptions.
     *
     * @param languageDescriptions the new language descriptions
     */
    public void setLanguageDescriptions(List<LanguageDescriptionDto> languageDescriptions) {
        this.languageDescriptions = languageDescriptions;
    }

    /**
     * Gets the accounting code.
     *
     * @return the accounting code
     */
    public String getAccountingCode() {
        return accountingCode;
    }

    /**
     * Sets the accounting code.
     *
     * @param accountingCode the new accounting code
     */
    public void setAccountingCode(String accountingCode) {
        this.accountingCode = accountingCode;
    }

    /**
     * Gets the custom fields.
     *
     * @return the customFields
     */
    public CustomFieldsDto getCustomFields() {
        return customFields;
    }

    /**
     * Sets the custom fields.
     *
     * @param customFields the customFields to set
     */
    public void setCustomFields(CustomFieldsDto customFields) {
        this.customFields = customFields;
    }

    @Override
	public String toString() {
		return "InvoiceSubCategoryDto [invoiceCategory=" + invoiceCategory + ", accountingCode=" + accountingCode + ", languageDescriptions=" + languageDescriptions
				+ ", customFields=" + customFields + ", taxScriptScode=" + taxScriptScode + "]";
	}

	public String getTaxScriptScode() {
		return taxScriptScode;
	}

	public void setTaxScriptScode(String taxScriptScode) {
		this.taxScriptScode = taxScriptScode;
	}

    /**
     * @return the occTemplateCode
     */
    public String getOccTemplateCode() {
        return occTemplateCode;
    }

    /**
     * @param occTemplateCode the occTemplateCode to set
     */
    public void setOccTemplateCode(String occTemplateCode) {
        this.occTemplateCode = occTemplateCode;
    }

    /**
     * @return the occTemplateNegativeCode
     */
    public String getOccTemplateNegativeCode() {
        return occTemplateNegativeCode;
    }

    /**
     * @param occTemplateNegativeCode the occTemplateNegativeCode to set
     */
    public void setOccTemplateNegativeCode(String occTemplateNegativeCode) {
        this.occTemplateNegativeCode = occTemplateNegativeCode;
    }

}
