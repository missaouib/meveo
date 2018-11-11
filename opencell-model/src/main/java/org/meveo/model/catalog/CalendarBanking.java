package org.meveo.model.catalog;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.google.common.collect.Lists;

/**
 * represents a calendar from which weekend and holidays can be excluded.
 *
 * @author hznibar
 * 
 */
@Entity
@DiscriminatorValue("BANKING")
public class CalendarBanking extends Calendar {

    private static final long serialVersionUID = 1L;
    
    /**
     * Specified weekend start.
     */
    @Column(name = "weekend_begin", nullable = false)
    @NotNull
    private int weekendBegin;

    /**
     * Specified weekend end. 
     */
    @Column(name = "weekend_end", nullable = false)
    @NotNull
    private int weekendEnd;

    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OneToMany(mappedBy = "calendar", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("holidayBegin")
    private List<CalendarHoliday> holidays;

    public List<CalendarHoliday> getHolidays() {
        return holidays;
    }

    public void setHolidays(List<CalendarHoliday> holidays) {
        this.holidays = holidays;
    }
    
    public int getWeekendBegin() {
		return weekendBegin;
	}

	public void setWeekendBegin(int weekendBegin) {
		this.weekendBegin = weekendBegin;
	}

	public int getWeekendEnd() {
		return weekendEnd;
	}

	public void setWeekendEnd(int weekendEnd) {
		this.weekendEnd = weekendEnd;
	}

	/**
     * Determines a next calendar date, if it's a holiday, the next date will be the holiday end period + 1 day.
     * 
     * 
     * @param date Date to check
     * @return Next calendar date.
     */
    public Date nextCalendarDate(Date date) {
    	if(date == null) {
    		return null;
    	}    	
    	Date nextCalendarDate = date;
        int iteration = 0;
        
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(nextCalendarDate);
        int lastYear;
        int newYear;
        do {
        	lastYear = calendar.get(java.util.Calendar.YEAR);
        	iteration++;
        	if(isWeekend(nextCalendarDate)) {            		
        		nextCalendarDate = getDateAfterWeekend(nextCalendarDate);
        	}
        	for (CalendarHoliday holidayPeriod : holidays) {
            	if(holidayPeriod.isHolidayDate(nextCalendarDate)) {
            		nextCalendarDate = holidayPeriod.getDateAfterHoliday(nextCalendarDate);
            		if(isWeekend(nextCalendarDate)) {            		
                		nextCalendarDate = getDateAfterWeekend(nextCalendarDate);
                	}
            	}            	
            }
        	calendar.setTime(nextCalendarDate);
        	newYear = calendar.get(java.util.Calendar.YEAR);
        } while((lastYear != newYear) && iteration < 2); // we do two iterations in case of change of year, because the nextCalendarDate can be a holiday or weekend of next year.
        
        if((lastYear != newYear) && iteration == 2) { // all possibles next dates are a holiday
        	throw new IllegalStateException("Next calendar date could not be found!");
        }
        
        return nextCalendarDate;
    }
    
    /**
     * Determines a previous calendar date, if it's a holiday, the previous date will be the holiday begin period -1 day.
     * 
     * 
     * @param date Date to check
     * @return Previous calendar date.
     */
	public Date previousCalendarDate(Date date) {

		if (date == null) {
			return null;
		}
		Date previousCalendarDate = date;
        int iteration = 0;
        
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(previousCalendarDate);
        int lastYear;
        int newYear;
		do {
			lastYear = calendar.get(java.util.Calendar.YEAR);
			iteration++;
			if(isWeekend(previousCalendarDate)) {            		
				previousCalendarDate = getDateBeforeWeekend(previousCalendarDate);
        	}
			for (CalendarHoliday holidayPeriod : holidays) {
				if (holidayPeriod.isHolidayDate(previousCalendarDate)) {
					previousCalendarDate = holidayPeriod.getDateBeforeHoliday(previousCalendarDate);
					if(isWeekend(previousCalendarDate)) {            		
						previousCalendarDate = getDateBeforeWeekend(previousCalendarDate);
		        	}
				}
			}
			calendar.setTime(previousCalendarDate);
        	newYear = calendar.get(java.util.Calendar.YEAR);
		} while ((lastYear != newYear) && iteration < 2); // we do two iterations in case of change of year, because the previousCalendarDate can be a holiday of last year.

		if ((lastYear != newYear) && iteration < 2) { // all possibles previous dates are a holiday
			throw new IllegalStateException("Next calendar date could not be found!");
		}

		return previousCalendarDate;

	}

    private Date getDateBeforeWeekend(Date date) {
		// if the date is not a weekend, we return the same date
		if (!isWeekend(date)) {
			return date;
		}

		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		int weekday = calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1;
        if (weekday == 0) {
            weekday = 7;
        }
        
        int weekdayAdjusted = weekday;
        if (isWeekendCrossBoundry() && weekday < getWeekendBegin()) {
            weekdayAdjusted = weekday + 7;
        }
        calendar.add(java.util.Calendar.DATE, (getWeekendBegin() - weekdayAdjusted)-1);
		return calendar.getTime();
	}

	private Date getDateAfterWeekend(Date date) {
		// if the date is not a weekend, we return the same date
		if (!isWeekend(date)) {
			return date;
		}

		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		int weekday = calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1;
        if (weekday == 0) {
            weekday = 7;
        }
        
        int weekdayAdjusted = weekday;
        if (isWeekendCrossBoundry() && weekday < getWeekendBegin()) {
            weekdayAdjusted = weekday + 7;
        }
        calendar.add(java.util.Calendar.DATE, (getWeekendEndAdjusted() - weekdayAdjusted)+1);
		return calendar.getTime();
	}

	/**
     * check if the date d is a weekend date
     * @param d the date to check
     * @return true if the checked date is a weekend, false otherwise
     */
	private boolean isWeekend(Date d) {
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(d);

		int weekday = calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1;
		if (weekday == 0) {
			weekday = 7;
		}
		// Adjust given date's weekday if interval crosses to another week - and weekday
		// corresponds to another week. E.g checking monday for friday-tuesday period
		int weekdayAdjusted = weekday;
		if (isWeekendCrossBoundry() && weekday < getWeekendBegin()) {
			weekdayAdjusted = weekday + 7;
		}
		return getWeekendBegin() <= weekdayAdjusted && weekdayAdjusted <= getWeekendEndAdjusted();
	}
	
	public static void main(String[] args) {
    	CalendarBanking c = new CalendarBanking();
    	c.setWeekendBegin(6);
    	c.setWeekendEnd(7);
    	CalendarHoliday h = new CalendarHoliday();
    	h.setHolidayBegin(1112);
    	h.setHolidayEnd(1130);
    	CalendarHoliday h2 = new CalendarHoliday();
    	h2.setHolidayBegin(121);
    	h2.setHolidayEnd(1231);
    	c.setHolidays(Lists.newArrayList(h));
    	GregorianCalendar calendar = new GregorianCalendar(2018,10,11);
    	System.out.println(c.nextCalendarDate(calendar.getTime()));
    	System.out.println(c.previousCalendarDate(calendar.getTime()));
    	System.out.println(c.getDateBeforeWeekend(calendar.getTime()));
    	System.out.println(c.getDateAfterWeekend(calendar.getTime()));
		
	}

	/**
	 * When weekend period spans to another week (e.g. Thursday to Monday), the interval end value is adjusted by 7 days.
	 * @return Adjusted end weekend End value
	 */
	private int getWeekendEndAdjusted() {
		if (weekendEnd < weekendBegin) {
			return weekendEnd + 7;
		}
		return weekendEnd;
	}

	private boolean isWeekendCrossBoundry() {
		return weekendEnd < weekendBegin;
	}

	@Override
	public Date previousPeriodEndDate(Date date) {
		return null;
	}

	@Override
	public Date nextPeriodStartDate(Date date) {
		return null;
	}
}