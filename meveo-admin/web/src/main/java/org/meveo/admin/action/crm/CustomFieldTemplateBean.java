package org.meveo.admin.action.crm;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.crm.impl.CustomFieldTemplateService;


@Named
@ConversationScoped
public class CustomFieldTemplateBean extends BaseBean<CustomFieldTemplate> {

	private static final long serialVersionUID = 9099292371182275568L;

	@Inject
	CustomFieldTemplateService cftService;
	
	@Override
	protected IPersistenceService<CustomFieldTemplate> getPersistenceService() {
		return cftService;
	}

	@Override
	protected String getListViewName() {
		return "customFieldTemplates";
	}

	@Override
	public String getNewViewName() {
		return "customFieldTemplateDetail";
	}
	
	@Override
	protected String getDefaultSort() {
		return "code";
	}

}
