package org.meveo.admin.job;

import java.io.Serializable;
import java.util.Date;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.interceptor.PerformanceInterceptor;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.billing.impl.CounterInstanceService;
import org.meveo.service.job.JobExecutionService;
import org.meveo.service.notification.InboundRequestService;
import org.meveo.service.notification.NotificationHistoryService;
import org.slf4j.Logger;

@Stateless
public class PurgeJobBean extends BaseJobBean implements Serializable {

    private static final long serialVersionUID = 2226065462536318643L;

    @Inject
    private CounterInstanceService counterInstanceService;

    @Inject
    private JobExecutionService jobExecutionService;
    
    @Inject
    private InboundRequestService inboundRequestService;

    @Inject
    private NotificationHistoryService notificationHistoryService;

    @Inject
    private Logger log;

    @Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void execute(JobExecutionResultImpl result, JobInstance jobInstance) {

        try {
            // Purge job execution history
            String jobname = (String) this.getParamOrCFValue(jobInstance, "PurgeJob_jobExecHistory_jobName");
            Long nbDays = (Long) this.getParamOrCFValue(jobInstance, "PurgeJob_jobExecHistory_nbDays");
            if (jobname != null || nbDays != null) {
                Date date = DateUtils.addDaysToDate(new Date(), nbDays.intValue() * (-1));
                long nbItemsToProcess = jobExecutionService.countJobExecutionHistoryToDelete(jobname, date);
                if (nbItemsToProcess > 0) {
                    result.setNbItemsToProcess(nbItemsToProcess); // it might well happen we dont know in advance how many items we have to process,in that case comment this method
                    long nbSuccess = jobExecutionService.deleteJobExecutionHistory(jobname, date);
                    result.setNbItemsCorrectlyProcessed(nbSuccess);
                    result.setNbItemsProcessedWithError(nbItemsToProcess - nbSuccess);
                    if (nbSuccess > 0) {
                        result.setReport("Purged " + nbSuccess + " from " + jobname);
                    }
                }
            }

            // Purge counter periods
            nbDays = (Long) this.getParamOrCFValue(jobInstance, "PurgeJob_counterPeriod_nbDays");
            if (nbDays != null) {
                Date date = DateUtils.addDaysToDate(new Date(), nbDays.intValue() * (-1));
                long nbItemsToProcess = counterInstanceService.countCounterPeriodsToDelete(date);
                if (nbItemsToProcess > 0) {
                    result.addNbItemsToProcess(nbItemsToProcess); // it might well happen we dont know in advance how many items we have to process,in that case comment this method
                    long nbSuccess = counterInstanceService.deleteCounterPeriods(date);
                    result.addNbItemsCorrectlyProcessed(nbSuccess);
                    result.addNbItemsProcessedWithError(nbItemsToProcess - nbSuccess);
                    if (nbSuccess > 0) {
                        result.addReport("Purged " + nbSuccess + " counter periods");
                    }
                }
            }
            
            // Purge notification history
            String notificationCode = (String) this.getParamOrCFValue(jobInstance, "PurgeJob_notificationHistory_notifCode");
            nbDays = (Long) this.getParamOrCFValue(jobInstance, "PurgeJob_notificationHistory_nbDays");
            if (nbDays != null) {
                Date date = DateUtils.addDaysToDate(new Date(), nbDays.intValue() * (-1));
                long nbItemsToProcess = notificationHistoryService.countHistoryToDelete(notificationCode, date);
                if (nbItemsToProcess > 0) {
                    result.addNbItemsToProcess(nbItemsToProcess);
                    long nbSuccess = notificationHistoryService.deleteHistory(notificationCode, date);
                    result.addNbItemsCorrectlyProcessed(nbSuccess);
                    result.addNbItemsProcessedWithError(nbItemsToProcess - nbSuccess);
                    if (nbSuccess > 0) {
                        result.addReport("Purged " + nbSuccess + (notificationCode != null ? " " + notificationCode : "") + " notification history records");
                    }
                }
            }

            // Purge inbound requests
            nbDays = (Long) this.getParamOrCFValue(jobInstance, "PurgeJob_inboundRequests_nbDays");
            if (nbDays != null) {
                Date date = DateUtils.addDaysToDate(new Date(), nbDays.intValue() * (-1));
                long nbItemsToProcess = inboundRequestService.countRequestsToDelete(date);
                if (nbItemsToProcess > 0) {
                    result.addNbItemsToProcess(nbItemsToProcess);
                    long nbSuccess = inboundRequestService.deleteRequests(date);
                    result.addNbItemsCorrectlyProcessed(nbSuccess);
                    result.addNbItemsProcessedWithError(nbItemsToProcess - nbSuccess);
                    if (nbSuccess > 0) {
                        result.addReport("Purged " + nbSuccess + " inbound request records");
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to purge database", e);
            result.registerError(e.getClass().getName() + " " + e.getMessage());
        }
    }
}
