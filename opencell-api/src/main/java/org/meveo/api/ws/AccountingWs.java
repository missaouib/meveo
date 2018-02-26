package org.meveo.api.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.billing.AccountingCodeDto;
import org.meveo.api.dto.response.PagingAndFiltering;
import org.meveo.api.dto.response.billing.AccountingCodeGetResponseDto;
import org.meveo.api.dto.response.billing.AccountingCodeListResponse;

/**
 * @author Edward P. Legaspi
 * @version 23 Feb 2018
 **/
@WebService
public interface AccountingWs extends IBaseWs {

    /**
     * Creates a new AccountingCode.
     * 
     * @param postData
     * @return
     */
    @WebMethod
    ActionStatus createAccountingCode(@WebParam(name = "accountingCode") AccountingCodeDto postData);

    /**
     * Updates an AccountingCode.
     * 
     * @param postData
     * @return
     */
    @WebMethod
    ActionStatus updateAccountingCode(@WebParam(name = "accountingCode") AccountingCodeDto postData);

    /**
     * Creates or updates an AccountingCode.
     * 
     * @param postData
     * @return
     */
    @WebMethod
    ActionStatus createOrUpdateAccountingCode(@WebParam(name = "accountingCode") AccountingCodeDto postData);

    /**
     * Finds an AccountingCode.
     * 
     * @param accountingCode
     * @return
     */
    @WebMethod
    AccountingCodeGetResponseDto findAccountingCode(@WebParam(name = "accountingCode") String accountingCode);

    /**
     * Returns a list of AccountingCode.
     * 
     * @param accountingCode - Paging and Filtering criteria
     * @return
     */
    @WebMethod
    AccountingCodeListResponse listAccountingCode(@WebParam(name = "pagingAndFiltering") PagingAndFiltering pagingAndFiltering);

    /**
     * Removes an AccountingCode.
     * 
     * @param accountingCode
     * @return
     */
    @WebMethod
    ActionStatus removeAccountingCode(@WebParam(name = "accountingCode") String accountingCode);

}
