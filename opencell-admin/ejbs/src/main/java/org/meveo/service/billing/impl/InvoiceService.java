/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.service.billing.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.meveo.commons.utils.NumberUtils.round;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.poi.util.IOUtils;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.ImportInvoiceException;
import org.meveo.admin.exception.InvoiceExistException;
import org.meveo.admin.exception.InvoiceJasperNotFoundException;
import org.meveo.admin.job.PDFParametersConstruction;
import org.meveo.admin.util.PdfWaterMark;
import org.meveo.api.dto.CategoryInvoiceAgregateDto;
import org.meveo.api.dto.RatedTransactionDto;
import org.meveo.api.dto.SubCategoryInvoiceAgregateDto;
import org.meveo.api.dto.invoice.GenerateInvoiceRequestDto;
import org.meveo.api.dto.invoice.InvoiceDto;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.InvalidParameterException;
import org.meveo.commons.exceptions.ConfigurationException;
import org.meveo.commons.utils.NumberUtils;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.commons.utils.StringUtils;
import org.meveo.jpa.JpaAmpNewTx;
import org.meveo.model.Auditable;
import org.meveo.model.BaseEntity;
import org.meveo.model.IBillableEntity;
import org.meveo.model.ICustomFieldEntity;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.*;
import org.meveo.model.catalog.DiscountPlanItem;
import org.meveo.model.catalog.DiscountPlanItemTypeEnum;
import org.meveo.model.catalog.RoundingModeEnum;
import org.meveo.model.communication.email.EmailTemplate;
import org.meveo.model.communication.email.MailingTypeEnum;
import org.meveo.model.crm.Customer;
import org.meveo.model.crm.custom.CustomFieldValues;
import org.meveo.model.filter.Filter;
import org.meveo.model.order.Order;
import org.meveo.model.payments.AccountOperation;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.MatchingStatusEnum;
import org.meveo.model.payments.PaymentMethod;
import org.meveo.model.payments.PaymentMethodEnum;
import org.meveo.model.payments.WriteOff;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.ValueExpressionWrapper;
import org.meveo.service.catalog.impl.InvoiceCategoryService;
import org.meveo.service.catalog.impl.InvoiceSubCategoryService;
import org.meveo.service.catalog.impl.TaxService;
import org.meveo.service.communication.impl.EmailSender;
import org.meveo.service.crm.impl.CustomFieldInstanceService;
import org.meveo.service.order.OrderService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.meveo.service.payments.impl.RecordedInvoiceService;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;
import org.meveo.service.script.billing.TaxScriptService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import net.sf.jasperreports.engine.util.JRLoader;

/**
 * The Class InvoiceService.
 *
 * @author Edward P. Legaspi
 * @author akadid abdelmounaim
 * @author Wassim Drira
 * @author Said Ramli
 * @author Khalid HORRI
 * @author Abdellatif BARI
 * @lastModifiedVersion 7.0
 */
@Stateless
public class InvoiceService extends PersistenceService<Invoice> {

    /**
     * The Constant INVOICE_ADJUSTMENT_SEQUENCE.
     */
    public final static String INVOICE_ADJUSTMENT_SEQUENCE = "INVOICE_ADJUSTMENT_SEQUENCE";

    /**
     * The Constant INVOICE_SEQUENCE.
     */
    public final static String INVOICE_SEQUENCE = "INVOICE_SEQUENCE";

    private final static BigDecimal HUNDRED = new BigDecimal("100");

    /**
     * The p DF parameters construction.
     */
    @EJB
    private PDFParametersConstruction pDFParametersConstruction;

    /**
     * The xml invoice creator.
     */
    @EJB
    private XMLInvoiceCreator xmlInvoiceCreator;

    /**
     * The customer account service.
     */
    @Inject
    private CustomerAccountService customerAccountService;

    /**
     * The invoice aggregate service.
     */
    @Inject
    private InvoiceAgregateService invoiceAgregateService;

    /**
     * The billing account service.
     */
    @Inject
    private BillingAccountService billingAccountService;

    /**
     * The rated transaction service.
     */
    @Inject
    private RatedTransactionService ratedTransactionService;

    /**
     * The rejected billing account service.
     */
    @Inject
    private RejectedBillingAccountService rejectedBillingAccountService;

    /**
     * The invoice type service.
     */
    @Inject
    private InvoiceTypeService invoiceTypeService;

    /**
     * The order service.
     */
    @Inject
    private OrderService orderService;

    /**
     * The recorded invoice service.
     */
    @Inject
    private RecordedInvoiceService recordedInvoiceService;

    /**
     * The service singleton.
     */
    @Inject
    private ServiceSingleton serviceSingleton;

    @Inject
    private ScriptInstanceService scriptInstanceService;

    @Inject
    protected CustomFieldInstanceService customFieldInstanceService;

    @Inject
    private EmailSender emailSender;

    @EJB
    private InvoiceService invoiceService;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private BillingRunService billingRunService;

    @Inject
    private InvoiceCategoryService invoiceCategoryService;

    @Inject
    private InvoiceSubCategoryService invoiceSubcategoryService;

    @Inject
    private InvoiceSubCategoryCountryService invoiceSubCategoryCountryService;

    @Inject
    private TaxScriptService taxScriptService;

    @Inject
    private TaxService taxService;

    @Inject
    private UserAccountService userAccountService;

    /**
     * folder for pdf .
     */
    private String PDF_DIR_NAME = "pdf";

    /**
     * folder for adjustment pdf.
     */
    private String ADJUSTEMENT_DIR_NAME = "invoiceAdjustmentPdf";

    /**
     * template jasper name.
     */
    private String INVOICE_TEMPLATE_FILENAME = "invoice.jasper";

    /**
     * date format.
     */
    private String DATE_PATERN = "yyyy.MM.dd";

    /**
     * map used to store temporary jasper report.
     */
    private Map<String, JasperReport> jasperReportMap = new HashMap<>();

    /**
     * Description translation map.
     */
    private Map<String, String> descriptionMap = new HashMap<>();

    private static int rtPaginationSize = 30000;

    @PostConstruct
    private void init() {
        ParamBean paramBean = ParamBeanFactory.getAppScopeInstance();

        rtPaginationSize = paramBean.getPropertyAsInteger("invoicing.rtPaginationSize", 30000);
    }

