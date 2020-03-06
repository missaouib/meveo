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

package org.meveo.audit.logging.core;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.audit.logging.annotations.IgnoreAudit;
import org.meveo.audit.logging.annotations.MeveoAudit;
import org.meveo.audit.logging.dto.AnnotationAuditEvent;
import org.meveo.audit.logging.dto.AuditEvent;
import org.meveo.audit.logging.dto.MethodParameter;

/**
 * @author Edward P. Legaspi
 **/
@Stateless
public class AuditManagerService {

	@Inject
	private MetadataHandler metadataHandler;

	@Inject
	private AuditEventProcessor auditEventProcessor;

	private final static String ACTION = "action";

	private Class getEntityClass(Class clazz) {
		while (!(clazz.getGenericSuperclass() instanceof ParameterizedType)) {
			clazz = clazz.getSuperclass();
		}
		Object o = ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[0];

		if (o instanceof TypeVariable) {
			return (Class) ((TypeVariable) o).getBounds()[0];
		} else {
			return (Class) o;
		}
	}

	public void audit(Class<? extends Object> clazz, String method, Object[] paramValues) throws BusinessException {
		AuditEvent event = new AuditEvent();
		event.setEntity(clazz.getName());
		event.setAction(method);
		event = metadataHandler.addSignature(event);
		auditEventProcessor.process(event);
	}

	public void audit(Class<? extends Object> clazz, Method method, Object[] paramValues) throws BusinessException {
		audit(new AnnotationAuditEvent(clazz, method, paramValues));
	}

	public void audit(AnnotationAuditEvent event) throws BusinessException {
		AuditEvent auditEvent = transformToEvent(event);
		auditEvent = metadataHandler.addSignature(auditEvent);
		auditEventProcessor.process(auditEvent);
	}

	public AuditEvent transformToEvent(AnnotationAuditEvent annotationEvent) {
		AuditEvent event = new AuditEvent();
		event.setEntity(getEntityClass(annotationEvent.getClazz()).getName());
		event.setAction(annotationEvent.getMethod().getName());
		event.setFields(getParameterLines(annotationEvent.getMethod(), annotationEvent.getParamValues()));
		return event;
	}

	public AuditEvent transformToEventExplicitAnnotated(AnnotationAuditEvent annotationEvent) {
		AuditEvent event = new AuditEvent();
		event.setEntity(getEntityClass(annotationEvent.getClazz()).getName());

		if (annotationEvent.getClazz().isAnnotationPresent(MeveoAudit.class)
				&& !annotationEvent.getMethod().isAnnotationPresent(IgnoreAudit.class)) {
			MeveoAudit audit = annotationEvent.getClazz().getAnnotation(MeveoAudit.class);
			event.setFields(getParameterLines(annotationEvent.getMethod(), annotationEvent.getParamValues()));

			String annotationAction = audit.action();
			if (ACTION.equals(annotationAction)) {
				event.setAction(annotationEvent.getMethod().getName());

			} else {
				event.setAction(annotationAction);
			}

		} else if (!annotationEvent.getClazz().isAnnotationPresent(MeveoAudit.class)
				&& annotationEvent.getMethod().isAnnotationPresent(MeveoAudit.class)) {
			MeveoAudit audit = annotationEvent.getMethod().getAnnotation(MeveoAudit.class);
			event.setFields(getParameterLines(annotationEvent.getMethod(), annotationEvent.getParamValues()));

			String annotationAction = audit.action();
			if (ACTION.equals(annotationAction)) {
				event.setAction(annotationEvent.getMethod().getName());

			} else {
				event.setAction(annotationAction);
			}
		}

		return event;
	}

	public static List<MethodParameter> getParameterLines(Method method, Object[] objects) {
		Parameter[] parameters = method.getParameters();
		List<MethodParameter> methodParameters = new ArrayList<>();

		int i = 0;
		for (Parameter parameter : parameters) {
			// if (!parameter.isNamePresent()) {
			// // throw new IllegalArgumentException("Parameter names are not
			// // present!");
			// continue;
			// }
			final Object obj = objects[i++];

			MethodParameter mp = new MethodParameter(parameter.getName(), obj, parameter.getType().getName());
			methodParameters.add(mp);
		}

		return methodParameters;
	}

}
