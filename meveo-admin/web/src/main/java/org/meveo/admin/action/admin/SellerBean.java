/*
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.admin.action.admin;

import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.admin.action.StatelessBaseBean;
import org.meveo.model.admin.Seller;
import org.meveo.service.admin.impl.SellerService;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;

@Named
@ConversationScoped
public class SellerBean extends StatelessBaseBean<Seller> {

	private static final long serialVersionUID = 1L;

	/**
	 * Injected @{link PricePlanMatrix} service. Extends
	 * {@link PersistenceService}.
	 */
	@Inject
	private SellerService sellerService;

	/**
	 * Constructor. Invokes super constructor and provides class type of this
	 * bean for {@link BaseBean}.
	 */
	public SellerBean() {
		super(Seller.class);
	}

	/**
	 * Factory method for entity to edit. If objectId param set load that entity
	 * from database, otherwise create new.
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */

	public Seller initEntity() {
		return super.initEntity();
	}

	/**
	 * Override default list view name. (By default its class name starting
	 * lower case + 's').
	 * 
	 * @see org.meveo.admin.action.BaseBean#getDefaultViewName()
	 */
	protected String getDefaultViewName() {
		return "sellers";
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getPersistenceService()
	 */
	@Override
	protected IPersistenceService<Seller> getPersistenceService() {
		return sellerService;
	}

	@Override
	protected String getListViewName() {
		return "sellers";
	}

	@Override
	protected String getDefaultSort() {
		return "code";
	}

	@Override
	protected List<String> getFormFieldsToFetch() {
		return Arrays.asList("provider");
	}

}