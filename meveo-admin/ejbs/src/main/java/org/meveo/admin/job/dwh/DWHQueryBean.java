package org.meveo.admin.job.dwh;

import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.crm.Provider;
import org.meveocrm.model.dwh.MeasurableQuantity;
import org.meveocrm.model.dwh.MeasuredValue;
import org.meveocrm.model.dwh.MeasurementPeriodEnum;
import org.meveocrm.services.dwh.MeasurableQuantityService;
import org.meveocrm.services.dwh.MeasuredValueService;

@Stateless
public class DWHQueryBean {

	@Inject
	private MeasurableQuantityService mqService;

	@Inject
	private MeasuredValueService mvService;
	
	@PersistenceContext
	EntityManager em;

	
	public int executeQuery(String measurableQuantityCode,Provider provider) throws BusinessException {
		//first we check that there is a measurable quantity for the given provider
		int result=0;
		MeasurableQuantity mq = mqService.findByCode(em,measurableQuantityCode,provider);
		if(mq==null){
			throw new BusinessException("no measurable quantity with code "+measurableQuantityCode+" for provider "+provider.getCode());
		}
		if(StringUtils.isBlank(mq.getJpaQuery())){
			throw new BusinessException("measurable quantity with code "+measurableQuantityCode+" has no JPA query set.");
		}
		try{
			Query query=em.createQuery(mq.getJpaQuery());
			@SuppressWarnings("unchecked")
			List<Object[]> results=query.getResultList();
			for(Object[] res:results){
				MeasuredValue mv = mvService.getByDate(em, (Date) res[0], MeasurementPeriodEnum.DAILY, mq);
				if(mv==null){
						mv=new MeasuredValue();
				}
				mv.setProvider(provider);
				mv.setMeasurableQuantity(mq);
				mv.setDate((Date) res[0]);
				mv.setMeasurementPeriod(MeasurementPeriodEnum.DAILY);
				mv.setValue((Long)res[1]);
				if(mv.getId()!=null){
					mvService.update(em, mv);
				} else {
					mvService.create(em, mv, null, provider);
				}
				result++;
			}
		} catch(Exception e){
			throw new BusinessException("measurable quantity with code "+measurableQuantityCode+" contain invalid JPA query: "+e.getMessage());			
		}
		
		return result;
	}
}
