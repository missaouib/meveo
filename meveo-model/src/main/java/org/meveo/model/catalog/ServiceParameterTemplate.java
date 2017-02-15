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
package org.meveo.model.catalog;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;

import org.meveo.model.BusinessEntity;
import org.meveo.model.ExportIdentifier;

@Entity
@ExportIdentifier({ "code"})
@Table(name = "RM_SERVICE_PARAM_TEMPLATE", uniqueConstraints = @UniqueConstraint(columnNames = { "CODE"}))
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "RM_SERVICE_PARAM_TEMPLATE_SEQ")
public class ServiceParameterTemplate extends BusinessEntity {

	private static final long serialVersionUID = 1L;

	@Column(name = "VALUE", length = 255)
    @Size(max = 255)
	private String value;

	@Column(name = "NAME", length = 255)
	@Size(max = 255)
	private String name;

	@Column(name = "CUMULATIVE_PERIODS")
	private Integer cumulativePeriods;

	@Column(name = "DEFAULT_VALUE", length = 255)
    @Size(max = 255)
	private String defaultValue;

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Integer getCumulativePeriods() {
		return cumulativePeriods;
	}

	public void setCumulativePeriods(Integer cumulativePeriods) {
		this.cumulativePeriods = cumulativePeriods;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String toString() {
		return name;
	}

}
