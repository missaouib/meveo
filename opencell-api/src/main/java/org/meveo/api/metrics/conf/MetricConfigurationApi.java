package org.meveo.api.metrics.conf;

import java.util.Arrays;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.BaseCrudApi;
import org.meveo.api.dto.metric.configuration.MetricConfigurationDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.InvalidParameterException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.cache.MetricsConfigurationCacheContainerProvider;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.metric.configuration.MetricConfiguration;
import org.meveo.service.metric.configuration.MetricConfigurationService;

import static javax.ws.rs.HttpMethod.*;

/**
 *
 */
@Stateless
public class MetricConfigurationApi extends BaseCrudApi<MetricConfiguration, MetricConfigurationDto> {

    @Inject
    MetricConfigurationService metricConfigurationService;

    @Inject
    MetricsConfigurationCacheContainerProvider metricsConfigurationCacheContainerProvider;

    @Override
    public MetricConfiguration create(MetricConfigurationDto dataDto) throws MeveoApiException, BusinessException {
        validate(dataDto);

        if (metricConfigurationService.findByCode(dataDto.getCode()) != null) {
            throw new EntityAlreadyExistsException(MetricConfigurationDto.class, dataDto.getCode());
        }
        metricConfigurationService.create(MetricConfigurationDto.fromDto(dataDto));
        // update cache
        metricsConfigurationCacheContainerProvider.refreshCache(null);
        return metricConfigurationService.findByCode(dataDto.getCode());
    }

    @Override
    public MetricConfigurationDto find(String code) throws MeveoApiException {
        MetricConfiguration metricConfiguration = metricConfigurationService.findByCode(code);
        if (metricConfiguration == null) {
            throw new EntityDoesNotExistsException(MetricConfigurationDto.class, code);
        }
        return MetricConfigurationDto.toDto(metricConfiguration);
    }

    @Override
    public MetricConfiguration update(MetricConfigurationDto dataDto) throws MeveoApiException, BusinessException {
        validate(dataDto);

        MetricConfiguration oldMetricConfiguration = metricConfigurationService.findByCode(dataDto.getCode());
        if (oldMetricConfiguration == null) {
            throw new EntityDoesNotExistsException(MetricConfigurationDto.class, dataDto.getCode());
        }
        oldMetricConfiguration.setFullPath(dataDto.getFullPath());
        oldMetricConfiguration.setMethod(dataDto.getMethod());
        oldMetricConfiguration.setMetricType(dataDto.getMetricType());
        MetricConfiguration updatedMetric = metricConfigurationService.update(oldMetricConfiguration);
        // update cache
        metricsConfigurationCacheContainerProvider.refreshCache(null);

        return updatedMetric;
    }

    @Override
    public void remove(String code) {
        super.remove(code);
    }

    private void validate(MetricConfigurationDto dataDto) {
        if (StringUtils.isBlank(dataDto.getCode())) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(dataDto.getFullPath())) {
            missingParameters.add("fullPath");
        }
        if (StringUtils.isBlank(dataDto.getMethod())) {
            missingParameters.add("method");
        }
        if (StringUtils.isBlank(dataDto.getMetricType())) {
            missingParameters.add("metricType");
        }
        List<String> metrics = Arrays.asList("counter", "timer", "gauge", "histogram", "meter");
        if (!metrics.contains(dataDto.getMetricType())) {
            throw new InvalidParameterException(" Invalid metrics type " + dataDto.getMetricType() + " , not in list " + metrics + " ");
        }
        List<String> methods = Arrays.asList(DELETE, GET, POST, PUT, PATCH, HEAD, OPTIONS);
        if (!methods.contains(dataDto.getMethod())) {
            throw new InvalidParameterException(" Invalid request method " + dataDto.getMethod() + " , not in list " + methods + " ");
        }

        handleMissingParametersAndValidate(dataDto);
    }
}
