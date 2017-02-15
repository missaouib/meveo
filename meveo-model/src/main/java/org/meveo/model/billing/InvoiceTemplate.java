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
package org.meveo.model.billing;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.meveo.model.BaseEntity;
import org.meveo.model.ExportIdentifier;

@Entity
@ExportIdentifier({ "code"})
@Table(name = "BILLING_INVOICE_TEMPLATE", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "CODE"}),
		@UniqueConstraint(columnNames = { "FILE_NAME" }) })
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "BILLING_INVOICE_TEMPLATE_SEQ")
public class InvoiceTemplate extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@Column(name = "CODE", nullable = false, length = 20)
	@Size(max = 20)
	@NotNull
	private String code;

	@Column(name = "TEMPLATE_VERSION", nullable = false, length = 255)
	@Size(max = 255)
    @NotNull
	private String templateVersion;

	@Column(name = "VALIDITY_START_DATE", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	@NotNull
	private Date validityStartDate;

	@Column(name = "VALIDITY_END_DATE", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	@NotNull
	private Date validityEndDate;

	@Column(name = "FILE_NAME", nullable = false, length = 255)
    @Size(max = 255)
	@NotNull
	private String fileName;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getTemplateVersion() {
		return templateVersion;
	}

	public void setTemplateVersion(String templateVersion) {
		this.templateVersion = templateVersion;
	}

	public Date getValidityStartDate() {
		return validityStartDate;
	}

	public void setValidityStartDate(Date validityStartDate) {
		this.validityStartDate = validityStartDate;
	}

	public Date getValidityEndDate() {
		return validityEndDate;
	}

	public void setValidityEndDate(Date validityEndDate) {
		this.validityEndDate = validityEndDate;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

}
