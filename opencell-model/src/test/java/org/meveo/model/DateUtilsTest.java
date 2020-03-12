/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */

package org.meveo.model;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.meveo.model.shared.DateUtils;

public class DateUtilsTest {

    @Test()
    public void isPeriodsOverlap() {

        Date startDate = DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, 15, 0, 0, 0);
        Date endDate = DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, 25, 0, 0, 0);

        // Check with both period dates
        Integer[] days = new Integer[] { 10, 15, 0, 10, 16, 1, 16, 20, 1, 20, 26, 1, 25, 27, 0, 10, 27, 1 };

        for (int i = 0; i < days.length; i = i + 3) {
            Integer from = days[i];
            Integer to = days[i + 1];
            boolean shouldMatch = days[i + 2] == 1;
            boolean matched = DateUtils.isPeriodsOverlap(startDate, endDate, DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, from, 0, 0, 0),
                DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, to, 0, 0, 0));

            Assert.assertTrue("Days " + from + "-" + to + " are incorrect", matched == shouldMatch);
        }

        // Check with no period start date
        days = new Integer[] { 10, 15, 1, 10, 16, 1, 16, 20, 1, 20, 26, 1, 25, 27, 0, 10, 27, 1 };

        for (int i = 0; i < days.length; i = i + 3) {
            Integer from = days[i];
            Integer to = days[i + 1];
            boolean shouldMatch = days[i + 2] == 1;
            boolean matched = DateUtils.isPeriodsOverlap(null, endDate, DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, from, 0, 0, 0),
                DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, to, 0, 0, 0));

            Assert.assertTrue("Days " + from + "-" + to + " are incorrect", matched == shouldMatch);
        }

        // Check with no period end date
        days = new Integer[] { 10, 15, 0, 10, 16, 1, 16, 20, 1, 20, 26, 1, 25, 27, 1, 10, 27, 1 };

        for (int i = 0; i < days.length; i = i + 3) {
            Integer from = days[i];
            Integer to = days[i + 1];
            boolean shouldMatch = days[i + 2] == 1;
            boolean matched = DateUtils.isPeriodsOverlap(startDate, null, DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, from, 0, 0, 0),
                DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, to, 0, 0, 0));

            Assert.assertTrue("Days " + from + "-" + to + " are incorrect", matched == shouldMatch);
        }

        // Check with no check start date
        days = new Integer[] { null, 10, 0, null, 15, 0, null, 16, 1, null, 25, 1, null, 27, 1 };

        for (int i = 0; i < days.length; i = i + 3) {
            Integer from = days[i];
            Integer to = days[i + 1];
            boolean shouldMatch = days[i + 2] == 1;
            boolean matched = DateUtils.isPeriodsOverlap(startDate, endDate, null, DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, to, 0, 0, 0));

            Assert.assertTrue("Days " + from + "-" + to + " are incorrect", matched == shouldMatch);
        }

        // Check with no check end date
        days = new Integer[] { 10, null, 1, 15, null, 1, 25, null, 0, 25, null, 0 };

        for (int i = 0; i < days.length; i = i + 3) {
            Integer from = days[i];
            Integer to = days[i + 1];
            boolean shouldMatch = days[i + 2] == 1;
            boolean matched = DateUtils.isPeriodsOverlap(startDate, endDate, DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, from, 0, 0, 0), null);

            Assert.assertTrue("Days " + from + "-" + to + " are incorrect", matched == shouldMatch);
        }

        // Check with no period start date, no check start date
        days = new Integer[] { null, 10, 1, null, 25, 1, null, 27, 1 };

        for (int i = 0; i < days.length; i = i + 3) {
            Integer from = days[i];
            Integer to = days[i + 1];
            boolean shouldMatch = days[i + 2] == 1;
            boolean matched = DateUtils.isPeriodsOverlap(null, endDate, null, DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, to, 0, 0, 0));

            Assert.assertTrue("Days " + from + "-" + to + " are incorrect", matched == shouldMatch);
        }

        // Check with no period start date, no check end date
        days = new Integer[] { 10, null, 1, 25, null, 0, 27, null, 0 };

        for (int i = 0; i < days.length; i = i + 3) {
            Integer from = days[i];
            Integer to = days[i + 1];
            boolean shouldMatch = days[i + 2] == 1;
            boolean matched = DateUtils.isPeriodsOverlap(null, endDate, DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, from, 0, 0, 0), null);

            Assert.assertTrue("Days " + from + "-" + to + " are incorrect", matched == shouldMatch);
        }

        // Check with no period end date, no check start date
        days = new Integer[] { null, 10, 0, null, 15, 0, null, 27, 1 };

        for (int i = 0; i < days.length; i = i + 3) {
            Integer from = days[i];
            Integer to = days[i + 1];
            boolean shouldMatch = days[i + 2] == 1;
            boolean matched = DateUtils.isPeriodsOverlap(startDate, null, null, DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, to, 0, 0, 0));

            Assert.assertTrue("Days " + from + "-" + to + " are incorrect", matched == shouldMatch);
        }

        // Check with no period end date, no check end date
        days = new Integer[] { 10, null, 1, 15, null, 1, 27, null, 1 };

        for (int i = 0; i < days.length; i = i + 3) {
            Integer from = days[i];
            Integer to = days[i + 1];
            boolean shouldMatch = days[i + 2] == 1;
            boolean matched = DateUtils.isPeriodsOverlap(startDate, null, DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, from, 0, 0, 0), null);

            Assert.assertTrue("Days " + from + "-" + to + " are incorrect", matched == shouldMatch);
        }

        // Check with no period start and end dates
        days = new Integer[] { 10, 15, 1, 15, null, 1, null, 15, 1, null, null, 1 };

        for (int i = 0; i < days.length; i = i + 3) {
            Integer from = days[i];
            Integer to = days[i + 1];
            boolean shouldMatch = days[i + 2] == 1;
            boolean matched = DateUtils.isPeriodsOverlap(null, null, from != null ? DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, from, 0, 0, 0) : null,
                to != null ? DateUtils.newDate(2015, java.util.Calendar.FEBRUARY, to, 0, 0, 0) : null);

            Assert.assertTrue("Days " + from + "-" + to + " are incorrect", matched == shouldMatch);
        }

    }
}