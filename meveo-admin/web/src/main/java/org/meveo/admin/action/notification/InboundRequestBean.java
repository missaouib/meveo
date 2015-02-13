package org.meveo.admin.action.notification;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.RejectedImportException;
import org.meveo.commons.utils.CsvBuilder;
import org.meveo.commons.utils.CsvReader;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.notification.InboundRequest;
import org.meveo.model.notification.Notification;
import org.meveo.model.notification.NotificationEventTypeEnum;
import org.meveo.model.notification.NotificationHistory;
import org.meveo.model.notification.StrategyImportTypeEnum;
import org.meveo.model.notification.WebHook;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.notification.InboundRequestService;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

@Named
@ConversationScoped
public class InboundRequestBean extends BaseBean<InboundRequest> {

    private static final long serialVersionUID = -6762628879784107169L;

    @Inject
    InboundRequestService inboundRequestService;

    public InboundRequestBean() {
        super(InboundRequest.class);
    }
    
    
    CsvReader csvReader = null;
    private UploadedFile file; 
    
    private StrategyImportTypeEnum strategyImportType;
    
    private static final int FROM_IP= 0;
    private static final int PORT= 1;
    private static final int PORTOCOL= 2; 
    private static final int PATH_INFO= 3;
    private static final int CODE= 4; 
    private static final int ACTIVE= 5;
    private static final int SCHEME= 6;
    private static final int CONTENT_TYPE= 7;
    private static final int CONTENT_LENGHT= 8;
    private static final int METHOD= 9;
    private static final int AUTHENTIFICATION_TYPE= 10;
    private static final int REQUEST_URI= 11;  
    private static final int RESPONSE_CONTENT_TYPE= 15;
    private static final int ENCODING= 16;  
    
    
    


    @Override
    protected IPersistenceService<InboundRequest> getPersistenceService() {
        return inboundRequestService;
    }

    protected String getDefaultViewName() {
        return "inboundRequests";
    }

    @Override
    protected List<String> getFormFieldsToFetch() {
        return Arrays.asList("parameters", "coockies", "headers", "responseCoockies", "responseHeaders");
    }

    @Override
    public InboundRequest initEntity() {
        InboundRequest inboundRequest = super.initEntity();

        extractMapTypeFieldFromEntity(inboundRequest.getHeaders(), "headers");
        extractMapTypeFieldFromEntity(inboundRequest.getParameters(), "parameters");
        extractMapTypeFieldFromEntity(inboundRequest.getCoockies(), "coockies");
        extractMapTypeFieldFromEntity(inboundRequest.getResponseCoockies(), "responseCoockies");
        extractMapTypeFieldFromEntity(inboundRequest.getResponseHeaders(), "responseHeaders");

        return inboundRequest;
    }

    @Override
    protected Map<String, Object> supplementSearchCriteria(Map<String, Object> searchCriteria) {

        // Do not user a check against user.provider as it contains only one value, while user can be linked to various providers
        searchCriteria.put(PersistenceService.SEARCH_SKIP_PROVIDER_CONSTRAINT, true);

        return searchCriteria;
    }
    
    public void exportToFile() throws Exception {

        CsvBuilder csv = new CsvBuilder();
        csv.appendValue("Update date"); 
        csv.appendValue("From IP"); 
        csv.appendValue("Port"); 
        csv.appendValue("Protocol");
        csv.appendValue("Path info");
        csv.appendValue("Code");
        csv.appendValue("Active");
        csv.appendValue("Scheme");
        csv.appendValue("Content type");
        csv.appendValue("Content length");
        csv.appendValue("Method");
        csv.appendValue("Authentication type");
        csv.appendValue("Request URI");
        csv.appendValue("Cookies");
        csv.appendValue("Headers");
        csv.appendValue("Parameters");
        csv.appendValue("ContentType");
        csv.appendValue("Encoding");
        csv.appendValue("Cookies");
        csv.appendValue("Headers"); 
        csv.startNewLine();
        for(InboundRequest  inboundRequest:inboundRequestService.list()){ 
        	 csv.appendValue(inboundRequest.getRemoteAddr());
        	 csv.appendValue(inboundRequest.getRemotePort()+"");
        	 csv.appendValue(inboundRequest.getProtocol());
        	 csv.appendValue(inboundRequest.getPathInfo());
        	 csv.appendValue(inboundRequest.getCode());
        	 csv.appendValue(inboundRequest.isDisabled()+"");
        	 csv.appendValue(inboundRequest.getScheme());
        	 csv.appendValue(inboundRequest.getContentType());
        	 csv.appendValue(inboundRequest.getContentLength()+"");
        	 csv.appendValue(inboundRequest.getMethod());
        	 csv.appendValue(inboundRequest.getAuthType());
        	 csv.appendValue(inboundRequest.getRequestURI());
        	 csv.appendValue(inboundRequest.getCoockies()+"");
        	 csv.appendValue(inboundRequest.getHeaders()+"");
        	 csv.appendValue(inboundRequest.getParameters()+"");
        	 csv.appendValue(inboundRequest.getResponseContentType()+"");
        	 csv.appendValue(inboundRequest.getResponseEncoding()+"");
        	 csv.appendValue(inboundRequest.getResponseCoockies()+"");
        	 csv.appendValue(inboundRequest.getResponseHeaders()+"");
        	 csv.appendValue(DateUtils.formatDateWithPattern(inboundRequest.getAuditable().getUpdated(), "dd/MM/yyyy"));
        	 csv.startNewLine();
        }
        InputStream inputStream=new ByteArrayInputStream(csv.toString().getBytes());
        csv.download(inputStream, "InboundRequests.csv");
    }
    

