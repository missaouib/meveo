/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */

package org.meveo.api.dto.response.catalog;

import org.meveo.api.dto.catalog.PricePlansDto;
import org.meveo.api.dto.response.SearchResponse;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The Class PricePlanMatrixesResponseDto.
 *
 * @author Edward P. Legaspi
 */
@XmlRootElement(name = "PricePlanMatrixesResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class PricePlanMatrixesResponseDto extends SearchResponse {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -7527987531474820250L;

    /** The price plan matrixes. */
    private PricePlansDto pricePlanMatrixes;

    /**
     * Gets the price plan matrixes.
     *
     * @return the price plan matrixes
     */
    public PricePlansDto getPricePlanMatrixes() {
        if (pricePlanMatrixes == null) {
            pricePlanMatrixes = new PricePlansDto();
        }
        return pricePlanMatrixes;
    }

    /**
     * Sets the price plan matrixes.
     *
     * @param pricePlanMatrixes the new price plan matrixes
     */
    public void setPricePlanMatrixes(PricePlansDto pricePlanMatrixes) {
        this.pricePlanMatrixes = pricePlanMatrixes;
    }

    @Override
    public String toString() {
        return "PricePlanMatrixesResponseDto [pricePlanMatrixes=" + pricePlanMatrixes + ", getActionStatus()=" + getActionStatus() + "]";
    }
}