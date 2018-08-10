package org.meveo.api.helpers.document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.meveo.api.dto.document.PDFContractRequestDto;
import org.meveo.api.dto.document.PDFTemplateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A business delegate class to generate pdf contract file.
 * This will prevent the client code from dealing with the implementation or the libs used to achieve the said pdf file generation.
 * @author Said Ramli
 */
public class PDFContractHelper {
    
    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(PDFContractHelper.class);
    
    /**
     * Generate PDF contract.
     *
     * @param postData the post data
     * @return the string
     * @throws Exception the exception
     */
    public static String generatePDFContract(PDFContractRequestDto postData) throws Exception {
        
        String contractDir = StringUtils.defaultIfEmpty(postData.getContractDestinationDir(), ".");
        String contarctNamePrefix = StringUtils.defaultIfEmpty(postData.getContarctNamePrefix(), "contract");
        
        try (PDDocument mainTemplateDoc = new PDDocument() ) {
            
            PDFBuilder pdfBuilder = PDFBuilder.newInstance(contractDir, contarctNamePrefix, mainTemplateDoc);
            String pdfFilePath = null;
            
             //  postData.getListTemplates size should be already verified
            for (PDFTemplateDto templateDto : postData.getListTemplates()) {
                
                File templateFile = new File(templateDto.getTemplatePath());
                try ( PDDocument templateDoc = PDDocument.load(templateFile) ) {
                    
                    pdfBuilder.withFormFieds(templateDto.getTemplateFields())
                            .withBarcodeFieds(templateDto.getBarCodeFields())
                            .withTemplate(templateDoc)
                            .buildAndAppendToMainTemplate();
                }
            }
            pdfFilePath = pdfBuilder.save();
            LOG.debug(" file created : " + pdfFilePath);
            return pdfFilePath;
            
        } catch (Exception e) {
            LOG.error("error on generatePDFContract {} ", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Gets the pdf file as bytes.
     *
     * @param pdfFilePath the pdf file path
     * @return the pdf file as bytes
     * @throws FileNotFoundException the file not found exception
     */
    public static byte[] getPdfFileAsBytes(String pdfFilePath) throws FileNotFoundException {
        
        File pdfFile = new File(pdfFilePath);
        if (!pdfFile.exists()) {
            throw new FileNotFoundException("Contract PDF was not found ! pdfFilePath :  " + pdfFilePath);
        }
        try (FileInputStream fileInputStream = new FileInputStream(pdfFile)) {
            long fileSize = pdfFile.length();
            if (fileSize > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("File is too big to put it to buffer in memory.");
            }
            byte[] fileBytes = new byte[(int) fileSize];
            fileInputStream.read(fileBytes);
            return fileBytes;
        } catch (Exception e) {
            LOG.error("Error reading contract PDF file {} contents", pdfFilePath, e);
        } 
        return null;
    }
}
