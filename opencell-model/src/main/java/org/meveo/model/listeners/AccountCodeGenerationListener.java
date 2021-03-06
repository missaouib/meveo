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
package org.meveo.model.listeners;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import org.meveo.model.AccountEntity;

public class AccountCodeGenerationListener {

    @PrePersist
    public void prePersist(AccountEntity account) {
        //TODO : remove this code for two reasons
        // 1 - the identifier is not created yet
        // 2 - to have a custom code, there is an "adm_custom_generic_entity_code" table which must be added
        if (account.getCode() == null) {
            account.setCode("A" + account.getId());
        }
        if (account.getCode() != null && account.isAppendGeneratedCode()) {
            account.setCode(account.getCode() + "_" + "A" + account.getId());
        }
    }

    @PreUpdate
    public void preUpdate(AccountEntity account) {
        if (account.getCode() == null) {
            account.setCode("A" + account.getId());
        }
        if (account.getCode() != null && account.isAppendGeneratedCode()) {
            account.setCode(account.getCode() + "_" + "A" + account.getId());
        }
    }

}
