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
package org.meveo.admin.action.catalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.MapUtils;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.catalog.OfferTemplateCategory;
import org.meveo.model.communication.MeveoInstance;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.catalog.impl.OfferTemplateCategoryService;
import org.primefaces.context.RequestContext;
import org.primefaces.model.LazyDataModel;

/**
 * The Class OfferTemplateListBean.
 * @author Said Ramli
 * @lastModifiedVersion 5.1
 */
@Named
@ConversationScoped
public class OfferTemplateListBean extends OfferTemplateBean {

	private static final long serialVersionUID = -3037867704912788024L;

	@Inject
    private OfferTemplateCategoryService offerTemplateCategoryService;
	
	private List<OfferTemplate> selectedOfferTemplates = new ArrayList<OfferTemplate>();
	private List<OfferTemplateCategory> selOfferTemplateCategories;
	private MeveoInstance meveoInstance = new MeveoInstance();
	private List<OfferTemplateCategory> activeOfferTemplateCategories;

    private long activeCount = 0;

    private long inactiveCount = 0;

    private long almostExpiredCount = 0;

    @Override
    public void preRenderView() {
        activeCount = offerTemplateService.countActive();
        inactiveCount = offerTemplateService.countDisabled();
        almostExpiredCount = offerTemplateService.countExpiring();
        super.preRenderView();
    }
	
	@Override
	protected List<String> getListFieldsToFetch() {
		return Arrays.asList("offerTemplateCategories");
	}

	public LazyDataModel<OfferTemplate> getLazyDataModelNoBSM() {
		filters.put("businessOfferModel", PersistenceService.SEARCH_IS_NULL);
		return getLazyDataModel(filters, listFiltered);
	}

	public void addForExport(OfferTemplate offerTemplate) {
		if (!selectedOfferTemplates.contains(offerTemplate)) {
			selectedOfferTemplates.add(offerTemplate);
		}
	}

	public void deleteForExport(OfferTemplate offerTemplate) {
		if (selectedOfferTemplates.contains(offerTemplate)) {
			selectedOfferTemplates.remove(offerTemplate);
		}
	}

	public void showSelectedOffersForExport() {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("resizable", false);
		options.put("draggable", false);
		options.put("modal", true);
		RequestContext.getCurrentInstance().openDialog("selectedOffersForExport", options, null);
	}


	public LazyDataModel<OfferTemplate> listFromBOM() {
		filters.put("businessOfferModel", PersistenceService.SEARCH_IS_NOT_NULL);
		if (selOfferTemplateCategories != null && selOfferTemplateCategories.size() > 0) {
			List<Long> offerTemplateCatIds = new ArrayList<>();
			for (OfferTemplateCategory otc : selOfferTemplateCategories) {
				offerTemplateCatIds.add(otc.getId());
			}
			filters.put("inList offerTemplateCategories.id", offerTemplateCatIds);
		}

		return getLazyDataModel();
	}
	
	@Override
	protected Map<String, Object> supplementSearchCriteria(Map<String, Object> searchCriteria) {
	    // 'name' filtering :
        if (MapUtils.isNotEmpty(searchCriteria) &&  searchCriteria.containsKey("name")) {
            Object filterValue = searchCriteria.remove("name");
            searchCriteria.put(PersistenceService.SEARCH_WILDCARD_OR_IGNORE_CAS.concat(" name"), filterValue);
        }
        // 'valid from' filtering : 
        if (MapUtils.isNotEmpty(searchCriteria) &&  searchCriteria.containsKey("validity.from")) {
            Object filterValue = searchCriteria.remove("validity.from");
            searchCriteria.put("fromRange validity.from", filterValue);
        }
        // 'valid to' filtering : 
        if (MapUtils.isNotEmpty(searchCriteria) &&  searchCriteria.containsKey("validity.to")) {
            Object filterValue = searchCriteria.remove("validity.to");
            searchCriteria.put("toRange validity.to", filterValue);
        }
        
        // 'categories' filtering : 
        if (MapUtils.isNotEmpty(searchCriteria) &&  searchCriteria.containsKey("offerTemplateCategories")) {
            Object filterValue = searchCriteria.remove("offerTemplateCategories");
            searchCriteria.put("listInList offerTemplateCategories", filterValue);
        }
        
	    return super.supplementSearchCriteria(searchCriteria);
	}
	
	public List<OfferTemplateCategory> getActiveOfferTemplateCategories() {
	    if (this.activeOfferTemplateCategories == null) {
	        this.activeOfferTemplateCategories = offerTemplateCategoryService.listActive();
	    }
        return activeOfferTemplateCategories;
	}
	
	public LazyDataModel<OfferTemplate> listNotDisabledFromBOM() {
        filters.put("disabled", false);
        return listFromBOM();
    }

	public List<OfferTemplate> getSelectedOfferTemplates() {
		return selectedOfferTemplates;
	}

	public void setSelectedOfferTemplates(List<OfferTemplate> selectedOfferTemplates) {
		this.selectedOfferTemplates = selectedOfferTemplates;
	}

	public List<OfferTemplateCategory> getSelOfferTemplateCategories() {
		return selOfferTemplateCategories;
	}

	public void setSelOfferTemplateCategories(List<OfferTemplateCategory> selOfferTemplateCategories) {
		this.selOfferTemplateCategories = selOfferTemplateCategories;
	}

	public MeveoInstance getMeveoInstance() {
		return meveoInstance;
	}

	public void setMeveoInstance(MeveoInstance meveoInstance) {
		this.meveoInstance = meveoInstance;
	}

	public long getActiveCount() {
        return activeCount;
    }
	
	public long getInactiveCount() {
        return inactiveCount;
    }
	
	public long getAlmostExpiredCount() {
        return almostExpiredCount;
    }
}
