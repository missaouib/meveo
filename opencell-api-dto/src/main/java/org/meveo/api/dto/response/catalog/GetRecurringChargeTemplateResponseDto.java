package org.meveo.api.dto.response.catalog;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.meveo.api.dto.catalog.RecurringChargeTemplateDto;
import org.meveo.api.dto.response.BaseResponse;

/**
 * The Class GetRecurringChargeTemplateResponseDto.
 *
 * @author Edward P. Legaspi
 */
@XmlRootElement(name = "GetRecurringChargeTemplateResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetRecurringChargeTemplateResponseDto extends BaseResponse {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -2699333443036516206L;

    /** The recurring charge template. */
    private RecurringChargeTemplateDto recurringChargeTemplate;

    /**
     * Gets the recurring charge template.
     *
     * @return the recurring charge template
     */
    public RecurringChargeTemplateDto getRecurringChargeTemplate() {
        return recurringChargeTemplate;
    }

    /**
     * Sets the recurring charge template.
     *
     * @param recurringChargeTemplate the new recurring charge template
     */
    public void setRecurringChargeTemplate(RecurringChargeTemplateDto recurringChargeTemplate) {
        this.recurringChargeTemplate = recurringChargeTemplate;
    }

    @Override
    public String toString() {
        return "GetRecurringChargeTemplateResponse [recurringChargeTemplate=" + recurringChargeTemplate + ", toString()=" + super.toString() + "]";
    }
}