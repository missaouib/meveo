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
package org.meveo.admin.action.billing;

import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.StatelessBaseBean;
import org.meveo.model.billing.SubscriptionTerminationReason;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.TerminationReasonService;

@Named
@ConversationScoped
public class TerminationReasonBean extends
		StatelessBaseBean<SubscriptionTerminationReason> {

	private static final long serialVersionUID = 3745349578244346473L;

	@Inject
	private TerminationReasonService terminationReasonService;

	public TerminationReasonBean() {
		super(SubscriptionTerminationReason.class);
	}

	@Override
	protected IPersistenceService<SubscriptionTerminationReason> getPersistenceService() {
		return terminationReasonService;
	}

	@Override
	protected String getListViewName() {
		return "terminationReasons";
	}

	@Override
	public String getEditViewName() {
		return "terminationReasonDetail";
	}

	@Override
	protected List<String> getFormFieldsToFetch() {
		return Arrays.asList("provider");
	}

}
