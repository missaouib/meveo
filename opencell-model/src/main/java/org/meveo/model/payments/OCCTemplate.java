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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.model.payments;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.BusinessEntity;
import org.meveo.model.ExportIdentifier;
import org.meveo.model.billing.AccountingCode;

/**
 * @author Edward P. Legaspi
 * @lastModifiedVersion 5.0
 */
@Entity
@Cacheable
@ExportIdentifier({ "code" })
@Table(name = "ar_occ_template", uniqueConstraints = @UniqueConstraint(columnNames = { "code" }))
@GenericGenerator(name = "ID_GENERATOR", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
        @Parameter(name = "sequence_name", value = "ar_occ_template_seq"), })
public class OCCTemplate extends BusinessEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounting_code_id")
    private AccountingCode accountingCode;

    /**
     * @deprecated As of version 5.0. All accountingCode now use {@link #accountingCode}
     */
    @Deprecated
    @Column(name = "account_code_client_side", length = 255)
    @Size(max = 255)
    private String accountCodeClientSide;

    @Column(name = "occ_category")
    @Enumerated(EnumType.STRING)
    private OperationCategoryEnum occCategory;

    public String getAccountCodeClientSide() {
        return accountCodeClientSide;
    }

    public void setAccountCodeClientSide(String accountCodeClientSide) {
        this.accountCodeClientSide = accountCodeClientSide;
    }

    public OperationCategoryEnum getOccCategory() {
        return occCategory;
    }

    public void setOccCategory(OperationCategoryEnum occCategory) {
        this.occCategory = occCategory;
    }

    @Override
    public int hashCode() {
        return 961 + ("OCCTemplate" + code).hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof OCCTemplate)) {
            return false;
        }

        OCCTemplate other = (OCCTemplate) obj;
        if (id != null && other.getId() != null && id.equals(other.getId())) {
            return true;
        }

        if (code == null) {
            if (other.code != null)
                return false;
        } else if (!code.equals(other.code))
            return false;
        return true;
    }

    public AccountingCode getAccountingCode() {
        return accountingCode;
    }

    public void setAccountingCode(AccountingCode accountingCode) {
        this.accountingCode = accountingCode;
    }

}
