package org.meveo.admin.job;

import java.io.Serializable;
import java.util.Date;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.cache.RatingCacheContainerProvider;
import org.meveo.interceptor.PerformanceInterceptor;
import org.meveo.model.admin.User;
import org.meveo.model.crm.Provider;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.billing.impl.CounterInstanceService;
import org.meveo.service.job.JobExecutionService;
import org.slf4j.Logger;

@Stateless
public class PurgeJobBean implements Serializable {

    private static final long serialVersionUID = 2226065462536318643L;

    @Inject
    private RatingCacheContainerProvider ratingCacheContainerProvider;

    @Inject
    private CounterInstanceService counterInstanceService;

    @Inject
    private JobExecutionService jobExecutionService;

    @Inject
    protected Logger log;

    @Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void execute(JobExecutionResultImpl result, JobInstance jobInstance, User currentUser) {
        Provider currentProvider = currentUser.getProvider();
        log.debug("Running for user={}", currentUser);

        try {
            // Purge job execution history
            String jobname = (String) jobInstance.getCFValue("PurgeJob_jobExecHistory_jobName");
            Long nbDays = (Long) jobInstance.getCFValue("PurgeJob_jobExecHistory_nbDays");
            if (jobname != null || nbDays != null) {
                Date date = DateUtils.addDaysToDate(new Date(), nbDays.intValue() * (-1));
                long nbItemsToProcess = jobExecutionService.countJobExecutionHistoryToDelete(jobname, date, currentProvider);
                if (nbItemsToProcess > 0) {
                    result.setNbItemsToProcess(nbItemsToProcess); // it might well happen we dont know in advance how many items we have to process,in that case comment this method
                    int nbSuccess = jobExecutionService.deleteJobExecutionHistory(jobname, date, currentUser.getProvider());
                    result.setNbItemsCorrectlyProcessed(nbSuccess);
                    result.setNbItemsProcessedWithError(nbItemsToProcess - nbSuccess);
                    if (nbSuccess > 0) {
                        result.setReport("Purged " + nbSuccess + " from " + jobname);
                    }
                }
            }

            // Purge counter periods
            nbDays = (Long) jobInstance.getCFValue("PurgeJob_counterPeriod_nbDays");
            if (nbDays != null) {
                Date date = DateUtils.addDaysToDate(new Date(), nbDays.intValue() * (-1));
                long nbItemsToProcess = counterInstanceService.countCounterPeriodsToDelete(date, currentProvider);
                if (nbItemsToProcess > 0) {
                    result.addNbItemsToProcess(nbItemsToProcess); // it might well happen we dont know in advance how many items we have to process,in that case comment this method
                    long nbSuccess = counterInstanceService.deleteCounterPeriods(date, currentUser.getProvider());
                    result.addNbItemsCorrectlyProcessed(nbSuccess);
                    result.addNbItemsProcessedWithError(nbItemsToProcess - nbSuccess);
                    if (nbSuccess > 0) {
                        result.addReport("Purged " + nbSuccess + " counter periods");
                        ratingCacheContainerProvider.refreshCache(RatingCacheContainerProvider.COUNTER_CACHE);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to purge database", e);
            result.registerError(e.getClass().getName() + " " + e.getMessage());
        }
    }
}
