package org.meveo.api.rest.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.meveo.api.dto.ActionStatus;

public class InternalServerErrorException extends WebApplicationException {
     public  InternalServerErrorException(ActionStatus status) {
         super(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
             .entity(status).type(MediaType.APPLICATION_JSON_TYPE).build());
     }
}