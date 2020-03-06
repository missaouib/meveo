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
package org.meveo.model.admin;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Size;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.BaseEntity;

/**
 * Information about MEVEO inputs. Applications like Medina, Vertina or Oudaya receives input then processes it and then provide output. Source of input can be files, webservices,
 * JMS, database etc. Input usually has number of tickets that has to be processed. So this class holds information about number of tickets parsed from input and how much of them
 * were successfully processed and how much were rejected. If application specific input has more information it extends this entity.
 */
@Entity
@Table(name = "adm_input_history")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "input_type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("NOT_SPECIFIED")
@GenericGenerator(name = "ID_GENERATOR", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
        @Parameter(name = "sequence_name", value = "adm_input_history_seq"), })
public class InputHistory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", length = 255)
    @Size(max = 255)
    private String name;

    @Column(name = "start_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date analysisStartDate;

    @Column(name = "end_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date analysisEndDate;

    @Column(name = "parsed_tickets")
    private Integer parsedTickets;

    @Column(name = "succeeded_tickets")
    private Integer succeededTickets;

    @Column(name = "ignored_tickets")
    private Integer ignoredTickets;

    @Column(name = "rejected_tickets")
    private Integer rejectedTickets;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getAnalysisStartDate() {
        return analysisStartDate;
    }

    public void setAnalysisStartDate(Date analysisStartDate) {
        this.analysisStartDate = analysisStartDate;
    }

    public Date getAnalysisEndDate() {
        return analysisEndDate;
    }

    public void setAnalysisEndDate(Date analysisEndDate) {
        this.analysisEndDate = analysisEndDate;
    }

    public Integer getParsedTickets() {
        return parsedTickets;
    }

    public void setParsedTickets(Integer parsedTickets) {
        this.parsedTickets = parsedTickets;
    }

    public Integer getSucceededTickets() {
        return succeededTickets;
    }

    public void setSucceededTickets(Integer succeededTickets) {
        this.succeededTickets = succeededTickets;
    }

    public Integer getRejectedTickets() {
        return rejectedTickets;
    }

    public void setRejectedTickets(Integer rejectedTickets) {
        this.rejectedTickets = rejectedTickets;
    }

    public Integer getIgnoredTickets() {
        return ignoredTickets;
    }

    public void setIgnoredTickets(Integer ignoredTickets) {
        this.ignoredTickets = ignoredTickets;
    }

}
