package org.meveo.api.rest.tmforum.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.meveo.api.billing.QuoteApi;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.api.rest.impl.BaseRs;
import org.meveo.api.rest.tmforum.QuoteRs;
import org.tmf.dsmapi.catalog.resource.order.ProductOrder;
import org.tmf.dsmapi.quote.ProductQuote;

@RequestScoped
@Interceptors({ WsRestApiInterceptor.class })
public class QuoteRsImpl extends BaseRs implements QuoteRs {

    @Inject
    private QuoteApi quoteApi;

    @Override
    public Response createProductQuote(ProductQuote productQuote, UriInfo info) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
        Response.ResponseBuilder responseBuilder = null;

        try {
            productQuote = quoteApi.createQuote(productQuote);
            responseBuilder = Response.status(Response.Status.CREATED).entity(productQuote);

        } catch (Exception e) {
            processExceptionAndSetBuilder(result, responseBuilder, e);
        }

        return buildResponse(responseBuilder);
    }

    @Override
    public Response getProductQuote(String quoteId, UriInfo info) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        Response.ResponseBuilder responseBuilder = null;

        try {

            ProductQuote productQuote = quoteApi.getQuote(quoteId);

            responseBuilder = Response.ok().entity(productQuote);

        } catch (Exception e) {
            processExceptionAndSetBuilder(result, responseBuilder, e);
        }


        return buildResponse(responseBuilder);
    }

    @Override
    public Response findProductQuotes(UriInfo info) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        Response.ResponseBuilder responseBuilder = null;

        try {

            Map<String, List<String>> filterCriteria = new HashMap<String, List<String>>();
            List<ProductQuote> quotes = quoteApi.findQuotes(filterCriteria);

            responseBuilder = Response.ok().entity(quotes);

//        } catch (MeveoApiException e) {
//            responseBuilder = Response.status(Response.Status.BAD_REQUEST);
//            responseBuilder.entity(new ActionStatus(ActionStatusEnum.FAIL, e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            processExceptionAndSetBuilder(result, responseBuilder, e);
        }


        return buildResponse(responseBuilder);
    }

    @Override
    public Response updateProductQuote(String quoteId, ProductQuote productQuote, UriInfo info) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        Response.ResponseBuilder responseBuilder = null;

        try {

            productQuote = quoteApi.updatePartiallyQuote(quoteId, productQuote);
            responseBuilder = Response.ok().entity(productQuote);

        } catch (Exception e) {
            processExceptionAndSetBuilder(result, responseBuilder, e);
        }


        return buildResponse(responseBuilder);
    }

    @SuppressWarnings("hiding")
    @Override
    public Response deleteProductQuote(String quoteId, UriInfo info) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        Response.ResponseBuilder responseBuilder = null;

        try {
            quoteApi.deleteQuote(quoteId);

            responseBuilder = Response.ok();

        } catch (Exception e) {
            processExceptionAndSetBuilder(result, responseBuilder, e);
        }


        return buildResponse(responseBuilder);
    }

    @Override
    public Response placeOrder(String quoteId, UriInfo info) {
    	
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
        Response.ResponseBuilder responseBuilder = null;
        try {
        	
            ProductOrder productOrder = quoteApi.placeOrder(quoteId);
            responseBuilder = Response.ok().entity(productOrder);

        } catch (Exception e) {
            processExceptionAndSetBuilder(result, responseBuilder, e);
        }


        return buildResponse(responseBuilder);
    }

    /**
     * @param result action result
     * @param responseBuilder builder response
     * @param e exception happened
     */
    private void processExceptionAndSetBuilder(ActionStatus result, Response.ResponseBuilder responseBuilder, Exception e) {
        processException(e, result);
        if (responseBuilder != null) {
            responseBuilder.entity(result);
        }
    }

    /**
     * @param responseBuilder response builder
     * @return instance of Response.
     */
    private Response buildResponse(Response.ResponseBuilder responseBuilder) {
        Response response = null;
        if (responseBuilder != null) {
            response = responseBuilder.build();
            log.debug("RESPONSE={}", response.getEntity());
        }
        
        
        return response;
    }
}