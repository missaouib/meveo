package org.meveocrm.services.dwh;

import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.BeanUtils;
import org.meveo.model.dwh.MeasurableQuantity;
import org.meveo.model.dwh.MeasuredValue;
import org.meveo.model.dwh.MeasurementPeriodEnum;
import org.meveo.service.base.PersistenceService;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Wassim Drira
 * @lastModifiedVersion 5.0.1
 */
@Stateless
public class MeasuredValueService extends PersistenceService<MeasuredValue> {

    /**
     * @param date date
     * @param period MeasurementPeriodEnum
     * @param mq MeasurableQuantity
     * @return MeasuredValue
     */
    public MeasuredValue getByDate(Date date, MeasurementPeriodEnum period, MeasurableQuantity mq) {

        MeasuredValue result = null;
        // QueryBuilder queryBuilder = new QueryBuilder(MeasuredValue.class, " m ");
        // queryBuilder.addCriterionDate("m.date", date);
        // queryBuilder.addCriterionEnum("m.measurementPeriod", period);
        // queryBuilder.addCriterionEntity("m.measurableQuantity", mq);
        // Query query = queryBuilder.getQuery(em);
        //
        // log.info("> MeasuredValueService > getByDate > query 1 > {}", query.toString());

        if (date == null || period == null || mq == null) {
            return null;
        }

        Query myQuery = getEntityManager()
            .createQuery("from " + MeasuredValue.class.getName() + " m where m.date=:date and m.measurementPeriod=:period and m.measurableQuantity= :measurableQuantity");
        myQuery.setParameter("date", date).setParameter("period", period).setParameter("measurableQuantity", mq);

        @SuppressWarnings("unchecked")
        List<MeasuredValue> res = myQuery.getResultList();
        if (res.size() > 0) {
            result = res.get(0);
        }
        return result;
    }

    /**
     * 
     * @param dimensionIndex dimension index
     * @param fromDate starting date
     * @param toDate end date
     * @param mq MeasurableQuantity
     * @return list
     */
    @SuppressWarnings("rawtypes")
    public List<String> getDimensionList(int dimensionIndex, Date fromDate, Date toDate, MeasurableQuantity mq) {
        List<String> result = new ArrayList<>();
        Calendar end = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        String dimension = "dimension" + dimensionIndex;
        StringBuilder sqlQuery = new StringBuilder("SELECT DISTINCT(mv.")
                .append(dimension)
                .append(") FROM ")
                .append(MeasuredValue.class.getName())
                .append(" mv WHERE mv.measurableQuantity= :measurableQuantity ");
        if (fromDate != null) {
            start.setTime(fromDate);
            start.set(Calendar.HOUR, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);
            sqlQuery.append(" AND (mv.date >= :start)");
        }
        if (toDate != null) {
            end.setTime(toDate);
            end.set(Calendar.HOUR, 0);
            end.set(Calendar.MINUTE, 0);
            end.set(Calendar.SECOND, 0);
            end.set(Calendar.MILLISECOND, 0);
            sqlQuery.append(" AND (mv.date < :end)");
        }
        sqlQuery.append(" AND mv.measurementPeriod = :measurementPeriod ");
        sqlQuery.append(" ORDER BY :dimension ASC");
        Query query = getEntityManager().createQuery(sqlQuery.toString())
                .setParameter("dimension", "mv." + dimension)
                .setParameter("measurableQuantity", mq.getId())
                .setParameter("measurementPeriod", mq.getMeasurementPeriod());
        if (fromDate != null) {
            query.setParameter("start", start.getTime());
        }
        if (toDate != null) {
            query.setParameter("end", end.getTime());
        }
        List resultList = query.getResultList();
        if (resultList != null) {
            for (Object res : resultList) {
                if (res != null) {
                    result.add(res.toString());
                }
            }
        }
        return result;
    }

    /**
     * List of measured values.
     * 
     * @param code MeasuredValue code
     * @param fromDate starting date
     * @param toDate ending date
     * @param period DAILY, WEEKLY, MONTHLY orYEARLY
     * @param mq MeasurableQuantity
     * @return list of measured values
     */
    public List<MeasuredValue> getByDateAndPeriod(String code, Date fromDate, Date toDate, MeasurementPeriodEnum period, MeasurableQuantity mq) {
        return getByDateAndPeriod(code, fromDate, toDate, period, mq, false);
    }