    /**
     * Gets the invoice.
     *
     * @param invoiceNumber   invoice's number
     * @param customerAccount customer account
     * @return invoice
     * @throws BusinessException business exception
     */
    public Invoice getInvoice(String invoiceNumber, CustomerAccount customerAccount) throws BusinessException {
        try {
            Query q = getEntityManager().createQuery("from Invoice where invoiceNumber = :invoiceNumber and billingAccount.customerAccount=:customerAccount");
            q.setParameter("invoiceNumber", invoiceNumber).setParameter("customerAccount", customerAccount);
            Object invoiceObject = q.getSingleResult();
            return (Invoice) invoiceObject;
        } catch (NoResultException e) {
            log.info("Invoice with invoice number {} was not found. Returning null.", invoiceNumber);
            return null;
        } catch (NonUniqueResultException e) {
            log.info("Multiple invoices with invoice number {} was found. Returning null.", invoiceNumber);
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the invoice by number.
     *
     * @param invoiceNumber invoice's number
     * @return found invoice.
     * @throws BusinessException business exception
     */
    public Invoice getInvoiceByNumber(String invoiceNumber) throws BusinessException {
        return getInvoiceByNumber(invoiceNumber, invoiceTypeService.getDefaultCommertial());
    }

    /**
     * Find by invoice number and type.
     *
     * @param invoiceNumber invoice's number
     * @param invoiceType   invoice's type
     * @return found invoice
     * @throws BusinessException business exception
     */
    public Invoice findByInvoiceNumberAndType(String invoiceNumber, InvoiceType invoiceType) throws BusinessException {
        QueryBuilder qb = new QueryBuilder(Invoice.class, "i", null);
        qb.addCriterion("i.invoiceNumber", "=", invoiceNumber, true);
        qb.addCriterionEntity("i.invoiceType", invoiceType);
        try {
            return (Invoice) qb.getQuery(getEntityManager()).getSingleResult();
        } catch (NoResultException e) {
            log.info("Invoice with invoice number {} was not found. Returning null.", invoiceNumber);
            return null;
        } catch (NonUniqueResultException e) {
            log.info("Multiple invoices with invoice number {} was found. Returning null.", invoiceNumber);
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the invoice by number.
     *
     * @param invoiceNumber invoice's number
     * @param invoiceType   invoice's type
     * @return found invoice
     * @throws BusinessException business exception
     */
    public Invoice getInvoiceByNumber(String invoiceNumber, InvoiceType invoiceType) throws BusinessException {
        return findByInvoiceNumberAndType(invoiceNumber, invoiceType);
    }

    /**
     * Gets the invoices.
     *
     * @param billingRun instance of billing run
     * @return list of invoices related to given billing run
     * @throws BusinessException business exception
     */
    @SuppressWarnings("unchecked")
    public List<Invoice> getInvoices(BillingRun billingRun) throws BusinessException {
        try {
            Query q = getEntityManager().createQuery("from Invoice where billingRun = :billingRun");
            q.setParameter("billingRun", billingRun);
            List<Invoice> invoices = q.getResultList();
            return invoices;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the invoices.
     *
     * @param billingAccount billing account
     * @param invoiceType    invoice's type
     * @return list of invoice
     * @throws BusinessException business exception
     */
    @SuppressWarnings("unchecked")
    public List<Invoice> getInvoices(BillingAccount billingAccount, InvoiceType invoiceType) throws BusinessException {
        try {
            Query q = getEntityManager().createQuery("from Invoice where billingAccount = :billingAccount and invoiceType=:invoiceType");
            q.setParameter("billingAccount", billingAccount);
            q.setParameter("invoiceType", invoiceType);
            List<Invoice> invoices = q.getResultList();
            log.info("getInvoices: founds {} invoices with BA_code={} and type={} ", invoices.size(), billingAccount.getCode(), invoiceType);
            return invoices;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Assign invoice number from reserve.
     *
     * @param invoice              invoice
     * @param invoicesToNumberInfo instance of InvoicesToNumberInfo
     * @throws BusinessException business exception
     */
    @SuppressWarnings("deprecation")
    private void assignInvoiceNumberFromReserve(Invoice invoice, InvoicesToNumberInfo invoicesToNumberInfo) throws BusinessException {
        InvoiceType invoiceType = invoice.getInvoiceType();
        String prefix = invoiceType.getPrefixEL();

        // TODO: 3508
        Seller seller = null;
        if (invoice.getBillingAccount() != null && invoice.getBillingAccount().getCustomerAccount() != null
                && invoice.getBillingAccount().getCustomerAccount().getCustomer() != null && invoice.getBillingAccount().getCustomerAccount().getCustomer().getSeller() != null) {
            seller = invoice.getBillingAccount().getCustomerAccount().getCustomer().getSeller();
        }

        InvoiceTypeSellerSequence invoiceTypeSellerSequence = invoiceType.getSellerSequenceByType(seller);
        if (invoiceTypeSellerSequence != null) {
            prefix = invoiceTypeSellerSequence.getPrefixEL();
        }

        if (prefix != null && !StringUtils.isBlank(prefix)) {
            prefix = evaluatePrefixElExpression(prefix, invoice);
        } else {
            prefix = "";
        }

        String invoiceNumber = invoicesToNumberInfo.nextInvoiceNumber();
        // request to store invoiceNo in alias field
        invoice.setAlias(invoiceNumber);
        invoice.setInvoiceNumber(prefix + invoiceNumber);
    }

    /**
     * Get a list of invoices that are validated, but PDF was not yet generated.
     *
     * @param billingRunId An optional billing run identifier for filtering
     * @return A list of invoice ids
     */
    public List<Long> getInvoicesIdsValidatedWithNoPdf(Long billingRunId) {

        if (billingRunId == null) {
            return getEntityManager().createNamedQuery("Invoice.validatedNoPdf", Long.class).getResultList();

        } else {
            return getEntityManager().createNamedQuery("Invoice.validatedNoPdfByBR", Long.class).setParameter("billingRunId", billingRunId).getResultList();
        }
    }

    /**
     * Get list of Draft invoice Ids that belong to the given Billing Run and not having PDF generated yet.
     *
     * @param billingRunId An optional billing run identifier for filtering
     * @return A list of invoice ids
     */
    public List<Long> getDraftInvoiceIdsByBRWithNoPdf(Long billingRunId) {
        if (billingRunId == null) {
            return getEntityManager().createNamedQuery("Invoice.draftNoPdf", Long.class).getResultList();

        } else {
            return getEntityManager().createNamedQuery("Invoice.draftNoPdfByBR", Long.class).setParameter("billingRunId", billingRunId).getResultList();
        }
    }

    /**
     * Get list of Draft and validated invoice Ids that belong to the given Billing Run and not having PDF generated yet.
     *
     * @param billingRunId An optional billing run identifier for filtering
     * @return A list of invoice ids
     */
    public List<Long> getInvoiceIdsIncludeDraftByBRWithNoPdf(Long billingRunId) {
        if (billingRunId == null) {
            return getEntityManager().createNamedQuery("Invoice.allNoPdf", Long.class).getResultList();

        } else {
            return getEntityManager().createNamedQuery("Invoice.allNoPdfByBR", Long.class).setParameter("billingRunId", billingRunId).getResultList();
        }
    }

    /**
     * Gets the invoice ids with no account operation.
     *
     * @param br billing run
     * @return list of invoice's which doesn't have the account operation.
     */
    public List<Long> getInvoiceIdsWithNoAccountOperation(BillingRun br) {
        try {
            QueryBuilder qb = queryInvoiceIdsWithNoAccountOperation(br);
            return qb.getIdQuery(getEntityManager()).getResultList();
        } catch (Exception ex) {
            log.error("failed to get invoices with no account operation", ex);
        }
        return null;
    }

    /**
     * Query invoice ids with no account operation.
     *
     * @param br the br
     * @return the query builder
     */
    private QueryBuilder queryInvoiceIdsWithNoAccountOperation(BillingRun br) {
        QueryBuilder qb = new QueryBuilder(Invoice.class, " i");
        qb.addSql("i.invoiceNumber is not null");
        qb.addSql("i.recordedInvoice is null");
        if (br != null) {
            qb.addCriterionEntity("i.billingRun", br);
        }
        return qb;
    }

    /**
     * @param br                           billing run
     * @param excludeInvoicesWithoutAmount exclude invoices without amount.
     * @return list of invoice's which doesn't have the account operation, and have an amount
     */
    public List<Long> queryInvoiceIdsWithNoAccountOperation(BillingRun br, boolean excludeInvoicesWithoutAmount, Boolean invoiceAccountable) {
        try {
            QueryBuilder qb = queryInvoiceIdsWithNoAccountOperation(br);
            if (excludeInvoicesWithoutAmount) {
                qb.addSql("i.amount != 0 ");
            }
            if (invoiceAccountable != null) {
                qb.addSql("i.invoiceType.invoiceAccountable = ".concat(invoiceAccountable.toString()));
            }
            return qb.getIdQuery(getEntityManager()).getResultList();
        } catch (Exception ex) {
            log.error("failed to get invoices with amount and with no account operation", ex);
        }
        return null;
    }

    /**
     * Get rated transactions for entity grouped by billing account, seller and invoice type
     *
     * @param entityToInvoice        entity to be billed
     * @param billingAccount         Payment method. Provided in case of Billing account or Subscription billable entity type. Order can span multiple billing accounts and therefore will
     *                               be determined for each rated transaction.
     * @param billingRun             billing run
     * @param defaultBillingCycle    Billing cycle applicable to billable entity or to billing run
     * @param defaultInvoiceType     Invoice type. A default invoice type for postpaid rated transactions. In case of prepaid RTs, a prepaid invoice type is used. Provided in case of
     *                               Billing account or Subscription billable entity type. Order can span multiple billing accounts and therefore will be determined for each billing account occurrence.
     * @param ratedTransactionFilter rated transaction filter
     * @param firstTransactionDate   date of first transaction
     * @param lastTransactionDate    date of last transaction
     * @param isDraft                Is it a draft invoice
     * @return List of rated transaction groups for entity and a flag indicating if there are more Rated transactions to retrieve
     */
    private RatedTransactionsToInvoice getRatedTransactionGroups(IBillableEntity entityToInvoice, BillingAccount billingAccount, BillingRun billingRun,
            BillingCycle defaultBillingCycle, InvoiceType defaultInvoiceType, Filter ratedTransactionFilter, Date firstTransactionDate, Date lastTransactionDate, boolean isDraft)
            throws BusinessException {

        List<RatedTransaction> ratedTransactions = ratedTransactionService
                .listRTsToInvoice(entityToInvoice, firstTransactionDate, lastTransactionDate, ratedTransactionFilter, rtPaginationSize);

        // If retrieved RT and pagination size does not match, it means no more RTs are pending to be processed and invoice can be closed
        boolean moreRts = ratedTransactions.size() == rtPaginationSize;

        // Split RTs billing account groups to billing account/seller groups
        if (log.isDebugEnabled()) {
            log.debug("Split {} RTs for {}/{} in to billing account/seller/invoice type groups. {} RTs to retrieve.", ratedTransactions.size(),
                    entityToInvoice.getClass().getSimpleName(), entityToInvoice.getId(), moreRts ? "More" : "No more");
        }
        // Instantiated invoices. Key ba.id_seller.id_invoiceType.id
        Map<String, RatedTransactionGroup> rtGroups = new HashMap<>();

        BillingCycle billingCycle = defaultBillingCycle;
        InvoiceType postPaidInvoiceType = defaultInvoiceType;

        EntityManager em = getEntityManager();

        for (RatedTransaction rt : ratedTransactions) {

            // Order can span multiple billing accounts and some Billing account-dependent values have to be recalculated
            if (entityToInvoice instanceof Order) {
                // Retrieve BA and determine postpaid invoice type only if it has not changed from the last iteration
                if (billingAccount == null || !billingAccount.getId().equals(rt.getBillingAccount().getId())) {
                    billingAccount = rt.getBillingAccount();
                    if (defaultBillingCycle == null) {
                        billingCycle = billingAccount.getBillingCycle();
                    }
                    if (defaultInvoiceType == null) {
                        postPaidInvoiceType = determineInvoiceType(false, isDraft, billingCycle, billingRun, billingAccount);
                    }
                }
            }
            InvoiceType invoiceType = postPaidInvoiceType;
            boolean isPrepaid = rt.isPrepaid();
            if (isPrepaid) {
                invoiceType = determineInvoiceType(true, isDraft, null, null, null);
            }

            String invoiceKey = billingAccount.getId() + "_" + rt.getSeller().getId() + "_" + invoiceType.getId() + "_" + isPrepaid;
            RatedTransactionGroup rtGroup = rtGroups.get(invoiceKey);

            if (rtGroup == null) {
                rtGroup = new RatedTransactionGroup(billingAccount, rt.getSeller(), billingCycle != null ? billingCycle : billingAccount.getBillingCycle(), invoiceType, isPrepaid);
                rtGroups.put(invoiceKey, rtGroup);
            }
            rtGroup.getRatedTransactions().add(rt);

            em.detach(rt);
        }

        List<RatedTransactionGroup> convertedRtGroups = new ArrayList<>();

        // Check if any script to run to group rated transactions by invoice type or other parameters. Script accepts a RatedTransaction list object as an input.
        for (RatedTransactionGroup rtGroup : rtGroups.values()) {

            if (rtGroup.getBillingCycle().getScriptInstance() != null) {
                convertedRtGroups.addAll(executeBCScript(billingRun, rtGroup.getInvoiceType(), rtGroup.getRatedTransactions(), entityToInvoice,
                        rtGroup.getBillingCycle().getScriptInstance().getCode()));
            } else {
                convertedRtGroups.add(rtGroup);
            }
        }

        return new RatedTransactionsToInvoice(moreRts, convertedRtGroups);

    }

    /**
     * Creates invoices and their aggregates - IN new transaction
     *
     * @param entityToInvoice                  entity to be billed
     * @param billingRun                       billing run
     * @param ratedTransactionFilter           rated transaction filter
     * @param invoiceDate                      date of invoice
     * @param firstTransactionDate             date of first transaction
     * @param lastTransactionDate              date of last transaction
     * @param instantiateMinRtsForService      Should rated transactions to reach minimum invoicing amount be checked and instantiated on service level.
     * @param instantiateMinRtsForSubscription Should rated transactions to reach minimum invoicing amount be checked and instantiated on subscription level.
     * @param instantiateMinRtsForBA           Should rated transactions to reach minimum invoicing amount be checked and instantiated on Billing account level.
     * @param isDraft                          Is this a draft invoice
     * @return A list of created invoices
     * @throws BusinessException business exception
     */
    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Invoice> createAgregatesAndInvoiceInNewTransaction(IBillableEntity entityToInvoice, BillingRun billingRun, Filter ratedTransactionFilter, Date invoiceDate,
            Date firstTransactionDate, Date lastTransactionDate, MinAmountForAccounts minAmountForAccounts,
            boolean isDraft) throws BusinessException {
        //MinAmountForAccounts minAmountForAccounts = new MinAmountForAccounts(instantiateMinRtsForBA, false, instantiateMinRtsForSubscription, instantiateMinRtsForService);
        return createAgregatesAndInvoice(entityToInvoice, billingRun, ratedTransactionFilter, invoiceDate, firstTransactionDate, lastTransactionDate, minAmountForAccounts,
                isDraft);
    }

    /**
     * Creates invoices and their aggregates
     *
     * @param entityToInvoice                  entity to be billed
     * @param billingRun                       billing run
     * @param ratedTransactionFilter           rated transaction filter
     * @param invoiceDate                      date of invoice
     * @param firstTransactionDate             date of first transaction
     * @param lastTransactionDate              date of last transaction
     * @param instantiateMinRtsForService      Should rated transactions to reach minimum invoicing amount be checked and instantiated on service level.
     * @param instantiateMinRtsForSubscription Should rated transactions to reach minimum invoicing amount be checked and instantiated on subscription level.
     * @param instantiateMinRtsForBA           Should rated transactions to reach minimum invoicing amount be checked and instantiated on Billing account level.
     * @param isDraft                          Is this a draft invoice
     * @return A list of created invoices
     * @throws BusinessException business exception
     */
    public List<Invoice> createAgregatesAndInvoice(IBillableEntity entityToInvoice, BillingRun billingRun, Filter ratedTransactionFilter, Date invoiceDate,
            Date firstTransactionDate, Date lastTransactionDate, MinAmountForAccounts minAmountForAccounts, boolean isDraft) throws BusinessException {

        log.debug("Will create invoice and aggregates for {}/{}", entityToInvoice.getClass().getSimpleName(), entityToInvoice.getId());

        if (billingRun == null) {
            if (invoiceDate == null) {
                throw new BusinessException("invoiceDate must be set if billingRun is null");
            }
            if (StringUtils.isBlank(lastTransactionDate) && ratedTransactionFilter == null) {
                throw new BusinessException("lastTransactionDate or ratedTransactionFilter must be set if billingRun is null");
            }
        }

        // First retrieve it here as not to loose it if billable entity is not managed and has to be retrieved
        List<RatedTransaction> minAmountTransactions = entityToInvoice.getMinRatedTransactions();

        try {
            BillingAccount ba = null;

            if (entityToInvoice instanceof Subscription) {
                entityToInvoice = subscriptionService.retrieveIfNotManaged((Subscription) entityToInvoice);
                ba = ((Subscription) entityToInvoice).getUserAccount().getBillingAccount();
            } else if (entityToInvoice instanceof BillingAccount) {
                entityToInvoice = billingAccountService.retrieveIfNotManaged((BillingAccount) entityToInvoice);
                ba = (BillingAccount) entityToInvoice;
            } else if (entityToInvoice instanceof Order) {
                entityToInvoice = orderService.retrieveIfNotManaged((Order) entityToInvoice);
            }

            if (billingRun != null) {
                billingRun = billingRunService.retrieveIfNotManaged(billingRun);
            }

            if (firstTransactionDate == null) {
                firstTransactionDate = new Date(0);
            }

            if (billingRun != null) {
                lastTransactionDate = billingRun.getLastTransactionDate();
                invoiceDate = billingRun.getInvoiceDate();
            }

            if (Boolean.parseBoolean(paramBeanFactory.getInstance().getProperty("reset.lastTransactionDate", "true"))) {
                lastTransactionDate = DateUtils.setTimeToZero(lastTransactionDate);
            }

            // Instantiate additional RTs to reach minimum amount to invoice on service, subscription or BA level if needed
            if (isMinAmountApplies(entityToInvoice, minAmountForAccounts)) {
                ratedTransactionService.calculateAmountsAndCreateMinAmountTransactions(entityToInvoice, firstTransactionDate, lastTransactionDate, false, minAmountForAccounts);
                minAmountTransactions = entityToInvoice.getMinRatedTransactions();
            }

            BillingCycle billingCycle = billingRun != null ? billingRun.getBillingCycle() : entityToInvoice.getBillingCycle();
            if (billingCycle == null && !(entityToInvoice instanceof Order)) {
                billingCycle = ba.getBillingCycle();
            }

            // Payment method is calculated on Order or Customer Account level and will be the same for all rated transactions
            PaymentMethod paymentMethod = null;

            // Due balance are calculated on CA level and will be the same for all rated transactions
            BigDecimal balanceDue = null;
            BigDecimal totalInvoiceBalance = null;
            InvoiceType invoiceType = null;

            if (entityToInvoice instanceof Order) {
                paymentMethod = ((Order) entityToInvoice).getPaymentMethod();

            } else {
                paymentMethod = customerAccountService.getPreferredPaymentMethod(ba.getCustomerAccount().getId());
                balanceDue = customerAccountService.customerAccountBalanceDue(ba.getCustomerAccount(), new Date());
                totalInvoiceBalance = customerAccountService.customerAccountFutureBalanceExigibleWithoutLitigation(ba.getCustomerAccount());
                invoiceType = determineInvoiceType(false, isDraft, billingCycle, billingRun, ba);
            }

            // Store RTs, to reach minimum amount per invoice, to DB
            if (minAmountTransactions != null && !minAmountTransactions.isEmpty()) {
                for (RatedTransaction minRatedTransaction : minAmountTransactions) {
                    // This is needed, as even if ratedTransactionService.create() is called and then sql is called to retrieve RTs, these minAmountTransactions will contain
                    // unmanaged
                    // BA and invoiceSubcategory entities
                    minRatedTransaction.setBillingAccount(billingAccountService.retrieveIfNotManaged(minRatedTransaction.getBillingAccount()));
                    minRatedTransaction.setInvoiceSubCategory(invoiceSubcategoryService.retrieveIfNotManaged(minRatedTransaction.getInvoiceSubCategory()));

                    ratedTransactionService.create(minRatedTransaction);
                }
                // Flush RTs to DB as next interaction with RT table will be via sqls only.
                commit();
            }

            return createAggregatesAndInvoiceFromRTs(entityToInvoice, billingRun, ratedTransactionFilter, invoiceDate, firstTransactionDate, lastTransactionDate, isDraft,
                    billingCycle, ba, paymentMethod, invoiceType, balanceDue, totalInvoiceBalance);

        } catch (Exception e) {
            log.error("Error for entity {}", entityToInvoice.getCode(), e);
            if (entityToInvoice instanceof BillingAccount) {
                BillingAccount ba = (BillingAccount) entityToInvoice;
                if (billingRun != null) {
                    rejectedBillingAccountService.create(ba, getEntityManager().getReference(BillingRun.class, billingRun.getId()), e.getMessage());
                } else {
                    throw e instanceof BusinessException ? (BusinessException) e : new BusinessException(e);
                }
            } else {
                throw e instanceof BusinessException ? (BusinessException) e : new BusinessException(e);
            }
        }
        return null;
    }

    /**
     * @param entityToInvoice
     * @param minAmountForAccounts
     * @return
     */
    private boolean isMinAmountApplies(IBillableEntity entityToInvoice, MinAmountForAccounts minAmountForAccounts) {
        if (minAmountForAccounts.isServiceHasMinAmount()) {
            return true;
        }
        if ((minAmountForAccounts.isSubscriptionHasMinAmount() && (entityToInvoice instanceof Subscription && !StringUtils
                .isBlank(((Subscription) entityToInvoice).getMinimumAmountEl())))) {
            return true;
        }
        if(minAmountForAccounts.isUaHasMinAmount()){
            return true;
        }

        if ((minAmountForAccounts.isBaHasMinAmount() && (entityToInvoice instanceof BillingAccount && !StringUtils
                .isBlank(((BillingAccount) entityToInvoice).getMinimumAmountEl())))) {
            return true;
        }
        return false;
    }

    /**
     * Create invoices and aggregates for a given entity to invoice and date interval.
     *
     * @param entityToInvoice        Entity to invoice
     * @param billingRun             Billing run
     * @param ratedTransactionFilter Filter returning a list of rated transactions
     * @param invoiceDate            Invoice date
     * @param firstTransactionDate   Transaction usage date filter - start date
     * @param lastTransactionDate    Transaction usage date filter - end date
     * @param isDraft                Is it a draft invoice
     * @param defaultBillingCycle    Billing cycle applicable to billable entity or to billing run. For Order, if not provided at order level, will have to be determined from Order's
     *                               billing account.
     * @param billingAccount         Payment method. Provided in case of Billing account or Subscription billable entity type. Order can span multiple billing accounts and therefore will
     *                               be determined for each rated transaction.
     * @param defaultPaymentMethod   Payment method. Provided in case of Billing account or Subscription billable entity type. Order can span multiple billing accounts and therefore
     *                               will be determined for each billing account occurrence.
     * @param defaultInvoiceType     Invoice type. A default invoice type for postpaid rated transactions. In case of prepaid RTs, a prepaid invoice type is used. Provided in case of
     *                               Billing account or Subscription billable entity type. Order can span multiple billing accounts and therefore will be determined for each billing account occurrence.
     * @param balanceDue             Balance due. Provided in case of Billing account or Subscription billable entity type. Order can span multiple billing accounts and therefore will be
     *                               determined for each billing account occurrence.
     * @param totalInvoiceBalance    Total invoice balance. Provided in case of Billing account or Subscription billable entity type. Order can span multiple billing accounts and
     *                               therefore will be determined for each billing account occurrence.
     * @return A list of invoices
     * @throws BusinessException General business exception
     */
    @SuppressWarnings("unchecked")
    private List<Invoice> createAggregatesAndInvoiceFromRTs(IBillableEntity entityToInvoice, BillingRun billingRun, Filter ratedTransactionFilter, Date invoiceDate,
            Date firstTransactionDate, Date lastTransactionDate, boolean isDraft, BillingCycle defaultBillingCycle, BillingAccount billingAccount,
            PaymentMethod defaultPaymentMethod, InvoiceType defaultInvoiceType, BigDecimal balanceDue, BigDecimal totalInvoiceBalance) throws BusinessException {

        List<Invoice> invoiceList = new ArrayList<>();
        boolean moreRatedTransactionsExpected = true;

        PaymentMethod paymentMethod = defaultPaymentMethod;

        Map<String, InvoiceAggregateProcessingInfo> rtGroupToInvoiceMap = new HashMap<>();

        boolean allRTsInOneRun = true;

        while (moreRatedTransactionsExpected) {

            if (entityToInvoice instanceof Order) {
                billingAccount = null;
                defaultInvoiceType = null;
            }

            // Retrieve Rated transactions and split them into BA/seller combinations
            RatedTransactionsToInvoice rtsToInvoice = getRatedTransactionGroups(entityToInvoice, billingAccount, billingRun, defaultBillingCycle, defaultInvoiceType,
                    ratedTransactionFilter, firstTransactionDate, lastTransactionDate, isDraft);

            List<RatedTransactionGroup> ratedTransactionGroupsPaged = rtsToInvoice.ratedTransactionGroups;
            moreRatedTransactionsExpected = rtsToInvoice.moreRatedTransactions;
            if (moreRatedTransactionsExpected) {
                allRTsInOneRun = false;
            }

            if (rtGroupToInvoiceMap.isEmpty() && ratedTransactionGroupsPaged.isEmpty()) {
                log.warn("Account {}/{} has no billable transactions", entityToInvoice.getClass().getSimpleName(), entityToInvoice.getId());
                return new ArrayList<>();
                // throw new BusinessException(resourceMessages.getString("error.invoicing.noTransactions"));

                // Process newly retrieved rated transactions

            } else if (!ratedTransactionGroupsPaged.isEmpty()) {

                // Process each BA/seller/invoiceType combination separately, what corresponds to a separate invoice
                for (RatedTransactionGroup rtGroup : ratedTransactionGroupsPaged) {

                    // For order calculate for each BA
                    if (entityToInvoice instanceof Order) {
                        if (billingAccount == null || !billingAccount.getId().equals(rtGroup.getBillingAccount().getId())) {
                            billingAccount = rtGroup.getBillingAccount();
                            if (defaultPaymentMethod == null) {
                                paymentMethod = customerAccountService.getPreferredPaymentMethod(billingAccount.getCustomerAccount().getId());
                            }
                            // Due balance are calculated on CA level and will be the same for all rated transactions
                            balanceDue = customerAccountService.customerAccountBalanceDue(billingAccount.getCustomerAccount(), new Date());
                            totalInvoiceBalance = customerAccountService.customerAccountFutureBalanceExigibleWithoutLitigation(billingAccount.getCustomerAccount());
                        }
                    }

                    String invoiceKey = rtGroup.getInvoiceKey();

                    InvoiceAggregateProcessingInfo invoiceAggregateProcessingInfo = rtGroupToInvoiceMap.get(invoiceKey);
                    if (invoiceAggregateProcessingInfo == null) {
                        invoiceAggregateProcessingInfo = new InvoiceAggregateProcessingInfo();
                        rtGroupToInvoiceMap.put(invoiceKey, invoiceAggregateProcessingInfo);
                    }

                    if (invoiceAggregateProcessingInfo.invoice == null) {
                        invoiceAggregateProcessingInfo.invoice = instantiateInvoice(entityToInvoice, rtGroup.getBillingAccount(), rtGroup.getSeller(), billingRun, invoiceDate,
                                isDraft, rtGroup.getBillingCycle(), paymentMethod, rtGroup.getInvoiceType(), rtGroup.isPrepaid(), balanceDue.add(totalInvoiceBalance));
                        invoiceList.add(invoiceAggregateProcessingInfo.invoice);
                    }

                    Invoice invoice = invoiceAggregateProcessingInfo.invoice;

                    // Create aggregates.
                    // Indicate that no more RTs to process only in case when all RTs were retrieved for processing in a single query page.
                    // In other case - need to close invoices when all RTs are processed
                    appendInvoiceAgregates(entityToInvoice, rtGroup.getBillingAccount(), invoice, rtGroup.getRatedTransactions(), false, invoiceAggregateProcessingInfo,
                            !allRTsInOneRun);

                    // Collect information needed to update RTs with invoice information

                    //          Start of alternative 1 for 4326 // TODO 4326 alternative
                    List<Object[]> rtMassUpdates = new ArrayList<>();
                    List<Object[]> rtUpdates = new ArrayList<>();

                    for (SubCategoryInvoiceAgregate subAggregate : invoiceAggregateProcessingInfo.subCategoryAggregates.values()) {
                        if (subAggregate.getRatedtransactionsToAssociate() == null) {
                            continue;
                        }
                        List<Long> rtIds = new ArrayList<>();
                        List<RatedTransaction> rts = new ArrayList<>();

                        for (RatedTransaction rt : subAggregate.getRatedtransactionsToAssociate()) {

                            // Check that tax was not overridden in WO and tax recalculation should be ignored
                            if (rt.isTaxRecalculated()) {
                                rts.add(rt);
                            } else {
                                rtIds.add(rt.getId());
                            }
                        }

                        if (!rtIds.isEmpty()) {
                            rtMassUpdates.add(new Object[] { subAggregate, rtIds });
                        } else if (!rts.isEmpty()) {
                            rtUpdates.add(new Object[] { subAggregate, rts });
                        }
                        subAggregate.setRatedtransactionsToAssociate(new ArrayList<>());
                    }

                    // End of alternative 1 for 4326
                    // Start of alternative 2 for 4326
                    //            List<RatedTransaction> rtsToUpdate = new ArrayList<>();
                    //                for (SubCategoryInvoiceAgregate subAggregate : invoiceAggregateProcessingInfo.subCategoryAggregates.values()) {
                    //                    if (subAggregate.getRatedtransactionsToAssociate() == null) {
                    //                        continue;
                    //                    }
                    //                    rtsToUpdate.addAll(subAggregate.getRatedtransactionsToAssociate());
                    //                }
                    // End of alternative 2 for 4326

                    EntityManager em = getEntityManager();

                    // Save invoice and its aggregates during the first pagination run, or save only newly created aggregates during later pagination runs
                    if (invoice.getId() == null) {
                        this.create(invoice);

                    } else {
                        for (InvoiceAgregate invoiceAggregate : invoice.getInvoiceAgregates()) {
                            if (invoiceAggregate.getId() == null) {
                                em.persist(invoiceAggregate);
                            }
                        }
                    }

                    // Update RTs with invoice information

                    // AKK alternative 1 for 4326

                    em.flush(); // Need to flush, so RTs can be updated in mass

                    for (Object[] aggregateAndRtIds : rtMassUpdates) {
                        SubCategoryInvoiceAgregate subCategoryAggregate = (SubCategoryInvoiceAgregate) aggregateAndRtIds[0];
                        List<Long> rtIds = (List<Long>) aggregateAndRtIds[1];
                        em.createNamedQuery("RatedTransaction.massUpdateWithInvoiceInfo").setParameter("billingRun", billingRun).setParameter("invoice", invoice)
                                .setParameter("invoiceAgregateF", subCategoryAggregate).setParameter("ids", rtIds).executeUpdate();
                    }

                    for (Object[] aggregateAndRts : rtUpdates) {
                        SubCategoryInvoiceAgregate subCategoryAggregate = (SubCategoryInvoiceAgregate) aggregateAndRts[0];
                        List<RatedTransaction> rts = (List<RatedTransaction>) aggregateAndRts[1];
                        for (RatedTransaction rt : rts) {
                            rt.setBillingRun(billingRun);
                            rt.setInvoice(invoice);
                            rt.setInvoiceAgregateF(subCategoryAggregate);
                            rt.changeStatus(RatedTransactionStatusEnum.BILLED);
                            em.merge(rt);
                        }
                    }
                    // End of alternative 1 for 4326
                    // Start of alternative 2 for 4326
                    // ratedTransactionService.updateViaDeleteAndInsert(rtsToUpdate);
                    // End of alternative 2 for 4326
                }
            }
        }

        // Finalize invoices

        for (InvoiceAggregateProcessingInfo invoiceAggregateProcessingInfo : rtGroupToInvoiceMap.values()) {

            // Create discount, category and tax aggregates if not all RTs were retrieved and processed in a single page
            if (!allRTsInOneRun) {
                addDiscountCategoryAndTaxAggregates(invoiceAggregateProcessingInfo.invoice, invoiceAggregateProcessingInfo.subCategoryAggregates.values());
            }

            // Link orders to invoice
            Set<String> orderNums = invoiceAggregateProcessingInfo.orderNumbers;
            if (entityToInvoice instanceof Order) {
                orderNums.add(((Order) entityToInvoice).getOrderNumber());
            }
            if (orderNums != null && !orderNums.isEmpty()) {
                List<Order> orders = new ArrayList<Order>();
                for (String orderNum : orderNums) {
                    orders.add(orderService.findByCodeOrExternalId(orderNum));
                }
                invoiceAggregateProcessingInfo.invoice.setOrders(orders);
            }

            invoiceAggregateProcessingInfo.invoice.assignTemporaryInvoiceNumber();
            postCreate(invoiceAggregateProcessingInfo.invoice);
        }

        return invoiceList;

    }

    /**
     * Check if the electronic billing is enabled.
     *
     * @param invoice the invoice.
     * @return True if electronic billing is enabled for any Billable entity, false else.
     */
    private boolean isElectronicBillingEnabled(Invoice invoice) {
        boolean isElectronicBillingEnabled = false;

        if (invoice.getBillingAccount() != null) {
            isElectronicBillingEnabled = invoice.getBillingAccount().getElectronicBilling();
        }
        if (invoice.getSubscription() != null) {
            isElectronicBillingEnabled = invoice.getSubscription().getElectronicBilling();
        }
        if (invoice.getOrder() != null) {
            isElectronicBillingEnabled = invoice.getOrder().getElectronicBilling();
        }
        return isElectronicBillingEnabled;
    }

    /**
     * Execute a script to group rated transactions by invoice type
     *
     * @param billingRun         Billing run
     * @param invoiceType        Current Invoice type
     * @param ratedTransactions  Rated transactions to group
     * @param entity             Entity to invoice
     * @param scriptInstanceCode Script to execute
     * @return A list of rated transaction groups
     * @throws BusinessException
     */
    @SuppressWarnings("unchecked")
    private List<RatedTransactionGroup> executeBCScript(BillingRun billingRun, InvoiceType invoiceType, List<RatedTransaction> ratedTransactions, IBillableEntity entity,
            String scriptInstanceCode) throws BusinessException {

        HashMap<String, Object> context = new HashMap<String, Object>();
        context.put(Script.CONTEXT_ENTITY, entity);
        context.put(Script.CONTEXT_CURRENT_USER, currentUser);
        context.put(Script.CONTEXT_APP_PROVIDER, appProvider);
        context.put("br", billingRun);
        context.put("invoiceType", invoiceType);
        context.put("ratedTransactions", ratedTransactions);
        scriptInstanceService.executeCached(scriptInstanceCode, context);
        return (List<RatedTransactionGroup>) context.get(Script.RESULT_VALUE);
    }

    /**
     * Creates Invoice and its aggregates in memory.
     *
     * @param ratedTransactions list of rated transaction
     * @param billingAccount    billing account
     * @param invoiceType       type of invoice
     * @return invoice
     * @throws BusinessException business exception
     */
    public Invoice createAgregatesAndInvoiceVirtual(List<RatedTransaction> ratedTransactions, BillingAccount billingAccount, InvoiceType invoiceType) throws BusinessException {

        if (invoiceType == null) {
            invoiceType = invoiceTypeService.getDefaultCommertial();
        }
        Invoice invoice = new Invoice();
        invoice.setSeller(billingAccount.getCustomerAccount().getCustomer().getSeller());
        invoice.setInvoiceType(invoiceType);
        invoice.setBillingAccount(billingAccount);
        invoice.setInvoiceDate(new Date());
        serviceSingleton.assignInvoiceNumberVirtual(invoice);

        PaymentMethod preferedPaymentMethod = invoice.getBillingAccount().getCustomerAccount().getPreferredPaymentMethod();
        if (preferedPaymentMethod != null) {
            invoice.setPaymentMethodType(preferedPaymentMethod.getPaymentType());
        }

        appendInvoiceAgregates(billingAccount, billingAccount, invoice, ratedTransactions, false, null, false);
        invoice.setTemporaryInvoiceNumber(UUID.randomUUID().toString());

        return invoice;
    }

    /**
     * Find by billing run.
     *
     * @param billingRun billing run
     * @return list of invoice for given billing run
     */
    @SuppressWarnings("unchecked")
    public List<Invoice> findByBillingRun(BillingRun billingRun) {
        QueryBuilder qb = new QueryBuilder(Invoice.class, "i");
        qb.addCriterionEntity("billingRun", billingRun);

        try {
            return (List<Invoice>) qb.getQuery(getEntityManager()).getResultList();
        } catch (NoResultException e) {
            log.warn("failed to find by billingRun", e);
            return null;
        }
    }

    /**
     * Produce invoice pdf in new transaction.
     *
     * @param invoiceId id of invoice
     * @throws BusinessException business exception
     */
    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void produceInvoicePdfInNewTransaction(Long invoiceId) throws BusinessException {
        Invoice invoice = findById(invoiceId);
        produceInvoicePdf(invoice);
    }

    /**
     * Produce invoice's PDF file and update invoice record in DB.
     *
     * @param invoice Invoice
     * @return Update invoice entity
     * @throws BusinessException business exception
     */
    public Invoice produceInvoicePdf(Invoice invoice) throws BusinessException {

        produceInvoicePdfNoUpdate(invoice);
        invoice.setStatus(InvoiceStatusEnum.GENERATED);
        invoice = updateNoCheck(invoice);
        return invoice;
    }

    /**
     * Produce invoice. v5.0 Refresh jasper template without restarting wildfly
     *
     * @param invoice invoice to generate pdf
     * @throws BusinessException business exception
     * @author akadid abdelmounaim
     * @lastModifiedVersion 5.0
     */
    public void produceInvoicePdfNoUpdate(Invoice invoice) throws BusinessException {
        log.debug("Creating pdf for invoice id={} number={}", invoice.getId(), invoice.getInvoiceNumberOrTemporaryNumber());

        ParamBean paramBean = paramBeanFactory.getInstance();
        String meveoDir = paramBean.getChrootDir(currentUser.getProviderCode()) + File.separator;
        String invoiceXmlFileName = getFullXmlFilePath(invoice, false);
        Map<String, Object> parameters = pDFParametersConstruction.constructParameters(invoice, currentUser.getProviderCode());

        String INVOICE_TAG_NAME = "invoice";

        boolean isInvoiceAdjustment = invoice.getInvoiceType().getCode().equals(invoiceTypeService.getAdjustementCode());

        File invoiceXmlFile = new File(invoiceXmlFileName);
        if (!invoiceXmlFile.exists()) {
            produceInvoiceXmlNoUpdate(invoice);
        }

        BillingAccount billingAccount = invoice.getBillingAccount();

        BillingCycle billingCycle = null;
        if (billingAccount != null && billingAccount.getBillingCycle() != null) {
            billingCycle = billingAccount.getBillingCycle();
        }

        String billingTemplateName = getInvoiceTemplateName(invoice, billingCycle, invoice.getInvoiceType());

        String resDir = meveoDir + "jasper";

        String pdfFilename = getOrGeneratePdfFilename(invoice);
        invoice.setPdfFilename(pdfFilename);
        String pdfFullFilename = getFullPdfFilePath(invoice, true);
        InputStream reportTemplate = null;
        try {
            File destDir = new File(resDir + File.separator + billingTemplateName + File.separator + "pdf");

            if (!destDir.exists()) {

                String sourcePath =
                        Thread.currentThread().getContextClassLoader().getResource("./jasper").getPath() + File.separator + billingTemplateName + File.separator + "invoice";

                File sourceFile = new File(sourcePath);
                if (!sourceFile.exists()) {
                    VirtualFile vfDir = VFS.getChild(
                            "content/" + ParamBeanFactory.getAppScopeInstance().getProperty("opencell.moduleName", "opencell") + ".war/WEB-INF/classes/jasper/"
                                    + billingTemplateName + File.separator + "invoice");
                    log.info("default jaspers path :" + vfDir.getPathName());
                    URL vfPath = VFSUtils.getPhysicalURL(vfDir);
                    sourceFile = new File(vfPath.getPath());

                    // if (!sourceFile.exists()) {
                    //
                    // sourcePath = Thread.currentThread().getContextClassLoader().getResource("./jasper").getPath() + "default/invoice";
                    // sourceFile = new File(sourcePath);

                    if (!sourceFile.exists()) {
                        throw new BusinessException("embedded jasper report for invoice is missing..");
                    }
                    // }
                }
                destDir.mkdirs();
                FileUtils.copyDirectory(sourceFile, destDir);
            }

            File destDirInvoiceAdjustment = new File(resDir + File.separator + billingTemplateName + File.separator + "invoiceAdjustmentPdf");
            if (!destDirInvoiceAdjustment.exists() && isInvoiceAdjustment) {
                destDirInvoiceAdjustment.mkdirs();
                String sourcePathInvoiceAdjustment =
                        Thread.currentThread().getContextClassLoader().getResource("./jasper").getPath() + File.separator + billingTemplateName + "/invoiceAdjustment";
                File sourceFileInvoiceAdjustment = new File(sourcePathInvoiceAdjustment);
                if (!sourceFileInvoiceAdjustment.exists()) {
                    VirtualFile vfDir = VFS.getChild(
                            "content/" + ParamBeanFactory.getAppScopeInstance().getProperty("opencell.moduleName", "opencell") + ".war/WEB-INF/classes/jasper/"
                                    + billingTemplateName + "/invoiceAdjustment");
                    URL vfPath = VFSUtils.getPhysicalURL(vfDir);
                    sourceFileInvoiceAdjustment = new File(vfPath.getPath());
                    if (!sourceFileInvoiceAdjustment.exists()) {

                        URL resource = Thread.currentThread().getContextClassLoader().getResource("./jasper/" + billingTemplateName + "/invoiceAdjustment");

                        if (resource == null) {
                            resource = Thread.currentThread().getContextClassLoader().getResource("./jasper/default/invoiceAdjustment");
                        }

                        if (resource == null) {
                            throw new BusinessException("embedded InvoiceAdjustment jasper report for invoice is missing!");
                        }

                        sourcePathInvoiceAdjustment = resource.getPath();

                        if (!sourceFileInvoiceAdjustment.exists()) {
                            throw new BusinessException("embedded jasper report for invoice is missing.");
                        }

                    }
                }
                FileUtils.copyDirectory(sourceFileInvoiceAdjustment, destDirInvoiceAdjustment);

            }

            CustomerAccount customerAccount = billingAccount.getCustomerAccount();
            PaymentMethod preferedPaymentMethod = customerAccount.getPreferredPaymentMethod();
            PaymentMethodEnum paymentMethodEnum = null;

            if (preferedPaymentMethod != null) {
                paymentMethodEnum = preferedPaymentMethod.getPaymentType();
            }

            File jasperFile = getJasperTemplateFile(resDir, billingTemplateName, paymentMethodEnum, isInvoiceAdjustment);
            if (!jasperFile.exists()) {
                throw new InvoiceJasperNotFoundException("The jasper file doesn't exist.");
            }
            log.debug("Jasper template used: {}", jasperFile.getCanonicalPath());

            reportTemplate = new FileInputStream(jasperFile);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document xmlDocument = db.parse(invoiceXmlFile);
            xmlDocument.getDocumentElement().normalize();
            Node invoiceNode = xmlDocument.getElementsByTagName(INVOICE_TAG_NAME).item(0);
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter writer = new StringWriter();
            trans.transform(new DOMSource(xmlDocument), new StreamResult(writer));

            JRXmlDataSource dataSource = new JRXmlDataSource(new ByteArrayInputStream(getNodeXmlString(invoiceNode).getBytes(StandardCharsets.UTF_8)), "/invoice");

            String fileKey = jasperFile.getPath() + jasperFile.lastModified();
            JasperReport jasperReport = jasperReportMap.get(fileKey);
            if (jasperReport == null) {
                jasperReport = (JasperReport) JRLoader.loadObject(reportTemplate);
                jasperReportMap.put(fileKey, jasperReport);
            }

            DefaultJasperReportsContext context = DefaultJasperReportsContext.getInstance();
            JRPropertiesUtil.getInstance(context).setProperty("net.sf.jasperreports.xpath.executer.factory", "net.sf.jasperreports.engine.util.xml.JaxenXPathExecuterFactory");

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            JasperExportManager.exportReportToPdfFile(jasperPrint, pdfFullFilename);
            if ("true".equals(paramBeanFactory.getInstance().getProperty("invoice.pdf.addWaterMark", "true"))) {
                if (invoice.getInvoiceType().getCode().equals(paramBeanFactory.getInstance().getProperty("invoiceType.draft.code", "DRAFT")) || (invoice.isDraft() != null
                        && invoice.isDraft())) {
                    PdfWaterMark.add(pdfFullFilename, paramBean.getProperty("invoice.pdf.waterMark", "PROFORMA"), null);
                }
            }
            invoice.setPdfFilename(pdfFilename);

            log.info("PDF file '{}' produced for invoice {}", pdfFullFilename, invoice.getInvoiceNumberOrTemporaryNumber());

        } catch (IOException | JRException | TransformerException | ParserConfigurationException | SAXException e) {
            throw new BusinessException("Failed to generate a PDF file for " + pdfFilename, e);
        } finally {
            IOUtils.closeQuietly(reportTemplate);
        }
    }

    /**
     * Delete invoice's PDF file.
     *
     * @param invoice Invoice
     * @return True if file was deleted
     * @throws BusinessException business exception
     */
    public Invoice deleteInvoicePdf(Invoice invoice) throws BusinessException {

        String pdfFilename = getFullPdfFilePath(invoice, false);

        invoice.setPdfFilename(null);
        invoice = update(invoice);

        File file = new File(pdfFilename);
        if (file.exists()) {
            file.delete();
        }
        return invoice;
    }

    /**
     * Gets the jasper template file.
     *
     * @param resDir              resource directory
     * @param billingTemplate     billing template
     * @param paymentMethod       payment method
     * @param isInvoiceAdjustment true/false
     * @return jasper file
     */
    private File getJasperTemplateFile(String resDir, String billingTemplate, PaymentMethodEnum paymentMethod, boolean isInvoiceAdjustment) {
        String pdfDirName = new StringBuilder(resDir).append(File.separator).append(billingTemplate).append(File.separator)
                .append(isInvoiceAdjustment ? ADJUSTEMENT_DIR_NAME : PDF_DIR_NAME).toString();

        File pdfDir = new File(pdfDirName);
        String paymentMethodFileName = new StringBuilder("invoice_").append(paymentMethod).append(".jasper").toString();
        File paymentMethodFile = new File(pdfDir, paymentMethodFileName);

        if (paymentMethodFile.exists()) {
            return paymentMethodFile;
        } else {
            File defaultTemplate = new File(pdfDir, INVOICE_TEMPLATE_FILENAME);
            return defaultTemplate;
        }
    }

    /**
     * Gets the node xml string.
     *
     * @param node instance of Node.
     * @return xml node as string
     */
    protected String getNodeXmlString(Node node) {
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            StringWriter buffer = new StringWriter();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(node), new StreamResult(buffer));
            return buffer.toString();
        } catch (Exception e) {
            log.error("Error converting xml node to its string representation. {}", e);
            throw new ConfigurationException();
        }
    }

    /**
     * Format invoice date.
     *
     * @param invoiceDate invoice date
     * @return invoice date as string
     */
    public String formatInvoiceDate(Date invoiceDate) {
        DateFormat dateFormat = new SimpleDateFormat(DATE_PATERN);
        return dateFormat.format(invoiceDate);
    }

    /**
     * Evaluate prefix el expression.
     *
     * @param prefix  prefix of EL expression
     * @param invoice invoice
     * @return evaluated value
     * @throws BusinessException business exception
     */
    public static String evaluatePrefixElExpression(String prefix, Invoice invoice) throws BusinessException {

        if (StringUtils.isBlank(prefix)) {
            return null;
        }
        Map<Object, Object> userMap = new HashMap<Object, Object>();
        if (prefix.indexOf("entity") >= 0) {
            userMap.put("entity", invoice);
        }
        if (prefix.indexOf("invoice") >= 0) {
            userMap.put("invoice", invoice);
        }

        String result = ValueExpressionWrapper.evaluateExpression(prefix, userMap, String.class);

        return result;
    }

    /**
     * Recompute aggregates.
     *
     * @param invoice invoice
     * @throws BusinessException business exception
     */
    public void recomputeAggregates(Invoice invoice) throws BusinessException {

        boolean entreprise = appProvider.isEntreprise();
        int invoiceRounding = appProvider.getInvoiceRounding();
        RoundingModeEnum invoiceRoundingMode = appProvider.getInvoiceRoundingMode();

        BillingAccount billingAccount = billingAccountService.findById(invoice.getBillingAccount().getId());
        BigDecimal nonEnterprisePriceWithTax = BigDecimal.ZERO;

        Map<Long, TaxInvoiceAgregate> taxInvoiceAgregateMap = new HashMap<Long, TaxInvoiceAgregate>();
        List<SubCategoryInvoiceAgregate> subCategoryInvoiceAgregates = new ArrayList<SubCategoryInvoiceAgregate>();
        invoice.setAmountTax(null);
        invoice.setAmountWithoutTax(null);
        invoice.setAmountWithTax(null);

        // update the aggregated subcat of an invoice
        for (InvoiceAgregate invoiceAggregate : invoice.getInvoiceAgregates()) {
            if (invoiceAggregate instanceof CategoryInvoiceAgregate) {
                invoiceAggregate.resetAmounts();
            } else if (invoiceAggregate instanceof TaxInvoiceAgregate) {
                TaxInvoiceAgregate taxInvoiceAgregate = (TaxInvoiceAgregate) invoiceAggregate;
                taxInvoiceAgregateMap.put(taxInvoiceAgregate.getTax().getId(), taxInvoiceAgregate);
            } else if (invoiceAggregate instanceof SubCategoryInvoiceAgregate) {
                subCategoryInvoiceAgregates.add((SubCategoryInvoiceAgregate) invoiceAggregate);
            }
        }

        for (TaxInvoiceAgregate taxInvoiceAgregate : taxInvoiceAgregateMap.values()) {
            taxInvoiceAgregate.setAmountWithoutTax(new BigDecimal(0));
            for (SubCategoryInvoiceAgregate subCategoryInvoiceAgregate : subCategoryInvoiceAgregates) {
                if (subCategoryInvoiceAgregate.getQuantity().signum() != 0) {
                    if (subCategoryInvoiceAgregate.getTax().equals(taxInvoiceAgregate.getTax())) {
                        taxInvoiceAgregate.addAmountWithoutTax(subCategoryInvoiceAgregate.getAmountWithoutTax());
                    }
                }
            }

            taxInvoiceAgregate.setAmountTax(taxInvoiceAgregate.getAmountWithoutTax().multiply(taxInvoiceAgregate.getTaxPercent()).divide(new BigDecimal("100")));
            // then round the tax
            taxInvoiceAgregate.setAmountTax(taxInvoiceAgregate.getAmountTax().setScale(invoiceRounding, invoiceRoundingMode.getRoundingMode()));

            taxInvoiceAgregate.setAmountWithTax(taxInvoiceAgregate.getAmountWithoutTax().add(taxInvoiceAgregate.getAmountTax()));
        }

        // update the amount with and without tax of all the tax aggregates in
        // each sub category aggregate
        SubCategoryInvoiceAgregate biggestSubCat = null;
        BigDecimal biggestAmount = new BigDecimal("-100000000");

        for (InvoiceAgregate invoiceAgregate : subCategoryInvoiceAgregates) {
            SubCategoryInvoiceAgregate subCategoryInvoiceAgregate = (SubCategoryInvoiceAgregate) invoiceAgregate;

            if (!entreprise) {
                nonEnterprisePriceWithTax = nonEnterprisePriceWithTax.add(subCategoryInvoiceAgregate.getAmountWithTax());
            }

            BigDecimal amountWithoutTax = subCategoryInvoiceAgregate.getAmountWithoutTax();
            subCategoryInvoiceAgregate
                    .setAmountWithoutTax(amountWithoutTax != null ? amountWithoutTax.setScale(invoiceRounding, invoiceRoundingMode.getRoundingMode()) : BigDecimal.ZERO);

            subCategoryInvoiceAgregate.getCategoryInvoiceAgregate().addAmountWithoutTax(subCategoryInvoiceAgregate.getAmountWithoutTax());

            if (subCategoryInvoiceAgregate.getAmountWithoutTax().compareTo(biggestAmount) > 0) {
                biggestAmount = subCategoryInvoiceAgregate.getAmountWithoutTax();
                biggestSubCat = subCategoryInvoiceAgregate;
            }
        }

        for (InvoiceAgregate invoiceAgregate : invoice.getInvoiceAgregates()) {
            if (invoiceAgregate instanceof CategoryInvoiceAgregate) {
                CategoryInvoiceAgregate categoryInvoiceAgregate = (CategoryInvoiceAgregate) invoiceAgregate;
                invoice.addAmountWithoutTax(categoryInvoiceAgregate.getAmountWithoutTax().setScale(invoiceRounding, invoiceRoundingMode.getRoundingMode()));
            }

            if (invoiceAgregate instanceof TaxInvoiceAgregate) {
                TaxInvoiceAgregate taxInvoiceAgregate = (TaxInvoiceAgregate) invoiceAgregate;
                invoice.addAmountTax(taxInvoiceAgregate.getAmountTax().setScale(invoiceRounding, invoiceRoundingMode.getRoundingMode()));
            }
        }

        if (invoice.getAmountWithoutTax() != null) {
            invoice.setAmountWithTax(invoice.getAmountWithoutTax().add(invoice.getAmountTax() == null ? BigDecimal.ZERO : invoice.getAmountTax()));
        }

        if (!entreprise && biggestSubCat != null && !billingAccountService.isExonerated(billingAccount)) {
            BigDecimal delta = nonEnterprisePriceWithTax.subtract(invoice.getAmountWithTax());
            log.debug("delta={}-{}={}", nonEnterprisePriceWithTax, invoice.getAmountWithTax(), delta);

            biggestSubCat.setAmountWithoutTax(biggestSubCat.getAmountWithoutTax().add(delta).setScale(invoiceRounding, invoiceRoundingMode.getRoundingMode()));
            Tax tax = biggestSubCat.getTax();
            TaxInvoiceAgregate invoiceAgregateT = taxInvoiceAgregateMap.get(tax.getId());
            log.debug("tax3 ht={}", invoiceAgregateT.getAmountWithoutTax());

            invoiceAgregateT.setAmountWithoutTax(invoiceAgregateT.getAmountWithoutTax().add(delta).setScale(invoiceRounding, invoiceRoundingMode.getRoundingMode()));
            log.debug("tax4 ht={}", invoiceAgregateT.getAmountWithoutTax());

            CategoryInvoiceAgregate invoiceAgregateR = biggestSubCat.getCategoryInvoiceAgregate();
            invoiceAgregateR.setAmountWithoutTax(invoiceAgregateR.getAmountWithoutTax().add(delta).setScale(invoiceRounding, invoiceRoundingMode.getRoundingMode()));

            invoice.setAmountWithoutTax(invoice.getAmountWithoutTax().add(delta).setScale(invoiceRounding, invoiceRoundingMode.getRoundingMode()));
            invoice.setAmountWithTax(nonEnterprisePriceWithTax.setScale(invoiceRounding, invoiceRoundingMode.getRoundingMode()));
        }

        // calculate discounts here
        // no need to create discount aggregates we will use the one from
        // adjustedInvoice

        Object[] discountAmount = invoiceAgregateService.findTotalAmountsForDiscountAggregates(getLinkedInvoice(invoice));
        BigDecimal discountAmountWithoutTax = (BigDecimal) discountAmount[0];
        BigDecimal discountAmountTax = (BigDecimal) discountAmount[1];
        BigDecimal discountAmountWithTax = (BigDecimal) discountAmount[2];

        log.debug("discountAmountWithoutTax= {}, discountAmountTax={}, discountAmountWithTax={}", discountAmount[0], discountAmount[1], discountAmount[2]);

        invoice.addAmountWithoutTax(round(discountAmountWithoutTax, invoiceRounding, invoiceRoundingMode));
        invoice.addAmountTax(round(discountAmountTax, invoiceRounding, invoiceRoundingMode));
        invoice.addAmountWithTax(round(discountAmountWithTax, invoiceRounding, invoiceRoundingMode));

        // compute net to pay
        BigDecimal netToPay = BigDecimal.ZERO;
        if (entreprise) {
            netToPay = invoice.getAmountWithTax();
        } else {
            BigDecimal balance = customerAccountService.customerAccountBalanceDue(null, invoice.getBillingAccount().getCustomerAccount().getCode(), invoice.getDueDate());

            if (balance == null) {
                throw new BusinessException("account balance calculation failed");
            }
            netToPay = invoice.getAmountWithTax().add(round(balance, invoiceRounding, invoiceRoundingMode));
        }

        invoice.setNetToPay(netToPay);
    }

    /**
     * Recompute sub category aggregate.
     *
     * @param invoice invoice used to recompute
     */
    public void recomputeSubCategoryAggregate(Invoice invoice) {

        int invoiceRounding = appProvider.getInvoiceRounding();
        RoundingModeEnum invoiceRoundingMode = appProvider.getInvoiceRoundingMode();

        List<TaxInvoiceAgregate> taxInvoiceAgregates = new ArrayList<TaxInvoiceAgregate>();
        List<SubCategoryInvoiceAgregate> subCategoryInvoiceAgregates = new ArrayList<SubCategoryInvoiceAgregate>();

        for (InvoiceAgregate invoiceAgregate : invoice.getInvoiceAgregates()) {
            if (invoiceAgregate instanceof TaxInvoiceAgregate) {
                taxInvoiceAgregates.add((TaxInvoiceAgregate) invoiceAgregate);
            } else if (invoiceAgregate instanceof SubCategoryInvoiceAgregate) {
                subCategoryInvoiceAgregates.add((SubCategoryInvoiceAgregate) invoiceAgregate);
            }
        }

        for (TaxInvoiceAgregate taxInvoiceAgregate : taxInvoiceAgregates) {
            taxInvoiceAgregate.setAmountWithoutTax(new BigDecimal(0));
            for (SubCategoryInvoiceAgregate subCategoryInvoiceAgregate : subCategoryInvoiceAgregates) {
                if (subCategoryInvoiceAgregate.getQuantity().signum() != 0) {
                    if (subCategoryInvoiceAgregate.getTax().equals(taxInvoiceAgregate.getTax())) {
                        taxInvoiceAgregate.addAmountWithoutTax(subCategoryInvoiceAgregate.getAmountWithoutTax());
                    }
                }
            }

            taxInvoiceAgregate.setAmountTax(taxInvoiceAgregate.getAmountWithoutTax().multiply(taxInvoiceAgregate.getTaxPercent()).divide(new BigDecimal("100")));
            // then round the tax
            taxInvoiceAgregate.setAmountTax(taxInvoiceAgregate.getAmountTax().setScale(invoiceRounding, invoiceRoundingMode.getRoundingMode()));

            taxInvoiceAgregate.setAmountWithTax(taxInvoiceAgregate.getAmountWithoutTax().add(taxInvoiceAgregate.getAmountTax()));
        }
    }

    /**
     * Find invoices by type.
     *
     * @param invoiceType invoice type
     * @param ba          billing account
     * @return list of invoice for given type
     */
    @SuppressWarnings("unchecked")
    public List<Invoice> findInvoicesByType(InvoiceType invoiceType, BillingAccount ba) {
        List<Invoice> result = new ArrayList<Invoice>();
        QueryBuilder qb = new QueryBuilder(Invoice.class, "i", null);
        qb.addCriterionEntity("billingAccount", ba);
        qb.addCriterionEntity("invoiceType", invoiceType);
        try {
            result = (List<Invoice>) qb.getQuery(getEntityManager()).getResultList();
        } catch (NoResultException e) {
        }
        return result;
    }

    /**
     * Get a full path to an invoice's XML file.
     *
     * @param invoice    Invoice
     * @param createDirs Should missing directories be created
     * @return Absolute path to an XML file
     */
    public String getFullXmlFilePath(Invoice invoice, boolean createDirs) {

        String meveoDir = paramBeanFactory.getChrootDir() + File.separator;

        String xmlFilename = meveoDir + "invoices" + File.separator + "xml" + File.separator + getOrGenerateXmlFilename(invoice);

        if (createDirs) {
            int pos = Integer.max(xmlFilename.lastIndexOf("/"), xmlFilename.lastIndexOf("\\"));
            String dir = xmlFilename.substring(0, pos);
            (new File(dir)).mkdirs();
        }

        return xmlFilename;
    }

    /**
     * Return a XML filename that was assigned to invoice, or in case it was not assigned yet - generate a filename. A default XML filename is
     * invoiceDateOrBillingRunId/invoiceNumber.pdf or invoiceDateOrBillingRunId/_IA_invoiceNumber.pdf for adjustment invoice
     *
     * @param invoice Invoice
     * @return XML file name
     */
    public String getOrGenerateXmlFilename(Invoice invoice) {
        ParamBean paramBean = paramBeanFactory.getInstance();

        if (invoice.getXmlFilename() != null) {
            return invoice.getXmlFilename();
        }

        // Generate a name for xml file from EL expression
        String xmlFileName = null;
        String expression = invoice.getInvoiceType().getXmlFilenameEL();
        if (!StringUtils.isBlank(expression)) {
            Map<Object, Object> contextMap = new HashMap<Object, Object>();
            contextMap.put("invoice", invoice);

            try {
                String value = ValueExpressionWrapper.evaluateExpression(expression, contextMap, String.class);
                if (value != null) {
                    xmlFileName = value;
                }
            } catch (BusinessException e) {
                // Ignore exceptions here - a default XML filename will be used instead. Error is logged in EL evaluation
            }
        }

        // Default to invoiceDateOrBillingRunId/invoiceNumber.xml or invoiceDateOrBillingRunId/_IA_invoiceNumber.xml for adjustment invoice
        if (StringUtils.isBlank(xmlFileName)) {

            boolean isInvoiceAdjustment = invoice.getInvoiceType().getCode().equals(invoiceTypeService.getAdjustementCode());

            BillingRun billingRun = invoice.getBillingRun();
            String brPath = billingRun == null ?
                    DateUtils.formatDateWithPattern(invoice.getInvoiceDate(), paramBean.getProperty("meveo.dateTimeFormat.string", "ddMMyyyy_HHmmss")) :
                    billingRun.getId().toString();

            xmlFileName = brPath + File.separator + (isInvoiceAdjustment ? paramBean.getProperty("invoicing.invoiceAdjustment.prefix", "_IA_") : "") + (!StringUtils
                    .isBlank(invoice.getInvoiceNumber()) ? invoice.getInvoiceNumber() : invoice.getTemporaryInvoiceNumber());
        }

        if (xmlFileName != null && !xmlFileName.toLowerCase().endsWith(".xml")) {
            xmlFileName = xmlFileName + ".xml";
        }
        xmlFileName = StringUtils.normalizeFileName(xmlFileName);
        return xmlFileName;
    }

    /**
     * Get a full path to an invoice's PDF file.
     *
     * @param invoice    Invoice
     * @param createDirs Should missing directories be created
     * @return Absolute path to a PDF file
     */
    public String getFullPdfFilePath(Invoice invoice, boolean createDirs) {

        String meveoDir = paramBeanFactory.getChrootDir() + File.separator;

        String pdfFilename = meveoDir + "invoices" + File.separator + "pdf" + File.separator + getOrGeneratePdfFilename(invoice);

        if (createDirs) {
            int pos = Integer.max(pdfFilename.lastIndexOf("/"), pdfFilename.lastIndexOf("\\"));
            String dir = pdfFilename.substring(0, pos);
            (new File(dir)).mkdirs();
        }

        return pdfFilename;
    }

    /**
     * Return a pdf filename that was assigned to invoice, or in case it was not assigned yet - generate a filename. A default PDF filename is invoiceDate_invoiceNumber.pdf or
     * invoiceDate_IA_invoiceNumber.pdf for adjustment invoice
     *
     * @param invoice Invoice
     * @return Pdf file name
     */
    public String getOrGeneratePdfFilename(Invoice invoice) {

        if (invoice.getPdfFilename() != null) {
            return invoice.getPdfFilename();
        }

        // Generate a name for pdf file from EL expression
        String pdfFileName = null;
        String expression = invoice.getInvoiceType().getPdfFilenameEL();
        if (!StringUtils.isBlank(expression)) {
            Map<Object, Object> contextMap = new HashMap<Object, Object>();
            contextMap.put("invoice", invoice);

            try {
                String value = ValueExpressionWrapper.evaluateExpression(expression, contextMap, String.class);

                if (value != null) {
                    pdfFileName = value;
                }
            } catch (BusinessException e) {
                // Ignore exceptions here - a default pdf filename will be used instead. Error is logged in EL evaluation
            }
        }

        // Default to invoiceDate_invoiceNumber.pdf or invoiceDate_IA_invoiceNumber.pdf for adjustment invoice
        if (StringUtils.isBlank(pdfFileName)) {

            boolean isInvoiceAdjustment = invoice.getInvoiceType().getCode().equals(invoiceTypeService.getAdjustementCode());

            pdfFileName = formatInvoiceDate(invoice.getInvoiceDate()) + (isInvoiceAdjustment ?
                    paramBeanFactory.getInstance().getProperty("invoicing.invoiceAdjustment.prefix", "_IA_") :
                    "_") + (!StringUtils.isBlank(invoice.getInvoiceNumber()) ? invoice.getInvoiceNumber() : invoice.getTemporaryInvoiceNumber());
        }

        if (pdfFileName != null && !pdfFileName.toLowerCase().endsWith(".pdf")) {
            pdfFileName = pdfFileName + ".pdf";
        }
        pdfFileName = StringUtils.normalizeFileName(pdfFileName);
        return pdfFileName;
    }

    /**
     * Produce invoice xml in new transaction.
     *
     * @param invoiceId invoice's id
     * @throws BusinessException business exception
     */
    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void produceInvoiceXmlInNewTransaction(Long invoiceId) throws BusinessException {
        Invoice invoice = findById(invoiceId);
        produceInvoiceXml(invoice);
    }

    /**
     * Produce invoice's XML file and update invoice record in DB.
     *
     * @param invoice Invoice to produce XML for
     * @return Update invoice entity
     * @throws BusinessException business exception
     */
    public Invoice produceInvoiceXml(Invoice invoice) throws BusinessException {

        produceInvoiceXmlNoUpdate(invoice);
        invoice.setStatus(InvoiceStatusEnum.GENERATED);
        invoice = updateNoCheck(invoice);
        return invoice;
    }

    /**
     * Produce invoice's XML file.
     *
     * @param invoice Invoice
     * @throws BusinessException business exception
     */
    public void produceInvoiceXmlNoUpdate(Invoice invoice) throws BusinessException {

        xmlInvoiceCreator.createXMLInvoice(invoice, false);
    }

    /**
     * Delete invoice's XML file.
     *
     * @param invoice Invoice
     * @return True if file was deleted
     * @throws BusinessException business exception
     */
    public Invoice deleteInvoiceXml(Invoice invoice) throws BusinessException {

        String xmlFilename = getFullXmlFilePath(invoice, false);

        invoice.setXmlFilename(null);
        invoice = update(invoice);

        File file = new File(xmlFilename);
        if (file.exists()) {
            file.delete();
        }
        return invoice;
    }

    /**
     * Check if invoice's XML file exists.
     *
     * @param invoice Invoice
     * @return True if invoice's XML file exists
     */
    public boolean isInvoiceXmlExist(Invoice invoice) {

        String xmlFileName = getFullXmlFilePath(invoice, false);
        File xmlFile = new File(xmlFileName);
        return xmlFile.exists();
    }

    /**
     * Retrieve invoice's XML file contents as a string.
     *
     * @param invoice Invoice
     * @return Invoice's XML file contents as a string
     * @throws BusinessException business exception
     */
    public String getInvoiceXml(Invoice invoice) throws BusinessException {

        if (invoice.isPrepaid()) {
            throw new BusinessException("Invoice XML is disabled for prepaid invoice: " + invoice.getInvoiceNumber());
        }

        String xmlFileName = getFullXmlFilePath(invoice, false);
        File xmlFile = new File(xmlFileName);
        if (!xmlFile.exists()) {
            throw new BusinessException("Invoice XML was not produced yet for invoice " + invoice.getInvoiceNumberOrTemporaryNumber());
        }

        Scanner scanner = null;
        try {
            scanner = new Scanner(xmlFile);
            String xmlContent = scanner.useDelimiter("\\Z").next();
            scanner.close();
            return xmlContent;
        } catch (Exception e) {
            log.error("Error reading invoice XML file {} contents", xmlFileName, e);

        } finally {
            if (scanner != null) {
                try {
                    scanner.close();
                } catch (Exception e) {
                    log.error("Error closing file scanner", e);
                }
            }

        }
        return null;
    }

    /**
     * Check if invoice's PDF file exists.
     *
     * @param invoice Invoice
     * @return True if invoice's PDF file exists
     */
    public boolean isInvoicePdfExist(Invoice invoice) {

        String pdfFileName = getFullPdfFilePath(invoice, false);
        File pdfFile = new File(pdfFileName);
        return pdfFile.exists();
    }

    /**
     * Retrieve invoice's PDF file contents as a byte array.
     *
     * @param invoice Invoice
     * @return Invoice's PDF file contents as a byte array
     * @throws BusinessException business exception
     */
    public byte[] getInvoicePdf(Invoice invoice) throws BusinessException {

        String pdfFileName = getFullPdfFilePath(invoice, false);
        File pdfFile = new File(pdfFileName);
        if (!pdfFile.exists()) {
            throw new BusinessException("Invoice PDF was not produced yet for invoice " + invoice.getInvoiceNumberOrTemporaryNumber());
        }

        FileInputStream fileInputStream = null;
        try {
            long fileSize = pdfFile.length();
            if (fileSize > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("File is too big to put it to buffer in memory.");
            }
            byte[] fileBytes = new byte[(int) fileSize];
            fileInputStream = new FileInputStream(pdfFile);
            fileInputStream.read(fileBytes);
            return fileBytes;

        } catch (Exception e) {
            log.error("Error reading invoice PDF file {} contents", pdfFileName, e);

        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    log.error("Error closing file input stream", e);
                }
            }
        }

        return null;
    }

    /**
     * Generate XML (if neeed) and PDF files for Invoice.
     *
     * @param invoice    Invoice
     * @param regenerate Regenerate XML and PDF files ignoring id they exist already
     * @return invoice
     * @throws BusinessException business exception
     */
    public Invoice generateXmlAndPdfInvoice(Invoice invoice, boolean regenerate) throws BusinessException {

        if (invoice.isPrepaid()) {
            return invoice;
        }

        if (regenerate || invoice.getXmlFilename() == null || !isInvoiceXmlExist(invoice)) {
            produceInvoiceXmlNoUpdate(invoice);
        }
        invoice = produceInvoicePdf(invoice);
        return invoice;
    }

    /**
     * Gets the linked invoice.
     *
     * @param invoice invoice used to find
     * @return linked invoice
     */
    public Invoice getLinkedInvoice(Invoice invoice) {
        if (invoice == null || invoice.getLinkedInvoices() == null || invoice.getLinkedInvoices().isEmpty()) {
            return null;
        }
        return invoice.getLinkedInvoices().iterator().next();
    }

    /**
     * Gets the invoices with account operation.
     *
     * @param billingAccount billing account
     * @return list of invoice which doesn't have account operation.
     */
    @SuppressWarnings("unchecked")
    public List<Invoice> getInvoicesWithAccountOperation(BillingAccount billingAccount) {
        try {
            QueryBuilder qb = new QueryBuilder(Invoice.class, "i");
            qb.addSql("i.recordedInvoice is not null");
            if (billingAccount != null) {
                qb.addCriterionEntity("i.billingAccount", billingAccount);
            }
            return (List<Invoice>) qb.getQuery(getEntityManager()).getResultList();
        } catch (Exception ex) {
            log.error("failed to get invoices with no account operation", ex);
        }
        return null;
    }

    /**
     * Create pending Rated transactions and generate invoice for the billingAccount. DOES assign an invoice number AND create XML/PDF files or account operation if requested.
     *
     * @param entityToInvoice           Entity to invoice
     * @param generateInvoiceRequestDto Generate invoice request
     * @param ratedTxFilter             A filter to select rated transactions
     * @param isDraft                   Is it a draft invoice
     * @return A list of generated invoices
     * @throws BusinessException General business exception
     */
    public List<Invoice> generateInvoice(IBillableEntity entityToInvoice, GenerateInvoiceRequestDto generateInvoiceRequestDto, Filter ratedTxFilter, boolean isDraft,
            CustomFieldValues customFieldValues) throws BusinessException {

        boolean produceXml =
                (generateInvoiceRequestDto.getGenerateXML() != null && generateInvoiceRequestDto.getGenerateXML()) || (generateInvoiceRequestDto.getGeneratePDF() != null
                        && generateInvoiceRequestDto.getGeneratePDF());
        boolean producePdf = (generateInvoiceRequestDto.getGeneratePDF() != null && generateInvoiceRequestDto.getGeneratePDF());
        boolean generateAO = generateInvoiceRequestDto.getGenerateAO() != null && generateInvoiceRequestDto.getGenerateAO();

        List<Invoice> invoices = invoiceService.createInvoice(entityToInvoice, generateInvoiceRequestDto, ratedTxFilter, isDraft);

        List<Invoice> invoicesWNumber = new ArrayList<Invoice>();
        for (Invoice invoice : invoices) {
            if (customFieldValues != null) {
                invoice.setCfValues(customFieldValues);
            }
            try {
                invoicesWNumber.add(serviceSingleton.assignInvoiceNumber(invoice));
            } catch (Exception e) {
                log.error("Failed to assign invoice number for invoice {}/{}", invoice.getId(), invoice.getInvoiceNumberOrTemporaryNumber(), e);
                continue;
            }
            try {
                produceFilesAndAO(produceXml, producePdf, generateAO, invoice.getId(), isDraft);
            } catch (Exception e) {
                log.error("Failed to generate XML/PDF files or recorded invoice AO for invoice {}/{}", invoice.getId(), invoice.getInvoiceNumberOrTemporaryNumber(), e);
            }
        }
        return refreshOrRetrieve(invoices);
    }

    /**
     * Create pending Rated transactions and generate invoice for the billingAccount. DOES NOT assign an invoice number NOR create XML/PDF files nor account operation. Use
     * generateInvoice() instead.
     *
     * @param entity                    Entity to invoice
     * @param generateInvoiceRequestDto Generate invoice request
     * @param ratedTxFilter             A filter to select rated transactions
     * @param isDraft                   Is it a draft invoice
     * @return A list of invoices
     * @throws BusinessException General business exception
     */
    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Invoice> createInvoice(IBillableEntity entity, GenerateInvoiceRequestDto generateInvoiceRequestDto, Filter ratedTxFilter, boolean isDraft)
            throws BusinessException {

        Date invoiceDate = generateInvoiceRequestDto.getInvoicingDate();
        Date firstTransactionDate = generateInvoiceRequestDto.getFirstTransactionDate();
        Date lastTransactionDate = generateInvoiceRequestDto.getLastTransactionDate();

        if (StringUtils.isBlank(entity)) {
            throw new BusinessException("entity is null");
        }
        if (StringUtils.isBlank(invoiceDate)) {
            throw new BusinessException("invoicingDate is null");
        }

        if (firstTransactionDate == null) {
            firstTransactionDate = new Date(0);
        }

        if (lastTransactionDate == null) {
            lastTransactionDate = invoiceDate;
        }

        if (entity.getBillingRun() != null && (entity.getBillingRun().getStatus().equals(BillingRunStatusEnum.NEW) || entity.getBillingRun().getStatus()
                .equals(BillingRunStatusEnum.PREVALIDATED) || entity.getBillingRun().getStatus().equals(BillingRunStatusEnum.POSTVALIDATED))) {

            throw new BusinessException("The entity is already in an billing run with status " + entity.getBillingRun().getStatus());
        }

        // Create missing rated transactions up to a last transaction date
        ratedTransactionService.createRatedTransaction(entity, lastTransactionDate);

        MinAmountForAccounts minAmountForAccounts = ratedTransactionService.isMinAmountForAccountsActivated();

        List<Invoice> invoices = createAgregatesAndInvoice(entity, null, ratedTxFilter, invoiceDate, firstTransactionDate, lastTransactionDate, minAmountForAccounts, isDraft);

        return invoices;
    }

    /**
     * Produce XML and PDF files and AO.
     *
     * @param produceXml To produce xml invoice file
     * @param producePdf To produce pdf invoice file
     * @param generateAO To generate Account operations
     * @param invoiceId  id of Invoice to operate on
     * @param isDraft    Is it a draft invoice
     * @throws BusinessException      General business exception
     * @throws InvoiceExistException  Invoice already exist exception
     * @throws ImportInvoiceException Import invoice exception
     */
    public void produceFilesAndAO(boolean produceXml, boolean producePdf, boolean generateAO, Long invoiceId, boolean isDraft)
            throws BusinessException, InvoiceExistException, ImportInvoiceException {

        if (produceXml) {
            invoiceService.produceInvoiceXmlInNewTransaction(invoiceId);
        }
        if (producePdf) {
            invoiceService.produceInvoicePdfInNewTransaction(invoiceId);
        }
        if (generateAO && !isDraft) {
            invoiceService.generateRecordedInvoiceAO(invoiceId);
        }
    }

    /**
     * Generate Recorded invoice account operation
     *
     * @param invoiceId Invoice identifier
     * @throws InvoiceExistException  Invoice already exists exception
     * @throws ImportInvoiceException Failed to import invoice exception
     * @throws BusinessException      General business exception
     */
    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void generateRecordedInvoiceAO(Long invoiceId) throws InvoiceExistException, ImportInvoiceException, BusinessException {

        Invoice invoice = findById(invoiceId);
        recordedInvoiceService.generateRecordedInvoice(invoice);
        invoice = update(invoice);
    }

    /**
     * Cancel invoice.
     *
     * @param invoice invoice to cancel
     * @throws BusinessException business exception
     */
    public void cancelInvoice(Invoice invoice) throws BusinessException {
        if (invoice.getRecordedInvoice() != null) {
            throw new BusinessException("Can't cancel an invoice that present in AR");
        }

        ratedTransactionService.deleteMinRTs(invoice);
        ratedTransactionService.uninvoiceRTs(invoice);

        super.remove(invoice);

        log.debug("Invoice canceled {}", invoice.getTemporaryInvoiceNumber());
    }

    /**
     * Evaluate integer expression.
     *
     * @param expression     expression as string
     * @param billingAccount billing account
     * @param invoice        which is used to evaluate
     * @param order          order related to invoice.
     * @return result of evaluation
     * @throws BusinessException business exception.
     */
    public Integer evaluateDueDelayExpression(String expression, BillingAccount billingAccount, Invoice invoice, Order order) throws BusinessException {
        Integer result = null;
        if (StringUtils.isBlank(expression)) {
            return result;
        }
        Map<Object, Object> userMap = new HashMap<Object, Object>();
        if (expression.indexOf("ba") >= 0) {
            userMap.put("ba", billingAccount);
        }
        if (expression.indexOf("invoice") >= 0) {
            userMap.put("invoice", invoice);
        }
        if (expression.indexOf("order") >= 0) {
            userMap.put("order", order);
        }
        Object res = ValueExpressionWrapper.evaluateExpression(expression, userMap, Integer.class);
        try {
            result = (Integer) res;
        } catch (Exception e) {
            throw new BusinessException("Expression " + expression + " do not evaluate to Integer but " + res);
        }
        return result;
    }

    /**
     * Evaluate billing template name.
     *
     * @param expression the expression
     * @param invoice    the invoice
     * @return the string
     */
    public String evaluateBillingTemplateName(String expression, Invoice invoice) {
        String billingTemplateName = null;

        if (!StringUtils.isBlank(expression)) {
            Map<Object, Object> contextMap = new HashMap<>();
            contextMap.put("invoice", invoice);

            try {
                String value = ValueExpressionWrapper.evaluateExpression(expression, contextMap, String.class);

                if (value != null) {
                    billingTemplateName = value;
                }
            } catch (BusinessException e) {
                // Ignore exceptions here - a default pdf filename will be used instead. Error is logged in EL evaluation
            }
        }

        billingTemplateName = StringUtils.normalizeFileName(billingTemplateName);
        return billingTemplateName;
    }

    /**
     * Determine invoice type given the following criteria
     * <p>
     * If is a prepaid invoice, default prepaid type is used.<br/>
     * If is a draft invoice, default draft type is used.<br/>
     * Otherwise invoice type is determined in the following order:<br/>
     * 1. billingCycle.invoiceTypeEl expression evaluated with billingRun and billingAccount a parameters, <br/>
     * 2. bilingCycle.invoiceType, <br/>
     * 3. Default commercial invoice type
     *
     * @param isPrepaid      Is it for prepaid invoice. If True, default prepaid type is used. Excludes other criteria.
     * @param isDraft        Is it a draft invoice. If true, default draft type is used. Excludes other criteria.
     * @param billingCycle   Billing cycle
     * @param billingRun     Billing run
     * @param billingAccount Billing account
     * @return Applicable invoice type
     * @throws BusinessException General business exception
     */
    private InvoiceType determineInvoiceType(boolean isPrepaid, boolean isDraft, BillingCycle billingCycle, BillingRun billingRun, BillingAccount billingAccount)
            throws BusinessException {
        InvoiceType invoiceType = null;

        if (isPrepaid) {
            invoiceType = invoiceTypeService.getDefaultPrepaid();

        } else if (isDraft) {
            invoiceType = invoiceTypeService.getDefaultDraft();

        } else {
            if (!StringUtils.isBlank(billingCycle.getInvoiceTypeEl())) {
                String invoiceTypeCode = evaluateInvoiceType(billingCycle.getInvoiceTypeEl(), billingRun, billingAccount);
                invoiceType = invoiceTypeService.findByCode(invoiceTypeCode);
            }
            if (invoiceType == null) {
                invoiceType = billingCycle.getInvoiceType();
            }
            if (invoiceType == null) {
                invoiceType = invoiceTypeService.getDefaultCommertial();
            }
        }

        return invoiceType;
    }

    public String evaluateInvoiceType(String expression, BillingRun billingRun, BillingAccount billingAccount) {
        String invoiceTypeCode = null;

        if (!StringUtils.isBlank(expression)) {
            Map<Object, Object> contextMap = new HashMap<>();
            contextMap.put("br", billingRun);
            contextMap.put("ba", billingAccount);

            try {
                String value = ValueExpressionWrapper.evaluateExpression(expression, contextMap, String.class);
                if (value != null) {
                    invoiceTypeCode = (String) value;
                }
            } catch (BusinessException e) {
                // Ignore exceptions here - a default pdf filename will be used instead. Error is logged in EL evaluation
            }
        }

        return invoiceTypeCode;
    }

    /**
     * Determine an invoice template to use. Rule for selecting an invoiceTemplate is: InvoiceType &gt; BillingCycle &gt; default.
     *
     * @param invoice      invoice
     * @param billingCycle Billing cycle
     * @param invoiceType  Invoice type
     * @return Invoice template name
     */
    public String getInvoiceTemplateName(Invoice invoice, BillingCycle billingCycle, InvoiceType invoiceType) {

        String billingTemplateName = "default";
        if (invoiceType != null && !StringUtils.isBlank(invoiceType.getBillingTemplateNameEL())) {
            billingTemplateName = evaluateBillingTemplateName(invoiceType.getBillingTemplateNameEL(), invoice);

        } else if (billingCycle != null && !StringUtils.isBlank(billingCycle.getBillingTemplateNameEL())) {
            billingTemplateName = evaluateBillingTemplateName(billingCycle.getBillingTemplateNameEL(), invoice);

        } else if (invoiceType != null && !StringUtils.isBlank(invoiceType.getBillingTemplateName())) {
            billingTemplateName = invoiceType.getBillingTemplateName();

        } else if (billingCycle != null && billingCycle.getInvoiceType() != null && !StringUtils.isBlank(billingCycle.getInvoiceType().getBillingTemplateName())) {
            billingTemplateName = billingCycle.getInvoiceType().getBillingTemplateName();

        } else if (billingCycle != null && !StringUtils.isBlank(billingCycle.getBillingTemplateName())) {
            billingTemplateName = billingCycle.getBillingTemplateName();
        }

        return billingTemplateName;
    }

    private Date getReferenceDate(Invoice invoice) {
        BillingRun billingRun = invoice.getBillingRun();
        Date referenceDate = new Date();
        ReferenceDateEnum referenceDateEnum = null;

        if (billingRun != null) {
            referenceDateEnum = billingRun.getReferenceDate();
        }

        if (referenceDateEnum == null && billingRun.getBillingCycle() != null) {
            referenceDateEnum = billingRun.getBillingCycle().getReferenceDate();
        }

        if (referenceDateEnum != null) {
            switch (referenceDateEnum) {
            case TODAY:
                referenceDate = new Date();
                break;
            case NEXT_INVOICE_DATE:
                referenceDate = invoice.getBillingAccount() != null ? invoice.getBillingAccount().getNextInvoiceDate() : null;
                break;
            case LAST_TRANSACTION_DATE:
                referenceDate = billingRun.getLastTransactionDate();
                break;
            case END_DATE:
                referenceDate = billingRun.getEndDate();
                break;
            default:
                break;
            }
        }
        return referenceDate;
    }

    /**
     * Assign invoice number and increment BA invoice date.
     *
     * @param invoiceId            invoice id
     * @param invoicesToNumberInfo instance of InvoicesToNumberInfo
     * @throws BusinessException business exception
     */
    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void assignInvoiceNumberAndIncrementBAInvoiceDate(Long invoiceId, InvoicesToNumberInfo invoicesToNumberInfo) throws BusinessException {

        Invoice invoice = findById(invoiceId);
        assignInvoiceNumberFromReserve(invoice, invoicesToNumberInfo);

        BillingAccount billingAccount = invoice.getBillingAccount();

        Date initCalendarDate = billingAccount.getSubscriptionDate();
        if (initCalendarDate == null) {
            initCalendarDate = billingAccount.getAuditable().getCreated();
        }

        Date nextCalendarDate = billingAccount.getBillingCycle().getNextCalendarDate(initCalendarDate, getReferenceDate(invoice));
        billingAccount.setNextInvoiceDate(nextCalendarDate);
        billingAccount.updateAudit(currentUser);
        invoice = update(invoice);
    }

    /**
     * Get a list of invoice identifiers that belong to a given Billing run and that do not have XML generated yet.
     *
     * @param billingRunId Billing run id
     * @return A list of invoice identifiers
     */
    public List<Long> getInvoiceIdsByBRWithNoXml(Long billingRunId) {
        if (billingRunId == null) {
            return getEntityManager().createNamedQuery("Invoice.validatedNoXml", Long.class).getResultList();
        }
        return getEntityManager().createNamedQuery("Invoice.validatedByBRNoXml", Long.class).setParameter("billingRunId", billingRunId).getResultList();
    }

    /**
     * Get list of Draft invoice Ids that belong to the given Billing Run and not having XML generated yet.
     *
     * @param billingRunId Billing run id
     * @return A list of invoice identifiers
     */
    public List<Long> getDraftInvoiceIdsByBRWithNoXml(Long billingRunId) {
        if (billingRunId == null) {
            return getEntityManager().createNamedQuery("Invoice.draftNoXml", Long.class).getResultList();
        }
        return getEntityManager().createNamedQuery("Invoice.draftByBRNoXml", Long.class).setParameter("billingRunId", billingRunId).getResultList();
    }

    /**
     * Get list of Draft and validated invoice Ids that belong to the given Billing Run and not having XML generated yet.
     *
     * @param billingRunId Billing run id
     * @return A list of invoice identifiers
     */
    public List<Long> getInvoiceIdsIncludeDraftByBRWithNoXml(Long billingRunId) {
        if (billingRunId == null) {
            return getEntityManager().createNamedQuery("Invoice.allNoXml", Long.class).getResultList();
        }
        return getEntityManager().createNamedQuery("Invoice.allByBRNoXml", Long.class).setParameter("billingRunId", billingRunId).getResultList();
    }

    /**
     * Get a summarized information for invoice numbering. Contains grouping by invoice type, seller, invoice date and a number of invoices.
     *
     * @param billingRunId Billing run id
     * @return A list of invoice identifiers
     */
    @SuppressWarnings("unchecked")
    public List<InvoicesToNumberInfo> getInvoicesToNumberSummary(Long billingRunId) {

        List<InvoicesToNumberInfo> invoiceSummaries = new ArrayList<>();
        List<Object[]> summary = getEntityManager().createNamedQuery("Invoice.invoicesToNumberSummary").setParameter("billingRunId", billingRunId).getResultList();

        for (Object[] summaryInfo : summary) {
            invoiceSummaries.add(new InvoicesToNumberInfo((Long) summaryInfo[0], (Long) summaryInfo[1], (Date) summaryInfo[2], (Long) summaryInfo[3]));
        }

        return invoiceSummaries;
    }

    /**
     * Retrieve invoice ids matching billing run, invoice type, seller and invoice date combination.
     *
     * @param billingRunId  Billing run id
     * @param invoiceTypeId Invoice type id
     * @param sellerId      Seller id
     * @param invoiceDate   Invoice date
     * @return A list of invoice identifiers
     */
    public List<Long> getInvoiceIds(Long billingRunId, Long invoiceTypeId, Long sellerId, Date invoiceDate) {
        return getEntityManager().createNamedQuery("Invoice.byBrItSelDate", Long.class).setParameter("billingRunId", billingRunId).setParameter("invoiceTypeId", invoiceTypeId)
                .setParameter("sellerId", sellerId).setParameter("invoiceDate", invoiceDate).getResultList();
    }

    /**
     * List by invoice.
     *
     * @param invoice invoice used to get subcategory
     * @return list of SubCategoryInvoiceAgregate
     */
    @SuppressWarnings("unchecked")
    public List<SubCategoryInvoiceAgregate> listByInvoice(Invoice invoice) {
        QueryBuilder qb = new QueryBuilder(SubCategoryInvoiceAgregate.class, "c");
        qb.addCriterionEntity("invoice", invoice);
        qb.addBooleanCriterion("discountAggregate", false);

        try {
            List<SubCategoryInvoiceAgregate> resultList = (List<SubCategoryInvoiceAgregate>) qb.getQuery(getEntityManager()).getResultList();
            return resultList;
        } catch (NoResultException e) {
            log.warn("error while getting user account list by billing account", e);
            return null;
        }
    }

    public List<String> listPdfInvoice(Customer cust) {
        List<String> result = new ArrayList<>();
        if (cust.getCustomerAccounts() != null && !cust.getCustomerAccounts().isEmpty()) {
            for (CustomerAccount ca : cust.getCustomerAccounts()) {
                if (ca.getBillingAccounts() != null && !ca.getBillingAccounts().isEmpty()) {
                    for (BillingAccount ba : ca.getBillingAccounts()) {
                        if (ba.getInvoices() != null && !ba.getInvoices().isEmpty()) {
                            for (Invoice inv : ba.getInvoices()) {
                                result.add(getFullPdfFilePath(inv, false));
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Update unpaid invoices status
     */
    public void updateUnpaidInvoicesStatus() {
        getEntityManager().createNamedQuery("Invoice.updateUnpaidInvoicesStatus").executeUpdate();
    }

    /**
     * Return all invoices with now - invoiceDate date &gt; n years.
     *
     * @param nYear age of the invoices
     * @return Filtered list of invoices
     */
    @SuppressWarnings("unchecked")
    public List<Invoice> listInactiveInvoice(int nYear) {
        QueryBuilder qb = new QueryBuilder(Invoice.class, "e");
        Date higherBound = DateUtils.addYearsToDate(new Date(), -1 * nYear);

        qb.addCriterionDateRangeToTruncatedToDay("invoiceDate", higherBound);

        return (List<Invoice>) qb.getQuery(getEntityManager()).getResultList();
    }

    public void bulkDelete(List<Invoice> inactiveInvoices) throws BusinessException {
        for (Invoice e : inactiveInvoices) {
            remove(e);
        }
    }

    /**
     * Nullify BR's invoices file names (xml and pdf).
     *
     * @param billingRun the billing run
     */
    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void nullifyInvoiceFileNames(BillingRun billingRun) {
        getEntityManager().createNamedQuery("Invoice.nullifyInvoiceFileNames").setParameter("billingRun", billingRun).executeUpdate();
    }

    /**
     * A first part of invoiceService.create() method. Does not call PersistenceService.create(), Need to call InvoiceService.postCreate() separately
     *
     * @param invoice Invoice entity
     * @throws BusinessException General business exception
     */
    @Override
    public void create(Invoice invoice) throws BusinessException {

        invoice.updateAudit(currentUser);

        // Schedule end of period events
        // Be careful - if called after persistence might loose ability to determine new period as CustomFeldvalue.isNewPeriod is not serialized to json
        if (invoice instanceof ICustomFieldEntity) {
            customFieldInstanceService.scheduleEndPeriodEvents((ICustomFieldEntity) invoice);
        }
        // activate/deactivate sending invoice by Emails
        if (!isElectronicBillingEnabled(invoice)) {
            invoice.setDontSend(true);
        }

        getEntityManager().persist(invoice);

        log.trace("end of create {}. entity id={}.", invoice.getClass().getSimpleName(), invoice.getId());
    }

    /**
     * A second part of invoiceService.create() method.
     *
     * @param invoice Invoice entity
     * @throws BusinessException General business exception
     */
    public void postCreate(Invoice invoice) throws BusinessException {

        entityCreatedEventProducer.fire((BaseEntity) invoice);

        cfValueAccumulator.entityCreated(invoice);

        log.trace("end of post create {}. entity id={}.", invoice.getClass().getSimpleName(), invoice.getId());
    }

    /**
     * Send the invoice by email
     *
     * @param invoice         the invoice
     * @param mailingTypeEnum : Mailing type
     * @param overrideEmail   : override Email
     * @return true if the invoice is sent, false else.
     * @throws BusinessException
     */
    public boolean sendByEmail(Invoice invoice, MailingTypeEnum mailingTypeEnum, String overrideEmail) throws BusinessException {
        try {
            if (invoice == null) {
                log.error("The invoice to be sent by Email is null!!");
                return false;
            }
            invoice = refreshOrRetrieve(invoice);
            if (invoice.getPdfFilename() == null) {
                log.warn("The Pdf for the invoice is not generated!!");
                return false;
            }
            List<String> to = new ArrayList<>();
            List<String> cc = new ArrayList<>();
            List<File> files = new ArrayList<>();

            String fileName = getFullPdfFilePath(invoice, false);
            File attachment = new File(fileName);
            if (!attachment.exists()) {
                log.warn("No Pdf file exists for the invoice " + invoice.getInvoiceNumber());
                return false;
            }
            files.add(attachment);
            EmailTemplate emailTemplate = invoice.getInvoiceType().getEmailTemplate();
            MailingTypeEnum mailingType = invoice.getInvoiceType().getMailingType();
            BillingAccount billingAccount = invoice.getBillingAccount();
            Seller seller = invoice.getSeller();
            if (billingAccount.getContactInformation() != null) {
                to.add(billingAccount.getContactInformation().getEmail());
            }
            if (billingAccount.getCcedEmails() != null) {
                cc.addAll(Arrays.asList(billingAccount.getCcedEmails().split(",")));
            }
            if (billingAccount.getEmailTemplate() != null) {
                emailTemplate = billingAccount.getEmailTemplate();
            }
            if (billingAccount.getMailingType() != null) {
                mailingType = billingAccount.getMailingType();
            }

            Boolean electronicBilling = billingAccount.getElectronicBilling();
            Subscription subscription = invoice.getSubscription();
            if (subscription != null) {
                electronicBilling = subscription.getElectronicBilling();
                seller = (subscription.getSeller() != null) ? subscription.getSeller() : seller;
                to.clear();
                to.add(subscription.getEmail());
                cc.clear();
                if (subscription.getCcedEmails() != null) {
                    cc.addAll(Arrays.asList(subscription.getCcedEmails().split(",")));
                }
                emailTemplate = (subscription.getEmailTemplate() != null) ? subscription.getEmailTemplate() : emailTemplate;
                mailingType = (subscription.getMailingType() != null) ? subscription.getMailingType() : mailingType;

            }
            Order order = invoice.getOrder();
            if (order != null) {
                electronicBilling = order.getElectronicBilling();
                to.clear();
                to.add(order.getEmail());
                cc.clear();
                if (order.getCcedEmails() != null) {
                    cc.addAll(Arrays.asList(order.getCcedEmails().split(",")));
                }
                emailTemplate = (order.getEmailTemplate() != null) ? order.getEmailTemplate() : emailTemplate;
                mailingType = (order.getMailingType() != null) ? order.getMailingType() : mailingType;
            }
            if (overrideEmail != null) {
                to.clear();
                to.add(overrideEmail);
                cc.clear();
            }
            if (to.isEmpty() || emailTemplate == null) {
                log.warn("No Email or  EmailTemplate is configured to receive the invoice!!");
                return false;
            }
            if (seller == null || seller.getContactInformation() == null) {
                log.warn("The Seller or it's contact information is null!!");
                return false;
            }
            if (electronicBilling && mailingTypeEnum.equals(mailingType)) {
                Map<Object, Object> params = new HashMap<>();
                params.put("invoice", invoice);
                String subject = ValueExpressionWrapper.evaluateExpression(emailTemplate.getSubject(), params, String.class);
                String content = ValueExpressionWrapper.evaluateExpression(emailTemplate.getTextContent(), params, String.class);
                String contentHtml = ValueExpressionWrapper.evaluateExpression(emailTemplate.getHtmlContent(), params, String.class);
                emailSender.send(seller.getContactInformation().getEmail(), to, to, cc, null, subject, content, contentHtml, files, null);
                invoice.setStatus(InvoiceStatusEnum.SENT);
                invoice.setAlreadySent(true);
                update(invoice);

                return true;
            }
            return false;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BusinessException(e.getMessage(), e);
        }
    }

    /**
     * Return a list of invoices that not already sent and can be sent : dontsend:false.
     *
     * @return a list of invoices
     * @throws BusinessException
     */
    @SuppressWarnings("unchecked")
    public List<Invoice> findByNotAlreadySentAndDontSend() throws BusinessException {
        List<Invoice> result = new ArrayList<Invoice>();
        QueryBuilder qb = new QueryBuilder(Invoice.class, "i", null);
        qb.addCriterionEntity("alreadySent", false);
        qb.addCriterionEntity("dontSend", false);
        try {
            result = (List<Invoice>) qb.getQuery(getEntityManager()).getResultList();
        } catch (NoResultException e) {
            throw new BusinessException(e.getMessage(), e);
        }
        return result;
    }

    /**
     * Check if an invoice is draft.
     *
     * @param invoice the invoice
     * @return true if is draft else return false.
     * @throws BusinessException
     */
    public boolean isDraft(Invoice invoice) throws BusinessException {
        invoice = refreshOrRetrieve(invoice);
        InvoiceType invoiceType = invoice.getInvoiceType();
        InvoiceType draftInvoiceType = invoiceTypeService.getDefaultDraft();
        return invoiceType != null && (invoiceType.equals(draftInvoiceType) || invoice.getInvoiceNumber() == null);
    }

    /**
     * Evaluate the override Email EL
     *
     * @param overrideEmailEl override Email
     * @param userMap         the userMap
     * @param invoice         the invoice
     * @return the
     * @throws BusinessException
     */
    public String evaluateOverrideEmail(String overrideEmailEl, HashMap<Object, Object> userMap, Invoice invoice) throws BusinessException {
        invoice = refreshOrRetrieve(invoice);
        userMap.put("invoice", invoice);
        String result = ValueExpressionWrapper.evaluateExpression(overrideEmailEl, userMap, String.class);
        if (StringUtils.isBlank(result)) {
            return null;
        }
        return result;
    }

    /**
     * Append invoice aggregates to an invoice. Retrieves all to-invoice Rated transactions for a given billing account
     *
     * @param billingAccount       Billing Account
     * @param invoice              Invoice to append invoice aggregates to
     * @param firstTransactionDate First transaction date
     * @param lastTransactionDate  Last transaction date
     * @throws BusinessException business exception
     */
    public void appendInvoiceAgregates(BillingAccount billingAccount, Invoice invoice, Date firstTransactionDate, Date lastTransactionDate) throws BusinessException {

        if (firstTransactionDate == null) {
            firstTransactionDate = new Date(0);
        }

        if (lastTransactionDate == null) {
            lastTransactionDate = new Date();
        }

        List<RatedTransaction> ratedTransactions = getEntityManager().createNamedQuery("RatedTransaction.listToInvoiceByBillingAccount", RatedTransaction.class)
                .setParameter("billingAccount", billingAccount).setParameter("firstTransactionDate", firstTransactionDate).setParameter("lastTransactionDate", lastTransactionDate)
                .getResultList();

        appendInvoiceAgregates(billingAccount, billingAccount, invoice, ratedTransactions, false, null, false);
    }

    /**
     * Creates Invoice aggregates from given Rated transactions and appends them to an invoice
     *
     * @param entityToInvoice                Entity to invoice
     * @param billingAccount                 Billing Account
     * @param invoice                        Invoice to append invoice aggregates to
     * @param ratedTransactions              A list of rated transactions
     * @param isInvoiceAdjustment            Is this invoice adjustment
     * @param invoiceAggregateProcessingInfo RT to invoice aggregation information when invoice is created with paged RT retrieval. NOTE: should pass NULL in non-paginated
     *                                       invoicing cases
     * @param subCategoryAggregates          Subcategory aggregates for invoice mapped by a key
     * @param moreRatedTransactionsExpected  Indicates that there are more RTs to be retrieved and aggregated in invoice before invoice can be closed. NOTE: should pass FALSE in
     *                                       non-paginated invoicing cases
     * @throws BusinessException BusinessException
     */
    private void appendInvoiceAgregates(IBillableEntity entityToInvoice, BillingAccount billingAccount, Invoice invoice, List<RatedTransaction> ratedTransactions,
            boolean isInvoiceAdjustment, InvoiceAggregateProcessingInfo invoiceAggregateProcessingInfo, boolean moreRatedTransactionsExpected) throws BusinessException {

        boolean isEnterprise = appProvider.isEntreprise();
        String languageCode = billingAccount.getTradingLanguage().getLanguageCode();
        Boolean isExonerated = billingAccount.isExoneratedFromtaxes();
        if (isExonerated == null) {
            isExonerated = billingAccountService.isExonerated(billingAccount);
        }
        int rtRounding = appProvider.getRounding();
        int invoiceRounding = appProvider.getInvoiceRounding();
        RoundingModeEnum rtRoundingMode = appProvider.getRoundingMode();
        RoundingModeEnum invoiceRoundingMode = appProvider.getInvoiceRoundingMode();
        Tax taxZero = isExonerated ? taxService.getZeroTax() : null;

        // InvoiceType.taxScript will calculate all tax aggregates at once.
        boolean calculateTaxOnSubCategoryLevel = invoice.getInvoiceType().getTaxScript() == null;

        // Should tax calculation on subcategory level be done externally
        boolean calculateExternalTax = "YES".equalsIgnoreCase((String) appProvider.getCfValue("OPENCELL_ENABLE_TAX_CALCULATION"));

        // Tax change mapping. Key is ba.id_seller.id_invoiceType.id_ua.id_walletInstance.id_invoiceSubCategory.id_tax.id and value is an array of [Tax to apply, True/false if tax
        // has changed]
        Map<String, Object[]> taxChangeMap = invoiceAggregateProcessingInfo != null ? invoiceAggregateProcessingInfo.taxChangeMap : new HashMap<>();

        // Subcategory aggregates mapping. Key is ua.id_walletInstance.id_invoiceSubCategory.id_tax.id
        Map<String, SubCategoryInvoiceAgregate> subCategoryAggregates =
                invoiceAggregateProcessingInfo != null ? invoiceAggregateProcessingInfo.subCategoryAggregates : new HashMap<>();

        Set<String> orderNumbers = invoiceAggregateProcessingInfo != null ? invoiceAggregateProcessingInfo.orderNumbers : new HashSet<String>();

        String scaKey = null;
        String scaKeyWithoutTax = null;

        if (log.isTraceEnabled()) {
            log.trace("ratedTransactions.totalAmountWithoutTax={}",
                    ratedTransactions != null ? ratedTransactions.stream().mapToDouble(e -> e.getAmountWithoutTax().doubleValue()).sum() : "0");
        }

        BillingRun billingRun = invoice.getBillingRun();
        boolean taxWasRecalculated = false;
        for (RatedTransaction ratedTransaction : ratedTransactions) {

            InvoiceSubCategory invoiceSubCategory = ratedTransaction.getInvoiceSubCategory();

            scaKeyWithoutTax = (ratedTransaction.getUserAccount() != null ? ratedTransaction.getUserAccount().getId() : "") + "_" + (ratedTransaction.getWallet() != null ?
                    ratedTransaction.getWallet().getId() :
                    "") + "_" + invoiceSubCategory.getId() + "_";

            Tax tax = ratedTransaction.getTax();
            scaKey = scaKeyWithoutTax + (calculateTaxOnSubCategoryLevel ? tax.getId() : "");

            // Check if tax has to be recalculated. Does not apply to RatedTransactions that had tax explicitly set/overriden
            if (calculateTaxOnSubCategoryLevel && !ratedTransaction.isTaxOverriden()) {
                Object[] changedToTax = taxChangeMap.get(scaKey);
                if (changedToTax == null) {
                    taxZero = isExonerated && taxZero == null ? taxService.getZeroTax() : taxZero;
                    Object[] applicableTax = getApplicableTax(tax, isExonerated, invoice, invoiceSubCategory,
                            ratedTransaction.getUserAccount() != null ? ratedTransaction.getUserAccount().getId() : null, taxZero, calculateExternalTax);
                    changedToTax = applicableTax;
                    taxChangeMap.put(scaKey, changedToTax);
                    if ((boolean) changedToTax[1]) {
                        log.debug("Will update rated transactions in subcategory {} with new tax from {} to {}", invoiceSubCategory.getCode(), tax.getPercent(),
                                ((Tax) changedToTax[0]).getPercent());
                    }
                }
                taxWasRecalculated = (boolean) changedToTax[1];
                if (taxWasRecalculated) {
                    tax = (Tax) changedToTax[0];
                    ratedTransaction.setTaxRecalculated(true);
                    scaKey = scaKeyWithoutTax + "_" + tax.getId();
                }
            }

            SubCategoryInvoiceAgregate scAggregate = subCategoryAggregates.get(scaKey);
            if (scAggregate == null) {
                scAggregate = new SubCategoryInvoiceAgregate(invoiceSubCategory, billingAccount, ratedTransaction.getUserAccount(), ratedTransaction.getWallet(),
                        calculateTaxOnSubCategoryLevel ? tax : null, invoice, invoiceSubCategory.getAccountingCode());
                scAggregate.updateAudit(currentUser);

                String translationSCKey = "SC_" + invoiceSubCategory.getId() + "_" + languageCode;
                String descTranslated = descriptionMap.get(translationSCKey);
                if (descTranslated == null) {
                    descTranslated = invoiceSubCategory.getDescriptionOrCode();
                    if ((invoiceSubCategory.getDescriptionI18n() != null) && (invoiceSubCategory.getDescriptionI18n().get(languageCode) != null)) {
                        descTranslated = invoiceSubCategory.getDescriptionI18n().get(languageCode);
                    }
                    descriptionMap.put(translationSCKey, descTranslated);
                }
                scAggregate.setDescription(descTranslated);

                subCategoryAggregates.put(scaKey, scAggregate);
                invoice.addInvoiceAggregate(scAggregate);
            }

            if (isEnterprise) {
                scAggregate.addAmountWithoutTax(ratedTransaction.getAmountWithoutTax());
            } else {
                scAggregate.addAmountWithTax(ratedTransaction.getAmountWithTax());
            }

            scAggregate.addRatedTransaction(ratedTransaction);

            if (!(entityToInvoice instanceof Order) && ratedTransaction.getOrderNumber() != null) {
                orderNumbers.add(ratedTransaction.getOrderNumber());
            }

            if (taxWasRecalculated) {
                ratedTransaction.setTax(scAggregate.getTax());
                ratedTransaction.setTaxPercent(scAggregate.getTaxPercent());
                ratedTransaction.computeDerivedAmounts(isEnterprise, rtRounding, rtRoundingMode);
            }
        }

        // Postpone other aggregate calculation until the last RT is aggregated to invoice
        if (moreRatedTransactionsExpected) {
            return;
        }

        addDiscountCategoryAndTaxAggregates(invoice, subCategoryAggregates.values());
    }

    private void addDiscountCategoryAndTaxAggregates(Invoice invoice, Collection<SubCategoryInvoiceAgregate> subCategoryAggregates) throws BusinessException {

        Subscription subscription = invoice.getSubscription();
        BillingAccount billingAccount = invoice.getBillingAccount();
        CustomerAccount customerAccount = billingAccount.getCustomerAccount();

        boolean isEnterprise = appProvider.isEntreprise();
        String languageCode = billingAccount.getTradingLanguage().getLanguageCode();
        int invoiceRounding = appProvider.getInvoiceRounding();
        RoundingModeEnum invoiceRoundingMode = appProvider.getInvoiceRoundingMode();
        // InvoiceType.taxScript will calculate all tax aggregates at once.

        Boolean isExonerated = billingAccount.isExoneratedFromtaxes();
        if (isExonerated == null) {
            isExonerated = billingAccountService.isExonerated(billingAccount);
        }
        boolean calculateTaxOnSubCategoryLevel = invoice.getInvoiceType().getTaxScript() == null;

        // Determine which discount plan items apply to this invoice
        List<DiscountPlanItem> subscriptionApplicableDiscountPlanItems = new ArrayList<>();
        List<DiscountPlanItem> billingAccountApplicableDiscountPlanItems = new ArrayList<>();

        if (subscription != null && subscription.getDiscountPlanInstances() != null && !subscription.getDiscountPlanInstances().isEmpty()) {
            subscriptionApplicableDiscountPlanItems.addAll(getApplicableDiscountPlanItems(billingAccount, subscription.getDiscountPlanInstances(), invoice, customerAccount));
        }
        if (billingAccount.getDiscountPlanInstances() != null && !billingAccount.getDiscountPlanInstances().isEmpty()) {
            billingAccountApplicableDiscountPlanItems.addAll(getApplicableDiscountPlanItems(billingAccount, billingAccount.getDiscountPlanInstances(), invoice, customerAccount));
        }

        if (log.isTraceEnabled()) {
            log.trace("subCategoryAggregates.total={}",
                    subCategoryAggregates != null ? subCategoryAggregates.stream().mapToDouble(e -> e.getAmountWithoutTax().doubleValue()).sum() : "0");
        }
        // Calculate derived aggregate amounts for subcategory aggregate, create category aggregates, discount aggregates and tax aggregates
        BigDecimal[] amounts = null;
        Map<String, CategoryInvoiceAgregate> categoryAggregates = new HashMap<>();
        List<SubCategoryInvoiceAgregate> discountAggregates = new ArrayList<>();
        Map<String, TaxInvoiceAgregate> taxAggregates = new HashMap<>();

        for (SubCategoryInvoiceAgregate scAggregate : subCategoryAggregates) {

            // Calculate derived amounts
            scAggregate.computeDerivedAmounts(isEnterprise, invoiceRounding, invoiceRoundingMode.getRoundingMode());

            InvoiceSubCategory invoiceSubCategory = scAggregate.getInvoiceSubCategory();

            // Create category aggregates or update their amounts

            String caKey = (scAggregate.getUserAccount() != null ? scAggregate.getUserAccount().getId() : "") + "_" + invoiceSubCategory.getInvoiceCategory().getId();

            CategoryInvoiceAgregate cAggregate = categoryAggregates.get(caKey);
            if (cAggregate == null) {
                cAggregate = new CategoryInvoiceAgregate(invoiceSubCategory.getInvoiceCategory(), billingAccount, scAggregate.getUserAccount(), invoice);
                categoryAggregates.put(caKey, cAggregate);

                cAggregate.updateAudit(currentUser);

                String translationCKey = "C_" + invoiceSubCategory.getInvoiceCategory().getId() + "_" + languageCode;
                String descTranslated = descriptionMap.get(translationCKey);
                if (descTranslated == null) {
                    descTranslated = invoiceSubCategory.getInvoiceCategory().getDescriptionOrCode();
                    if ((invoiceSubCategory.getInvoiceCategory().getDescriptionI18n() != null) && (invoiceSubCategory.getInvoiceCategory().getDescriptionI18n().get(languageCode)
                            != null)) {
                        descTranslated = invoiceSubCategory.getInvoiceCategory().getDescriptionI18n().get(languageCode);
                    }
                    descriptionMap.put(translationCKey, descTranslated);
                }

                cAggregate.setDescription(descTranslated);

                invoice.addInvoiceAggregate(cAggregate);
            }

            cAggregate.addSubCategoryInvoiceAggregate(scAggregate);

            BigDecimal amountCumulativeForTax = isEnterprise ? scAggregate.getAmountWithoutTax() : scAggregate.getAmountWithTax();

            if ((amountCumulativeForTax != null) && !BigDecimal.ZERO.equals(amountCumulativeForTax)) {

                BigDecimal amountAsDiscountBase = amountCumulativeForTax;

                // Add discount aggregates
                for (DiscountPlanItem discountPlanItem : subscriptionApplicableDiscountPlanItems) {
                    SubCategoryInvoiceAgregate discountAggregate = getDiscountAggregates(billingAccount, invoice, isEnterprise, invoiceRounding, invoiceRoundingMode, scAggregate,
                            amountCumulativeForTax, cAggregate, discountPlanItem);
                    if (discountAggregate != null) {
                        amountCumulativeForTax = amountCumulativeForTax.add(isEnterprise ? discountAggregate.getAmountWithoutTax() : discountAggregate.getAmountWithTax());
                    }
                    discountAggregates.add(discountAggregate);
                }

                amountAsDiscountBase = amountCumulativeForTax;
                for (DiscountPlanItem discountPlanItem : billingAccountApplicableDiscountPlanItems) {
                    SubCategoryInvoiceAgregate discountAggregate = getDiscountAggregates(billingAccount, invoice, isEnterprise, invoiceRounding, invoiceRoundingMode, scAggregate,
                            amountAsDiscountBase, cAggregate, discountPlanItem);
                    if (discountAggregate != null) {
                        amountCumulativeForTax = amountCumulativeForTax.add(isEnterprise ? discountAggregate.getAmountWithoutTax() : discountAggregate.getAmountWithTax());
                    }
                    discountAggregates.add(discountAggregate);
                }

                // Add tax aggregate or update its amounts

                if (calculateTaxOnSubCategoryLevel && !isExonerated && !BigDecimal.ZERO.equals(amountCumulativeForTax)) {

                    TaxInvoiceAgregate taxAggregate = taxAggregates.get(scAggregate.getTax().getCode());
                    if (taxAggregate == null) {
                        taxAggregate = new TaxInvoiceAgregate(billingAccount, scAggregate.getTax(), scAggregate.getTaxPercent(), invoice);
                        taxAggregate.updateAudit(currentUser);
                        taxAggregates.put(scAggregate.getTax().getCode(), taxAggregate);

                        String translationCKey = "T_" + scAggregate.getTax().getId() + "_" + languageCode;
                        String descTranslated = descriptionMap.get(translationCKey);
                        if (descTranslated == null) {
                            descTranslated = scAggregate.getTax().getDescriptionOrCode();
                            if ((scAggregate.getTax().getDescriptionI18n() != null) && (scAggregate.getTax().getDescriptionI18n().get(languageCode) != null)) {
                                descTranslated = scAggregate.getTax().getDescriptionI18n().get(languageCode);
                            }
                            descriptionMap.put(translationCKey, descTranslated);
                        }

                        taxAggregate.setDescription(descTranslated);

                        invoice.addInvoiceAggregate(taxAggregate);
                    }

                    if (isEnterprise) {
                        taxAggregate.addAmountWithoutTax(amountCumulativeForTax);

                    } else {
                        taxAggregate.addAmountWithTax(amountCumulativeForTax);
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("taxAggregate.currentTotal={}",
                                taxAggregates != null ? taxAggregates.values().stream().mapToDouble(e -> e.getAmountWithoutTax().doubleValue()).sum() : "0");
                    }
                }
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("taxAggregate.grantTotal={}", taxAggregates != null ? taxAggregates.values().stream().mapToDouble(e -> e.getAmountWithoutTax().doubleValue()).sum() : "0");
        }

        // Calculate derived tax aggregate amounts
        if (calculateTaxOnSubCategoryLevel && !isExonerated) {
            for (TaxInvoiceAgregate taxAggregate : taxAggregates.values()) {

                amounts = NumberUtils
                        .computeDerivedAmounts(taxAggregate.getAmountWithoutTax(), taxAggregate.getAmountWithTax(), taxAggregate.getTaxPercent(), isEnterprise, invoiceRounding,
                                invoiceRoundingMode.getRoundingMode());
                taxAggregate.setAmountWithoutTax(amounts[0]);
                taxAggregate.setAmountWithTax(amounts[1]);
                taxAggregate.setAmountTax(amounts[2]);

            }
        }

        // If tax calculation is not done at subcategory level, then call a global script to do calculation for the whole invoice
        if (!isExonerated && !calculateTaxOnSubCategoryLevel) {
            if ((invoice.getInvoiceType() != null) && (invoice.getInvoiceType().getTaxScript() != null)) {
                taxAggregates = taxScriptService.createTaxAggregates(invoice.getInvoiceType().getTaxScript().getCode(), invoice);
                if (taxAggregates != null) {
                    for (TaxInvoiceAgregate taxAggregate : taxAggregates.values()) {
                        taxAggregate.setInvoice(invoice);
                        invoice.addInvoiceAggregate(taxAggregate);
                    }
                }
            }
        }

        // Calculate invoice total amounts by the sum of tax aggregates or category aggregates minus discount aggregates
        // Left here in case tax script modifies something
        if (!isExonerated && (taxAggregates != null) && !taxAggregates.isEmpty()) {
            for (TaxInvoiceAgregate taxAggregate : taxAggregates.values()) {
                invoice.addAmountWithoutTax(taxAggregate.getAmountWithoutTax());
                invoice.addAmountWithTax(taxAggregate.getAmountWithTax());
                invoice.addAmountTax(taxAggregate.getAmountTax());
            }

        } else {

            for (CategoryInvoiceAgregate cAggregate : categoryAggregates.values()) {
                invoice.addAmountWithoutTax(cAggregate.getAmountWithoutTax());
                invoice.addAmountWithTax(cAggregate.getAmountWithTax());
                invoice.addAmountTax(isExonerated ? BigDecimal.ZERO : cAggregate.getAmountTax());
            }

            for (SubCategoryInvoiceAgregate discountAggregate : discountAggregates) {
                invoice.addAmountWithoutTax(discountAggregate.getAmountWithoutTax());
                invoice.addAmountWithTax(discountAggregate.getAmountWithTax());
                invoice.addAmountTax(isExonerated ? BigDecimal.ZERO : discountAggregate.getAmountTax());
            }
        }

        // If invoice is prepaid, skip threshold test
        if (!invoice.isPrepaid()) {
            BigDecimal invoicingThreshold =
                    billingAccount.getInvoicingThreshold() == null ? billingAccount.getBillingCycle().getInvoicingThreshold() : billingAccount.getInvoicingThreshold();
            if ((invoicingThreshold != null) && (invoicingThreshold.compareTo(isEnterprise ? invoice.getAmountWithoutTax() : invoice.getAmountWithTax()) > 0)) {
                throw new BusinessException("Invoice amount below the threshold");
            }
        }

        // Update net to pay amount
        invoice.setNetToPay(invoice.getAmountWithTax().add(invoice.getDueBalance() != null ? invoice.getDueBalance() : BigDecimal.ZERO));
    }

    private SubCategoryInvoiceAgregate getDiscountAggregates(BillingAccount billingAccount, Invoice invoice, boolean isEnterprise, int invoiceRounding,
            RoundingModeEnum invoiceRoundingMode, SubCategoryInvoiceAgregate scAggregate, BigDecimal amount, CategoryInvoiceAgregate cAggregate, DiscountPlanItem discountPlanItem)
            throws BusinessException {
        BigDecimal[] amounts;// Apply discount if matches the category, subcategory, or applies to any category
        SubCategoryInvoiceAgregate discountAggregate;
        if ((discountPlanItem.getInvoiceCategory() == null && discountPlanItem.getInvoiceSubCategory() == null) || (discountPlanItem.getInvoiceSubCategory() != null
                && discountPlanItem.getInvoiceSubCategory().getId().equals(scAggregate.getInvoiceSubCategory().getId())) || (discountPlanItem.getInvoiceCategory() != null
                && discountPlanItem.getInvoiceSubCategory() == null && discountPlanItem.getInvoiceCategory().getId()
                .equals(scAggregate.getInvoiceSubCategory().getInvoiceCategory().getId()))) {

            BigDecimal discountValue = getDiscountValue(invoice, scAggregate, amount, discountPlanItem);

            BigDecimal discountAmount = null;

            if (discountValue != null) {
                if (discountPlanItem.getDiscountPlanItemType().equals(DiscountPlanItemTypeEnum.PERCENTAGE)) {
                    discountAmount = amount.multiply(discountValue.divide(HUNDRED)).setScale(invoiceRounding, invoiceRoundingMode.getRoundingMode());
                } else {
                    discountAmount = discountValue.negate().setScale(invoiceRounding, invoiceRoundingMode.getRoundingMode());
                }
            }
            if (discountAmount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                discountAmount = discountAmount.negate();
            }

            if (discountAmount != null && discountAmount.abs().negate().compareTo(BigDecimal.ZERO) < 0) {
                discountAggregate = new SubCategoryInvoiceAgregate(scAggregate.getInvoiceSubCategory(), billingAccount, scAggregate.getUserAccount(), scAggregate.getWallet(),
                        scAggregate.getTax(), invoice, null);

                discountAggregate.updateAudit(currentUser);
                discountAggregate.setItemNumber(scAggregate.getItemNumber());
                discountAggregate.setCategoryInvoiceAgregate(cAggregate);

                discountAggregate.setDiscountAggregate(true);
                if (discountPlanItem.getDiscountPlanItemType().equals(DiscountPlanItemTypeEnum.PERCENTAGE)) {
                    discountAggregate.setDiscountPercent(discountValue);
                }
                discountAggregate.setDiscountPlanItem(discountPlanItem);
                discountAggregate.setDescription(discountPlanItem.getCode());

                amounts = NumberUtils
                        .computeDerivedAmounts(discountAmount, discountAmount, scAggregate.getTaxPercent(), isEnterprise, invoiceRounding, invoiceRoundingMode.getRoundingMode());

                discountAggregate.setAmountWithoutTax(amounts[0]);
                discountAggregate.setAmountWithTax(amounts[1]);
                discountAggregate.setAmountTax(amounts[2]);

                invoice.addInvoiceAggregate(discountAggregate);
                return discountAggregate;
            }
        }
        return null;
    }

    private BigDecimal getDiscountValue(Invoice invoice, SubCategoryInvoiceAgregate scAggregate, BigDecimal amount, DiscountPlanItem discountPlanItem) {
        BigDecimal discountValue = discountPlanItem.getDiscountValue();

        final String dpValueEL = discountPlanItem.getDiscountValueEL();
        if (isNotBlank(dpValueEL)) {
            final BigDecimal evalDiscountValue = evaluateDiscountPercentExpression(dpValueEL, scAggregate.getUserAccount(), scAggregate.getWallet(), invoice, amount);
            log.debug("for discountPlan {} percentEL -> {}  on amount={}", discountPlanItem.getCode(), discountValue, amount);
            if (discountValue != null) {
                discountValue = evalDiscountValue;
            }
        }
        if (discountValue == null || amount == null) {
            return BigDecimal.ZERO;
        }
        if (discountValue.compareTo(BigDecimal.ZERO) < 0 && amount.compareTo(BigDecimal.ZERO) < 0 && discountValue.compareTo(HUNDRED.negate()) < 0) {
            discountValue = HUNDRED.negate();
        }
        if (discountValue.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(BigDecimal.ZERO) > 0 && discountValue.compareTo(HUNDRED) > 0) {
            discountValue = HUNDRED;
        }
        return discountValue;
    }

    private List<DiscountPlanItem> getApplicableDiscountPlanItems(BillingAccount billingAccount, List<DiscountPlanInstance> discountPlanInstances, Invoice invoice,
            CustomerAccount customerAccount) throws BusinessException {
        List<DiscountPlanItem> applicableDiscountPlanItems = new ArrayList<>();
        for (DiscountPlanInstance dpi : discountPlanInstances) {
            if (!dpi.isEffective(invoice.getInvoiceDate())) {
                continue;
            }
            if (dpi.getDiscountPlan().isActive()) {
                List<DiscountPlanItem> discountPlanItems = dpi.getDiscountPlan().getDiscountPlanItems();
                for (DiscountPlanItem discountPlanItem : discountPlanItems) {
                    if (discountPlanItem.isActive() && matchDiscountPlanItemExpression(discountPlanItem.getExpressionEl(), customerAccount, billingAccount, invoice, dpi)) {
                        applicableDiscountPlanItems.add(discountPlanItem);
                    }
                }
            }
        }
        return applicableDiscountPlanItems;
    }

    /**
     * @param expression      EL exprestion
     * @param customerAccount customer account
     * @param billingAccount  billing account
     * @param invoice         invoice
     * @param dpi             the discount plan instance
     * @return true/false
     * @throws BusinessException business exception.
     */
    private boolean matchDiscountPlanItemExpression(String expression, CustomerAccount customerAccount, BillingAccount billingAccount, Invoice invoice, DiscountPlanInstance dpi)
            throws BusinessException {
        Boolean result = true;

        if (StringUtils.isBlank(expression)) {
            return result;
        }
        Map<Object, Object> userMap = new HashMap<Object, Object>();

        if (expression.indexOf("ca") >= 0) {
            userMap.put("ca", customerAccount);
        }
        if (expression.indexOf("ba") >= 0) {
            userMap.put("ba", billingAccount);
        }
        if (expression.indexOf("iv") >= 0) {
            userMap.put("iv", invoice);
        }
        if (expression.indexOf("dpi") >= 0) {
            userMap.put("dpi", dpi);
        }
        if (expression.indexOf("su") >= 0) {
            userMap.put("su", invoice.getSubscription());
        }
        Object res = ValueExpressionWrapper.evaluateExpression(expression, userMap, Boolean.class);
        try {
            result = (Boolean) res;
        } catch (Exception e) {
            throw new BusinessException("Expression " + expression + " do not evaluate to boolean but " + res);
        }
        return result;
    }

    /**
     * @param expression  el expression
     * @param userAccount user account
     * @param wallet      wallet
     * @param invoice     invoice
     * @param subCatTotal total of sub category
     * @return amount
     * @throws BusinessException business exception
     */
    private BigDecimal evaluateDiscountPercentExpression(String expression, UserAccount userAccount, WalletInstance wallet, Invoice invoice, BigDecimal subCatTotal)
            throws BusinessException {

        if (StringUtils.isBlank(expression)) {
            return null;
        }
        Map<Object, Object> userMap = new HashMap<Object, Object>();
        userMap.put("ca", userAccount.getBillingAccount().getCustomerAccount());
        userMap.put("ba", userAccount.getBillingAccount());
        userMap.put("iv", invoice);
        userMap.put("invoice", invoice);
        userMap.put("wa", wallet);
        userMap.put("amount", subCatTotal);

        BigDecimal result = ValueExpressionWrapper.evaluateExpression(expression, userMap, BigDecimal.class);
        return result;
    }

    private Invoice instantiateInvoice(IBillableEntity entity, BillingAccount billingAccount, Seller seller, BillingRun billingRun, Date invoiceDate, boolean isDraft,
            BillingCycle billingCycle, PaymentMethod paymentMethod, InvoiceType invoiceType, boolean isPrepaid, BigDecimal dueBalance) throws BusinessException {

        Invoice invoice = new Invoice();

        invoice.setBillingAccount(billingAccount);
        invoice.setSeller(seller);
        invoice.setStatus(InvoiceStatusEnum.CREATED);
        invoice.setInvoiceType(invoiceType);
        invoice.setPrepaid(isPrepaid);
        invoice.setInvoiceDate(invoiceDate);
        if (billingRun != null) {
            invoice.setBillingRun(getEntityManager().getReference(BillingRun.class, billingRun.getId()));
        }
        Order order = null;
        if (entity instanceof Order) {
            order = (Order) entity;
            invoice.setOrder(order);

        } else if (entity instanceof Subscription) {
            invoice.setSubscription((Subscription) entity);
        }
        if (paymentMethod != null) {
            invoice.setPaymentMethodType(paymentMethod.getPaymentType());
            invoice.setPaymentMethod(paymentMethod);
        }

        CustomerAccount customerAccount = billingAccount.getCustomerAccount();

        // Determine invoice due date delay either from Order, Customer account or Billing cycle
        Integer delay = billingCycle.getDueDateDelay();
        if (order != null && !StringUtils.isBlank(order.getDueDateDelayEL())) {
            delay = evaluateDueDelayExpression(order.getDueDateDelayEL(), billingAccount, invoice, order);

        } else if (!StringUtils.isBlank(customerAccount.getDueDateDelayEL())) {
            delay = evaluateDueDelayExpression(customerAccount.getDueDateDelayEL(), billingAccount, invoice, order);

        } else if (!StringUtils.isBlank(billingCycle.getDueDateDelayEL())) {
            delay = evaluateDueDelayExpression(billingCycle.getDueDateDelayEL(), billingAccount, invoice, order);
        }
        if (delay == null) {
            delay = billingCycle.getDueDateDelay();
        }

        Date dueDate = invoiceDate;
        if (delay != null) {
            dueDate = DateUtils.addDaysToDate(invoiceDate, delay);
        } else {
            throw new BusinessException("Due date delay is null");
        }
        invoice.setDueDate(dueDate);

        // Set due balance
        invoice.setDueBalance(dueBalance.setScale(appProvider.getInvoiceRounding(), appProvider.getInvoiceRoundingMode().getRoundingMode()));

        return invoice;
    }

    /**
     * Recalculate tax to see if it has changed
     *
     * @param tax                  Previous tax
     * @param isExonerated         Is Billing account exonerated from taxes
     * @param invoice              Invoice in reference
     * @param invoiceSubCategory   Invoice subcategory to determine tax
     * @param userAccountId        User account identifier to calculate tax by external program
     * @param taxZero              Zero tax to apply if Billing account is exonerated
     * @param calculateExternalTax Should tax be calculated by an external program if invoiceSubCategory has such script set
     * @return An array containing applicable tax and True/false if tax % has changed from a previous tax
     * @throws BusinessException Were not able to determine a tax
     */
    private Object[] getApplicableTax(Tax tax, boolean isExonerated, Invoice invoice, InvoiceSubCategory invoiceSubCategory, Long userAccountId, Tax taxZero,
            boolean calculateExternalTax) throws BusinessException {

        if (isExonerated) {
            return new Object[] { taxZero, BigDecimal.ZERO, false };

        } else {

            Tax recalculatedTax = null;

            // If there is a taxScript in invoiceSubCategory and script is applicable, use it to compute external taxes
            if (calculateExternalTax && (invoiceSubCategory.getTaxScript() != null)) {
                UserAccount userAccount = userAccountId != null ? userAccountService.findById(userAccountId) : null;
                if (taxScriptService.isApplicable(invoiceSubCategory.getTaxScript().getCode(), userAccount, invoice, invoiceSubCategory)) {
                    List<Tax> taxes = taxScriptService.computeTaxes(invoiceSubCategory.getTaxScript().getCode(), userAccount, invoice, invoiceSubCategory);
                    if (!taxes.isEmpty()) {
                        recalculatedTax = taxes.get(0);
                    }
                }
            }

            if (recalculatedTax == null) {
                recalculatedTax = invoiceSubCategoryCountryService
                        .determineTax(invoiceSubCategory, invoice.getSeller(), invoice.getBillingAccount(), invoice.getInvoiceDate(), false);
            }

            return new Object[] { recalculatedTax, tax.getPercent().compareTo(recalculatedTax.getPercent()) != 0 };
        }
    }

    /**
     * Create an invoice from an InvoiceDto
     *
     * @param invoiceDTO
     * @param seller
     * @param billingAccount
     * @param invoiceType
     * @return invoice
     * @throws EntityDoesNotExistsException
     * @throws BusinessApiException
     * @throws BusinessException
     * @throws InvalidParameterException
     */
    public Invoice createInvoice(InvoiceDto invoiceDTO, Seller seller, BillingAccount billingAccount, InvoiceType invoiceType)
            throws EntityDoesNotExistsException, BusinessApiException, BusinessException, InvalidParameterException {
        boolean isEnterprise = appProvider.isEntreprise();
        int invoiceRounding = appProvider.getInvoiceRounding();
        RoundingModeEnum invoiceRoundingMode = appProvider.getInvoiceRoundingMode();

        Auditable auditable = new Auditable(currentUser);
        Map<Long, TaxInvoiceAgregate> taxInvoiceAgregateMap = new HashMap<Long, TaxInvoiceAgregate>();

        BigDecimal invoiceAmountWithoutTax = BigDecimal.ZERO;
        BigDecimal invoiceAmountTax = BigDecimal.ZERO;
        BigDecimal invoiceAmountWithTax = BigDecimal.ZERO;

        Invoice invoice = this.initInvoice(invoiceDTO, billingAccount, invoiceType, seller);

        EntityManager em = getEntityManager();

        for (CategoryInvoiceAgregateDto catInvAgrDto : invoiceDTO.getCategoryInvoiceAgregates()) {

            UserAccount userAccount = null;
            if (catInvAgrDto.getUserAccountCode() != null) {
                userAccount = userAccountService.findByCode(catInvAgrDto.getUserAccountCode());
                if (userAccount == null) {
                    throw new EntityDoesNotExistsException(UserAccount.class, catInvAgrDto.getUserAccountCode());
                } else if (!userAccount.getBillingAccount().equals(billingAccount)) {
                    throw new InvalidParameterException(
                            "User account code " + catInvAgrDto.getUserAccountCode() + " does not correspond to a Billing account " + billingAccount.getCode());
                }
            } else {
                userAccount = billingAccount.getUsersAccounts().get(0);
            }

            BigDecimal catAmountWithoutTax = BigDecimal.ZERO;
            BigDecimal catAmountTax = BigDecimal.ZERO;
            BigDecimal catAmountWithTax = BigDecimal.ZERO;
            CategoryInvoiceAgregate invoiceAgregateCat = new CategoryInvoiceAgregate();
            invoiceAgregateCat.setAuditable(auditable);
            invoiceAgregateCat.setInvoice(invoice);
            invoiceAgregateCat.setBillingRun(null);
            invoiceAgregateCat.setDescription(catInvAgrDto.getDescription());
            invoiceAgregateCat.setItemNumber(catInvAgrDto.getListSubCategoryInvoiceAgregateDto().size());
            invoiceAgregateCat.setUserAccount(userAccount);
            invoiceAgregateCat.setBillingAccount(billingAccount);
            invoiceAgregateCat.setInvoiceCategory(invoiceCategoryService.findByCode(catInvAgrDto.getCategoryInvoiceCode()));
            invoiceAgregateCat.setUserAccount(userAccount);
            invoice.addInvoiceAggregate(invoiceAgregateCat);

            for (SubCategoryInvoiceAgregateDto subCatInvAgrDTO : catInvAgrDto.getListSubCategoryInvoiceAgregateDto()) {
                BigDecimal subCatAmountWithoutTax = BigDecimal.ZERO;
                BigDecimal subCatAmountTax = BigDecimal.ZERO;
                BigDecimal subCatAmountWithTax = BigDecimal.ZERO;

                InvoiceSubCategory invoiceSubCategory = invoiceSubcategoryService.findByCode(subCatInvAgrDTO.getInvoiceSubCategoryCode());

                Tax tax = invoiceSubCategoryCountryService.determineTax(invoiceSubCategory, seller, billingAccount, invoiceDTO.getInvoiceDate(), false);

                SubCategoryInvoiceAgregate invoiceAgregateSubcat = new SubCategoryInvoiceAgregate();
                invoiceAgregateSubcat.setCategoryInvoiceAgregate(invoiceAgregateCat);
                invoiceAgregateSubcat.setInvoiceSubCategory(invoiceSubCategory);
                invoiceAgregateSubcat.setInvoice(invoice);
                invoiceAgregateSubcat.setDescription(subCatInvAgrDTO.getDescription());
                invoiceAgregateSubcat.setBillingRun(null);
                if (userAccount != null) {
                    invoiceAgregateSubcat.setWallet(userAccount.getWallet());
                    invoiceAgregateSubcat.setUserAccount(userAccount);
                }
                invoiceAgregateSubcat.setAccountingCode(invoiceSubCategory.getAccountingCode());
                invoiceAgregateSubcat.setAuditable(auditable);
                invoiceAgregateSubcat.setTaxPercent(tax.getPercent());
                invoiceAgregateSubcat.setTax(tax);
                invoice.addInvoiceAggregate(invoiceAgregateSubcat);

                boolean isDetailledInvoiceMode = InvoiceModeEnum.DETAILLED.name().equals(invoiceDTO.getInvoiceMode().name());
                if (subCatInvAgrDTO.getRatedTransactions() != null) {
                    for (RatedTransactionDto ratedTransactionDto : subCatInvAgrDTO.getRatedTransactions()) {

                        BigDecimal tempAmountWithoutTax = BigDecimal.ZERO;
                        if (ratedTransactionDto.getUnitAmountWithoutTax() != null) {
                            tempAmountWithoutTax = ratedTransactionDto.getUnitAmountWithoutTax().multiply(ratedTransactionDto.getQuantity());
                        }
                        BigDecimal tempAmountWithTax = BigDecimal.ZERO;
                        if (ratedTransactionDto.getUnitAmountWithTax() != null) {
                            tempAmountWithTax = ratedTransactionDto.getUnitAmountWithTax().multiply(ratedTransactionDto.getQuantity());
                        }

                        BigDecimal[] amounts = NumberUtils.computeDerivedAmounts(tempAmountWithoutTax, tempAmountWithTax, tax.getPercent(), isEnterprise, invoiceRounding,
                                invoiceRoundingMode.getRoundingMode());

                        BigDecimal amountWithoutTax = amounts[0];
                        BigDecimal amountWithTax = amounts[1];
                        BigDecimal amountTax = amounts[2];

                        RatedTransaction meveoRatedTransaction = new RatedTransaction(ratedTransactionDto.getUsageDate(), ratedTransactionDto.getUnitAmountWithoutTax(),
                                ratedTransactionDto.getUnitAmountWithTax(), ratedTransactionDto.getUnitAmountTax(), ratedTransactionDto.getQuantity(), amountWithoutTax,
                                amountWithTax, amountTax, RatedTransactionStatusEnum.BILLED, userAccount.getWallet(), billingAccount, userAccount, invoiceSubCategory, null, null,
                                null, null, null, null, ratedTransactionDto.getUnityDescription(), null, null, null, null, ratedTransactionDto.getCode(),
                                ratedTransactionDto.getDescription(), ratedTransactionDto.getStartDate(), ratedTransactionDto.getEndDate(), seller, tax, tax.getPercent(), null);

                        meveoRatedTransaction.setWallet(userAccount.getWallet());
                        // #3355 : setting params 1,2,3
                        if (isDetailledInvoiceMode) {
                            meveoRatedTransaction.setParameter1(ratedTransactionDto.getParameter1());
                            meveoRatedTransaction.setParameter2(ratedTransactionDto.getParameter2());
                            meveoRatedTransaction.setParameter3(ratedTransactionDto.getParameter3());
                        }

                        meveoRatedTransaction.changeStatus(RatedTransactionStatusEnum.BILLED);
                        meveoRatedTransaction.setInvoice(invoice);
                        meveoRatedTransaction.setInvoiceAgregateF(invoiceAgregateSubcat);

                        invoiceAgregateSubcat.addRatedTransaction(meveoRatedTransaction);

                        subCatAmountWithoutTax = subCatAmountWithoutTax.add(amountWithoutTax);
                        subCatAmountTax = subCatAmountTax.add(amountTax);
                        subCatAmountWithTax = subCatAmountWithTax.add(amountWithTax);
                    }
                }

                // Include existing Open rated transactions for a given user account and invoice sub category
                if (invoiceDTO.getInvoiceType().equals(invoiceTypeService.getCommercialCode())) {

                    List<RatedTransaction> openedRT = ratedTransactionService.openRTbySubCat(userAccount.getWallet(), invoiceSubCategory, null, null);
                    for (RatedTransaction ratedTransaction : openedRT) {
                        subCatAmountWithoutTax = subCatAmountWithoutTax.add(ratedTransaction.getAmountWithoutTax());
                        subCatAmountTax = subCatAmountTax.add(ratedTransaction.getAmountTax());
                        subCatAmountWithTax = subCatAmountWithTax.add(ratedTransaction.getAmountWithTax());

                        ratedTransaction.changeStatus(RatedTransactionStatusEnum.BILLED);
                        ratedTransaction.setInvoice(invoice);
                        ratedTransaction.setInvoiceAgregateF(invoiceAgregateSubcat);
                        invoiceAgregateSubcat.addRatedTransaction(ratedTransaction);
                    }
                }

                // TODO AKK Why the size does not take into account the existing open RTs that were added for a commercial invoice
                if (isDetailledInvoiceMode) {
                    invoiceAgregateSubcat.setItemNumber(subCatInvAgrDTO.getRatedTransactions().size());
                    invoiceAgregateSubcat.setAmountWithoutTax(subCatAmountWithoutTax);
                    invoiceAgregateSubcat.setAmountTax(subCatAmountTax);
                    invoiceAgregateSubcat.setAmountWithTax(subCatAmountWithTax);

                } else {
                    // we add subCatAmountWithoutTax, in the case if there any opened RT to include
                    BigDecimal[] amounts = NumberUtils
                            .computeDerivedAmounts(subCatInvAgrDTO.getAmountWithoutTax(), subCatInvAgrDTO.getAmountWithTax(), tax.getPercent(), isEnterprise, invoiceRounding,
                                    invoiceRoundingMode.getRoundingMode());

                    invoiceAgregateSubcat.setAmountWithoutTax(amounts[0]);
                    invoiceAgregateSubcat.setAmountWithTax(amounts[1]);
                    invoiceAgregateSubcat.setAmountTax(amounts[2]);
                }

                // Save invoice subcategory and associate rated transactions
                List<RatedTransaction> ratedTransactions = invoiceAgregateSubcat.getRatedtransactionsToAssociate();
                if (invoice.getId() == null) {
                    create(invoice);
                } else {
                    em.persist(invoiceAgregateSubcat);
                }

                for (RatedTransaction ratedTransaction : ratedTransactions) {
                    if (ratedTransaction.getId() == null) {
                        em.persist(ratedTransaction);
                    } else {
                        em.merge(ratedTransaction);
                    }
                }

                TaxInvoiceAgregate invoiceAgregateTax = null;

                if (taxInvoiceAgregateMap.containsKey(tax.getId())) {
                    invoiceAgregateTax = taxInvoiceAgregateMap.get(tax.getId());
                } else {
                    invoiceAgregateTax = new TaxInvoiceAgregate();
                    invoiceAgregateTax.setInvoice(invoice);
                    invoiceAgregateTax.setBillingRun(null);
                    invoiceAgregateTax.setTax(tax);
                    invoiceAgregateTax.setAccountingCode(tax.getAccountingCode());
                    invoiceAgregateTax.setTaxPercent(tax.getPercent());
                    invoiceAgregateTax.setAmountWithoutTax(BigDecimal.ZERO);
                    invoiceAgregateTax.setAmountWithTax(BigDecimal.ZERO);
                    invoiceAgregateTax.setAmountTax(BigDecimal.ZERO);
                    invoiceAgregateTax.setBillingAccount(billingAccount);
                    invoiceAgregateTax.setAuditable(auditable);
                    invoice.addInvoiceAggregate(invoiceAgregateTax);
                }
                invoiceAgregateTax.setAmountWithoutTax(invoiceAgregateTax.getAmountWithoutTax().add(invoiceAgregateSubcat.getAmountWithoutTax()));
                invoiceAgregateTax.setAmountTax(invoiceAgregateTax.getAmountTax().add(invoiceAgregateSubcat.getAmountTax()));
                invoiceAgregateTax.setAmountWithTax(invoiceAgregateTax.getAmountWithTax().add(invoiceAgregateSubcat.getAmountWithTax()));

                taxInvoiceAgregateMap.put(tax.getId(), invoiceAgregateTax);

                catAmountWithoutTax = catAmountWithoutTax.add(invoiceAgregateSubcat.getAmountWithoutTax());
                catAmountTax = catAmountTax.add(invoiceAgregateSubcat.getAmountTax());
                catAmountWithTax = catAmountWithTax.add(invoiceAgregateSubcat.getAmountWithTax());

            }
            em.flush();
            invoiceAgregateCat.setAmountWithoutTax(catAmountWithoutTax);
            invoiceAgregateCat.setAmountTax(catAmountTax);
            invoiceAgregateCat.setAmountWithTax(catAmountWithTax);

            invoiceAmountWithoutTax = invoiceAmountWithoutTax.add(invoiceAgregateCat.getAmountWithoutTax());
            invoiceAmountTax = invoiceAmountTax.add(invoiceAgregateCat.getAmountTax());
            invoiceAmountWithTax = invoiceAmountWithTax.add(invoiceAgregateCat.getAmountWithTax());
        }

        invoice.setAmountWithoutTax(round(invoiceAmountWithoutTax, invoiceRounding, invoiceRoundingMode));
        invoice.setAmountTax(round(invoiceAmountTax, invoiceRounding, invoiceRoundingMode));
        invoice.setAmountWithTax(round(invoiceAmountWithTax, invoiceRounding, invoiceRoundingMode));

        BigDecimal netToPay = invoice.getAmountWithTax();
        if (!appProvider.isEntreprise() && invoiceDTO.isIncludeBalance() != null && invoiceDTO.isIncludeBalance()) {
            BigDecimal balance = customerAccountService.customerAccountBalanceDue(null, invoice.getBillingAccount().getCustomerAccount().getCode(), invoice.getDueDate());

            if (balance == null) {
                throw new BusinessException("account balance calculation failed");
            }
            netToPay = invoice.getAmountWithTax().add(round(balance, invoiceRounding, invoiceRoundingMode));
        }
        invoice.setNetToPay(netToPay);
        if (invoiceDTO.isAutoValidation() == null || invoiceDTO.isAutoValidation()) {
            invoice = serviceSingleton.assignInvoiceNumberVirtual(invoice);
        }
        this.postCreate(invoice);
        return invoice;

    }

    private Invoice initInvoice(InvoiceDto invoiceDTO, BillingAccount billingAccount, InvoiceType invoiceType, Seller seller)
            throws BusinessException, EntityDoesNotExistsException, BusinessApiException {
        Invoice invoice = new Invoice();
        invoice.setBillingAccount(billingAccount);
        invoice.setSeller(seller);
        invoice.setInvoiceDate(invoiceDTO.getInvoiceDate());
        invoice.setDueDate(invoiceDTO.getDueDate());
        invoice.setDraft(invoiceDTO.isDraft());
        invoice.setAlreadySent(invoiceDTO.isCheckAlreadySent());
        if (invoiceDTO.isCheckAlreadySent()) {
            invoice.setStatus(InvoiceStatusEnum.SENT);
        } else {
            invoice.setStatus(InvoiceStatusEnum.CREATED);
        }
        invoice.setDontSend(invoiceDTO.isSentByEmail());
        PaymentMethod preferedPaymentMethod = billingAccount.getCustomerAccount().getPreferredPaymentMethod();
        if (preferedPaymentMethod != null) {
            invoice.setPaymentMethodType(preferedPaymentMethod.getPaymentType());
        }
        invoice.setInvoiceType(invoiceType);
        if (invoiceDTO.getListInvoiceIdToLink() != null) {
            for (Long invoiceId : invoiceDTO.getListInvoiceIdToLink()) {
                Invoice invoiceTmp = findById(invoiceId);
                if (invoiceTmp == null) {
                    throw new EntityDoesNotExistsException(Invoice.class, invoiceId);
                }
                if (!invoiceType.getAppliesTo().contains(invoiceTmp.getInvoiceType())) {
                    throw new BusinessApiException("InvoiceId " + invoiceId + " cant be linked");
                }
                invoice.getLinkedInvoices().add(invoiceTmp);
            }
        }

        return invoice;
    }

    /**
     * Delete invoices associated to a billing run
     *
     * @param billingRun Billing run
     */
    public void deleteInvoices(BillingRun billingRun) {
        getEntityManager().createNamedQuery("Invoice.deleteByBR").setParameter("billingRun", billingRun).executeUpdate();
    }

    /**
     * Rated transactions to invoice
     */
    private class RatedTransactionsToInvoice {

        /**
         * Indicates that there are more RTs to be retrieved and aggregated in invoice before invoice can be closed
         */
        private boolean moreRatedTransactions;

        /**
         * Rated transactions split for invoicing based on Billing account, seller and invoice type
         */
        private List<RatedTransactionGroup> ratedTransactionGroups;

        /**
         * Constructor
         *
         * @param moreRatedTransactions  Indicates that there are more RTs to be retrieved and aggregated in invoice before invoice can be closed
         * @param ratedTransactionGroups Rated transactions split for invoicing based on Billing account, seller and invoice type
         */
        private RatedTransactionsToInvoice(boolean moreRatedTransactions, List<RatedTransactionGroup> ratedTransactionGroups) {
            super();
            this.moreRatedTransactions = moreRatedTransactions;
            this.ratedTransactionGroups = ratedTransactionGroups;
        }
    }

    /**
     * Stores Invoice and invoice aggregate information between paginated RT invoicing
     */
    private class InvoiceAggregateProcessingInfo {

        /**
         * Invoice
         */
        private Invoice invoice = null;

        /**
         * Tax change mapping. Key is ba.id_seller.id_invoiceType.id_ua.id_walletInstance.id_invoiceSubCategory.id_tax.id and value is an array of [Tax to apply, True/false if tax
         * has changed]
         */
        private Map<String, Object[]> taxChangeMap = new HashMap<>();

        /**
         * Subcategory aggregates mapping. Key is ua.id_walletInstance.id_invoiceSubCategory.id_tax.id
         */
        private Map<String, SubCategoryInvoiceAgregate> subCategoryAggregates = new HashMap<>();

        /**
         * Orders (numbers) referenced from Rated transactions
         */
        private Set<String> orderNumbers = new HashSet<String>();
    }
}