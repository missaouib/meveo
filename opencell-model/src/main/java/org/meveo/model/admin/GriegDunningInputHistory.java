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
package org.meveo.model.admin;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Information about inputs Grieg dunning received and processed.
 */
@Entity
@DiscriminatorValue("GRIEG_DUNNING")
public class GriegDunningInputHistory extends InputHistory {

    private static final long serialVersionUID = 1L;

    public GriegDunningInputHistory() {
    }

    /**
     * Convert from {@link InputHistory} to {@link GriegDunningInputHistory}.
     * 
     * @param inputInfo {@link InputHistory} superclass.
     */
    public GriegDunningInputHistory(InputHistory inputInfo) {
        // TODO this conversion is possible source of the bug (in case of
        // InputInfo changes)
        // so it should be either tested or generalized somehow.
        this.setId(inputInfo.getId());
        this.setVersion(inputInfo.getVersion());
        this.setAnalysisEndDate(inputInfo.getAnalysisEndDate());
        this.setAnalysisStartDate(inputInfo.getAnalysisStartDate());
        this.setName(inputInfo.getName());
        this.setParsedTickets(inputInfo.getParsedTickets());
        this.setSucceededTickets(inputInfo.getSucceededTickets());
        this.setRejectedTickets(inputInfo.getRejectedTickets());
        this.setIgnoredTickets(inputInfo.getIgnoredTickets());
    }

}
