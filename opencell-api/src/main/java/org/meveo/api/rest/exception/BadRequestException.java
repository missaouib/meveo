package org.meveo.api.rest.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.meveo.api.dto.ActionStatus;

public class BadRequestException extends WebApplicationException {
     public BadRequestException(ActionStatus status) {
         super(Response.status(Response.Status.BAD_REQUEST)
             .entity(status).type(MediaType.APPLICATION_JSON_TYPE).build());
     }
}