    public void handleFileUpload(FileUploadEvent event) throws Exception {
    	try {
    		file = event.getFile();
    	    log.info("handleFileUpload " + file);
    	    upload();
    	} catch (BusinessException e) {
    		log.error(e.getMessage(),e);
    		messages.error(e.getMessage());
    	} catch (IOException e) {
    		log.error(e.getMessage(),e);
    		messages.error(e.getMessage());
    	}
        
    }

	public void upload() throws IOException, BusinessException {
		if (file != null) {
			csvReader = new CsvReader(file.getInputstream(), ';',
					Charset.forName("ISO-8859-1"));
			csvReader.readHeaders();
			try {
				while (csvReader.readRecord()) {
					String[] values = csvReader.getValues();
					InboundRequest existingEntity = inboundRequestService
							.findByCode(values[CODE], getCurrentProvider());
					if (existingEntity != null) {
						checkSelectedStrategy(values, existingEntity);
					} else {
						InboundRequest inboundRequest = new InboundRequest();
						inboundRequest.setRemoteAddr(values[FROM_IP]);
						inboundRequest.setRemotePort(!StringUtils
								.isBlank(values[PORT]) ? Integer
								.parseInt(values[PORT]) : null);
						inboundRequest.setProtocol(values[PORTOCOL]);
						inboundRequest.setPathInfo(values[PATH_INFO]);
						inboundRequest.setCode(values[CODE]);
						inboundRequest.setDisabled(Boolean
								.parseBoolean(values[ACTIVE]));
						inboundRequest.setScheme(values[SCHEME]);
						inboundRequest.setContentType(values[CONTENT_TYPE]);
						inboundRequest.setContentLength(Integer
								.parseInt(values[CONTENT_LENGHT]));
						inboundRequest.setMethod(values[METHOD]);
						inboundRequest
								.setAuthType(values[AUTHENTIFICATION_TYPE]);
						inboundRequest.setRequestURI(values[REQUEST_URI]);
						inboundRequest
								.setResponseContentType(values[RESPONSE_CONTENT_TYPE]);
						inboundRequest.setResponseEncoding(values[ENCODING]);
						inboundRequestService.create(inboundRequest);
					}
				}
				messages.info(new BundleKey("messages", "commons.csv"));
			} catch (RejectedImportException e) {
				messages.error(new BundleKey("messages", e.getMessage()));
			}
		}
	}

	public void checkSelectedStrategy(String[] values,
			InboundRequest existingEntity) throws RejectedImportException {
		if (strategyImportType.equals(StrategyImportTypeEnum.UPDATED)) {
			existingEntity.setRemoteAddr(values[FROM_IP]);
			existingEntity
					.setRemotePort(!StringUtils.isBlank(values[PORT]) ? Integer
							.parseInt(values[PORT]) : null);
			existingEntity.setProtocol(values[PORTOCOL]);
			existingEntity.setPathInfo(values[PATH_INFO]);
			existingEntity.setDisabled(Boolean.parseBoolean(values[ACTIVE]));
			existingEntity.setScheme(values[SCHEME]);
			existingEntity.setContentType(values[CONTENT_TYPE]);
			existingEntity.setContentLength(Integer
					.parseInt(values[CONTENT_LENGHT]));
			existingEntity.setMethod(values[METHOD]);
			existingEntity.setAuthType(values[AUTHENTIFICATION_TYPE]);
			existingEntity.setRequestURI(values[REQUEST_URI]);
			existingEntity
					.setResponseContentType(values[RESPONSE_CONTENT_TYPE]);
			existingEntity.setResponseEncoding(values[ENCODING]);
			inboundRequestService.update(existingEntity);
		} else if (strategyImportType
				.equals(StrategyImportTypeEnum.REJECT_EXISTING_RECORDS)) {
			// add to a new csv
		} else if (strategyImportType
				.equals(StrategyImportTypeEnum.REJECTE_IMPORT)) {
			throw new RejectedImportException("notification.rejectImport");
		}
	}

	public StrategyImportTypeEnum getStrategyImportType() {
		return strategyImportType;
	}

	public void setStrategyImportType(StrategyImportTypeEnum strategyImportType) {
		this.strategyImportType = strategyImportType;
	}
    
    
}