    /**
     * @param code MeasuredValue code
     * @param fromDate starting date
     * @param toDate ending date
     * @param period DAILY, WEEKLY, MONTHLY orYEARLY
     * @param mq MeasurableQuantity
     * @param sortByDate do we need to sort by date
     * @return list of measured values
     */
    @SuppressWarnings("unchecked")
    public List<MeasuredValue> getByDateAndPeriod(String code, Date fromDate, Date toDate, MeasurementPeriodEnum period, MeasurableQuantity mq, Boolean sortByDate) {
        String sqlQuery = "";

        boolean whereExists = false;
        if (code != null) {
            sqlQuery += "m.code = :code ";
            whereExists = true;
        }

        if (fromDate != null) {
            if (!whereExists) {
                sqlQuery += "m.date >= :fromDate ";
                whereExists = true;
            } else {
                sqlQuery += "and m.date >= :fromDate ";
            }
        }
        if (toDate != null) {
            if (!whereExists) {
                sqlQuery += "m.date < :toDate ";
                whereExists = true;
            } else {
                sqlQuery += "and m.date < :toDate ";
            }
        }

        if (period != null) {
            if (!whereExists) {
                sqlQuery += "m.measurementPeriod = :period ";
                whereExists = true;
            } else {
                sqlQuery += "and m.measurementPeriod = :period ";
            }
        }
        if (mq != null) {
            if (!whereExists) {
                sqlQuery += "m.measurableQuantity.id = :id ";
                whereExists = true;
            } else {
                sqlQuery += "and m.measurableQuantity.id = :id ";
            }
        }

        if (sortByDate) {
            sqlQuery += "ORDER BY m.date ASC";
        }

        Query myQuery;
        if (whereExists) {
            sqlQuery = "FROM " + MeasuredValue.class.getName() + " m WHERE " + sqlQuery;
            myQuery = getEntityManager().createQuery(sqlQuery);
            if (code != null) {
                myQuery.setParameter("code", code.toUpperCase());
            }
            if (fromDate != null) {
                myQuery.setParameter("fromDate", fromDate);
            }
            if (toDate != null) {
                myQuery.setParameter("toDate", toDate);

            }
            if (period != null) {
                myQuery.setParameter("period", period);
            }
            if (mq != null) {
                myQuery.setParameter("id", mq.getId());
            }
            if (sortByDate) {
                sqlQuery += "ORDER BY m.date ASC";
            }
        } else {
            sqlQuery = "FROM " + MeasuredValue.class.getName() + " m " + sqlQuery;
            myQuery = getEntityManager().createQuery(sqlQuery);
        }

        return myQuery.getResultList();
    }

    /**
     * Save updated values
     *
     * @param entity that contains the updated values
     * @return true to indicate that an update was done
     * @throws BusinessException if the code was changed.
     */
    public boolean updateCellEdit(MeasuredValue entity) throws BusinessException {
        boolean result = false;
        MeasuredValue measuredValue = findById(entity.getId());
        if (measuredValue != null) {
            if (!BeanUtils.equals(entity.getCode(), measuredValue.getCode())) {
                // if code has changed, throw an error
                throw new BusinessException("Code " + entity.getCode() + " can not be replaced!");
            }
            if (!BeanUtils.equals(entity.getValue(), measuredValue.getValue())) {
                measuredValue.setValue(entity.getValue());
                result = true;
            }
            if (!BeanUtils.equals(entity.getDate(), measuredValue.getDate())) {
                measuredValue.setDate(entity.getDate());
                result = true;
            }
            if (!(BeanUtils.equals(entity.getDimension1(), measuredValue.getDimension1()))) {
                measuredValue.setDimension1(entity.getDimension1());
                result = true;
            }
            if (!BeanUtils.equals(entity.getDimension2(), measuredValue.getDimension2())) {
                measuredValue.setDimension2(entity.getDimension2());
                result = true;
            }
            if (!BeanUtils.equals(entity.getDimension3(), measuredValue.getDimension3())) {
                measuredValue.setDimension3(entity.getDimension3());
                result = true;
            }
            if (!BeanUtils.equals(entity.getDimension4(), measuredValue.getDimension4())) {
                measuredValue.setDimension4(entity.getDimension4());
                result = true;
            }
            if (result) {
                update(measuredValue);
            }
        }
        return result;
    }
}
