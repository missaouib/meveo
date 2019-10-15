package org.meveo.admin.jsf.converter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import org.apache.commons.lang.StringUtils;
import org.meveo.commons.utils.NumberUtils;
import org.meveo.model.catalog.RoundingModeEnum;

/**
 * Round a number depending on the number of decimal's digits and the rounding mode
 *
 * @author Khalid HORRI
 * @lastModifiedVersion 7.0
 */
@FacesConverter(value = "bigDecimalXDigitsConverter", managed = true)
public class BigDecimalXDigitsConverter extends BigDecimalConverter {

    private static final long serialVersionUID = -4022913574038089922L;

    /**
     * Number of digits in decimal part for a number
     */
    private Integer nbDecimal = NumberUtils.DEFAULT_NUMBER_DIGITS_DECIMAL_UI;
    /**
     * The rounding Mode.
     */
    private RoundingMode roundingMode = RoundingModeEnum.NEAREST.getRoundingMode();

    @Override
    public String getAsString(FacesContext facesContext, UIComponent uIComponent, BigDecimal obj) {
        if (obj == null || obj.toString().length() == 0) {
            return "";
        }

        String value = obj.toString();
        value = value.replace(" ", "");
        value = value.replace("\u00a0", "");
        return value;
    }

    @Override
    public BigDecimal getAsObject(FacesContext facesContext, UIComponent uIComponent, String str) {
        RoundingModeEnum roundingModeAttribute = (RoundingModeEnum) uIComponent.getAttributes().get("roundingMode");
        Integer nbDecimalAttribute = (Integer) uIComponent.getAttributes().get("nbDecimal");
        if (nbDecimalAttribute != null && nbDecimalAttribute != 0) {
            nbDecimal = nbDecimalAttribute;
        }
        if (roundingModeAttribute != null) {
            roundingMode = roundingModeAttribute.getRoundingMode();
        }
        BigDecimal number = super.getAsObject(facesContext, uIComponent, str);
        number = NumberUtils.round(number, nbDecimal, roundingMode);
        return number;
    }

    @Override
    protected DecimalFormat getDecimalFormat() {
        DecimalFormatSymbols decimalFormatSymbol = new DecimalFormatSymbols(FacesContext.getCurrentInstance().getViewRoot().getLocale());
        String suffixPattern = StringUtils.repeat("0", nbDecimal);
        return new DecimalFormat("#,##0." + suffixPattern, decimalFormatSymbol);
    }
}
