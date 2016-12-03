package org.meveo.model.cache;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.meveo.model.billing.CounterPeriod;

public class CachedCounterPeriod {

    private Long counterPeriodId;
    private Date startDate;
    private Date endDate;
    private BigDecimal value;
    private BigDecimal level;
    private boolean dbDirty;
    private CachedCounterInstance counterInstance;
    private List<BigDecimal> notificationLevels;

    public Long getCounterPeriodId() {
        return counterPeriodId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getLevel() {
        return level;
    }

    public boolean isDbDirty() {
        return dbDirty;
    }

    public CachedCounterInstance getCounterInstance() {
        return counterInstance;
    }

    public CachedCounterPeriod() {

    }

    public CachedCounterPeriod(CounterPeriod counterPeriod, CachedCounterInstance counterInstance) {
        this.counterPeriodId = counterPeriod.getId();
        this.endDate = counterPeriod.getPeriodEndDate();
        this.level = counterPeriod.getLevel();
        this.startDate = counterPeriod.getPeriodStartDate();
        this.value = counterPeriod.getValue();
        this.counterInstance = counterInstance;
        this.notificationLevels = counterPeriod.getNotificationLevelsAsList();
    }

    /**
     * Get a list of counter values for which notification should fire given the counter value change from (exclusive)/to (inclusive) value
     * 
     * @param fromValue Counter changed from value
     * @param toValue Counter changed to value
     * @return A list of counter values that match notification levels
     */
    public List<BigDecimal> getMatchedNotificationLevels(BigDecimal fromValue, BigDecimal toValue) {
        if (notificationLevels == null) {
            return null;
        }

        List<BigDecimal> matchedLevels = new ArrayList<>();
        for (BigDecimal notifValue : notificationLevels) {
            if (fromValue.compareTo(notifValue) > 0 && notifValue.compareTo(toValue) >= 0) {
                matchedLevels.add(notifValue);
            }
        }
        return matchedLevels;
    }

    @Override
    public String toString() {
        return String.format("CachedCounterPeriod [counterPeriodId=%s, startDate=%s, endDate=%s, value=%s, level=%s, dbDirty=%s, notificationLevels=%s]", counterPeriodId,
            startDate, endDate, value, level, dbDirty, notificationLevels);
    }
}