package io.mosip.registration.processor.stages.ocr_document_classifier;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.dto.Document;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.stages.exception.FileEmptyException;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


/**
 *
 * This is the class where business logic for Document classification goes where we will check the document and classify accordingly like
 *  is the document valid, document verified successfully etc.
 *
 */

@Service
@Transactional
public class OCRDocumentClassificationProcessor {

    /**
     * The reg proc logger.
     */
    private static Logger regProcLogger =
            RegProcessorLogger.getLogger(OCRDocumentClassificationProcessor.class);

    /**
     * The Constant USER.
     */
    private static final String USER = "MOSIP_SYSTEM";

    private static final String VALUE = "value";

    /** The tag value that will be used by default when the packet does not have value for the tag field */
    @Value("${mosip.regproc.packet.classifier.tagging.not-available-tag-value}")
    private String notAvailableTagValue;

    /**
     * The registration status service.
     */
    @Autowired
    RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;


    @Autowired
    public PriorityBasedPacketManagerService priorityBasedPacketManagerService;


    /** The registration status mapper util. */
    @Autowired
    private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

    private TrimExceptionMessage trimExpMessage = new TrimExceptionMessage();

    private TrimExceptionMessage trimExceptionMsg = new TrimExceptionMessage();

    @Autowired
    private Utilities utility;

    String process = null;

    // field, value, details
//    private boolean validateField(String field, String value, Map<String, String> details)
//    {
//
//        System.out.println("\n\ninside validateField. name from document = " + details.get(field) + ".\n name extracted from registration id = " + value + "\n\n");
//        return (value.equals(details.get(field)));
//
//    }

    /**
     * Process methods will be called will the stage to perform packet classification for each packet
     * @param object This is the actual event object will contains all the meta information about the packet
     * @param stageName The stageName that needs to be used in audit and status updates
     * @return The same event object with proper internal error and valid status set
     */
    public MessageDTO process(MessageDTO object, String stageName) {


        LogDescription description = new LogDescription();
        boolean isTransactionSuccessful = false;
        String registrationId = "";

        object.setMessageBusAddress(MessageBusAddress.OCR_CLASSIFIER_IN);
        object.setIsValid(FALSE);
        object.setInternalError(Boolean.TRUE);

        registrationId = object.getRid();
        regProcLogger.debug("process called for registrationId {}", registrationId);


        InternalRegistrationStatusDto registrationStatusDto = registrationStatusService.getRegistrationStatus(registrationId);

        registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.OCR_DOCUMENT_VALIDATION.toString()); // OCR_CLASSIFIER is the stage name and it shud be added in RegistrationTransactionTypeCode class.
        registrationStatusDto.setRegistrationStageName(stageName);

        process = registrationStatusDto.getRegistrationType();
        try
        {
            // name fetching
            String name = priorityBasedPacketManagerService.getFieldByMappingJsonKey(registrationId,
                   MappingJsonConstants.NAME, registrationStatusDto.getRegistrationType(),
                    ProviderStageName.OCR_DOCUMENT_VALIDATOR);

            //System.out.println("name is = " + name);
            // document fetching
            // took this from E:\IIIT BANGALORE Semester material\Sem-3\PE\git pulls of MOSIP\registration\registration-processor\pre-processor\registration-processor-packet-validator-stage\src\main\java\io\mosip\registration\processor\stages\ utils\ApplicantDocumentValidation.java
            JSONObject docMappingJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT);


//            JSONObject functionOutput = JsonUtil.getJSONObject(docMappingJson, MappingJsonConstants.POI);
//            String proofOfIdentityLabel = JsonUtil.getJSONValue(functionOutput, VALUE);
            String proofOfIdentityLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(docMappingJson, MappingJsonConstants.POI), VALUE);
            //System.out.println("functionOutput = " + functionOutput.get("value"));
            String proofOfDateOfBirthLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(docMappingJson, MappingJsonConstants.POB), VALUE);

            //System.out.println("proofOfIdentityLabel======= = " + proofOfIdentityLabel + ".\n proofOfDateOfBirthLabel = " + proofOfDateOfBirthLabel);


            List<String> fields = new ArrayList<>();
            fields.add(proofOfDateOfBirthLabel);
            fields.add(proofOfIdentityLabel);

            Map<String, String> docFields = priorityBasedPacketManagerService.getFields(registrationId, fields, process, ProviderStageName.OCR_DOCUMENT_VALIDATOR);

            // added extra line.
            //System.out.println("docFields size===== = " + docFields.size());


            boolean DOBDOCUMENT=TRUE, IDENTITYDOCUMENT=TRUE;
            if (docFields.get(proofOfDateOfBirthLabel) != null) {

                //System.out.println("inside docFields.get(proofOfDateOfBirthLabel) != null ");
                //Document temp = priorityBasedPacketManagerService.getDocument(registrationId, proofOfDateOfBirthLabel, process, ProviderStageName.OCR_DOCUMENT_VALIDATOR);
                if (priorityBasedPacketManagerService.getDocument(registrationId, proofOfDateOfBirthLabel, process, ProviderStageName.OCR_DOCUMENT_VALIDATOR) == null)
                //System.out.println("temp = " + temp);
                //if (temp == null)
                    DOBDOCUMENT= FALSE;
            }
            if (docFields.get(proofOfIdentityLabel) != null) {
                if (priorityBasedPacketManagerService.getDocument(registrationId, proofOfIdentityLabel, process, ProviderStageName.OCR_DOCUMENT_VALIDATOR) == null)
                    IDENTITYDOCUMENT= FALSE;
            }

            System.out.println("DOBDOCUMENT = " + DOBDOCUMENT +" IDENTITYDOCUMENT = " + IDENTITYDOCUMENT);


            if(DOBDOCUMENT==FALSE && IDENTITYDOCUMENT==FALSE)
            {
                description.setCode(PlatformErrorMessages.RPR_OCR_NAME_VALIDATION_FILES_EMPTY.getCode());
                description.setMessage(PlatformErrorMessages.RPR_OCR_NAME_VALIDATION_FILES_EMPTY.getMessage());
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
                        PlatformErrorMessages.RPR_OCR_NAME_VALIDATION_FILES_EMPTY.getMessage());
                throw new FileEmptyException(PlatformErrorMessages.RPR_OCR_NAME_VALIDATION_FILES_EMPTY.getCode(),
                        PlatformErrorMessages.RPR_OCR_NAME_VALIDATION_FILES_EMPTY.getMessage());
            }

            // --------------------------------
            else
            {
                if(DOBDOCUMENT)
                {
                    //System.out.println("\n=====inside if(DOBDOCUMENT) =====\n");
                    Document dobdocument = priorityBasedPacketManagerService.getDocument(registrationId, proofOfDateOfBirthLabel, process, ProviderStageName.OCR_DOCUMENT_VALIDATOR);

                    //System.out.println("dobdocument = "+ dobdocument);

                    TesseractOCR tesseractOCR = new TesseractOCR();
                    Map<String, String> details = tesseractOCR.getDetails(dobdocument);

                    // field, value, details.
                    LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
                    //String name = "ALAM MAQSOOD";
                    int similarityScore = levenshteinDistance.apply(name.toLowerCase(), details.get("Name").toLowerCase());
                    //System.out.println("\nLevenshtein Distance between "+ name +" and " + details.get("Name") + " = "+similarityScore);
//                    boolean result = validateField("Name", name, details);
//                    //boolean result = validateField("name", "name", details);
//                    System.out.println("result11: "+ result);
                    boolean result=FALSE;

                    if(similarityScore < 3)
                        result= TRUE;
                    object.setIsValid(result);
                    object.setInternalError(!result);

                    /* convert to lowercase and do it.
                        ==> if we are using levenshtein distance then we have to uncomment below code:

                        int value = calculate(name, details.get("Name"));

                        int lengthOfName = name.length();

                        //if value is < (0.6)*lengthOfName then reject.
                        //otherwise accept.

                        if((float)value < ((0.6)*lengthOfName))
                        {
                            object.setIsValid(false);
                            object.setInternalError(true);
                        }
                        else
                        {
                            object.setIsValid(true);
                            object.setInternalError(false);
                        }
                        */

                }
                else
                {
                    //System.out.println("\n=====inside if(IDENTITYDOCUMENT) =====\n");
                    Document identityDocument = priorityBasedPacketManagerService.getDocument(registrationId,
                            proofOfIdentityLabel, process, ProviderStageName.OCR_DOCUMENT_VALIDATOR);

                    //TesseractOCR tesseractOCR = new TesseractOCR();
                    /*
                    Map<String, String> details = tesseractOCR.getDetails(identityDocument);

                    // field, value, details.
                    boolean result = validateField("name", name, details);
                    System.out.println("result22: "+ result);
                    //boolean result = validateField("name", "name", details);
                    object.setIsValid(result);
                    object.setInternalError(!result);

 */
                    //object.setIsValid(TRUE);
                    //object.setInternalError(FALSE);

                    TesseractOCR tesseractOCR = new TesseractOCR();
                    Map<String, String> details = tesseractOCR.getDetails(identityDocument);

                    LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
                    int similarityScore = levenshteinDistance.apply(name.toLowerCase(), details.get("Name").toLowerCase());
                    boolean result=FALSE;

                    if(similarityScore < 3)
                        result= TRUE;
                    object.setIsValid(result);
                    object.setInternalError(!result);
                }


                /*
                    1. check parsePassportTD3()
                    2. error -> catch() fill it
                    3. may be check tesseract optimization.

                 */
            }
            /*
                to ensure modularity we thought of putting lines 138 to 169 in another function. but we werent sure of the return value of this function
                so we didnt proceed with this idea.

            */
            // --------------------------------

        }
        catch (ApisResourceAccessException e) {
            registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION));
            registrationStatusDto.setStatusComment(trimExpMessage
                    .trimExceptionMessage(StatusUtil.API_RESOUCE_ACCESS_FAILED.getMessage() + e.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
            object.setInternalError(Boolean.TRUE);
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    registrationId, PlatformErrorMessages.RPR_PUM_NGINX_ACCESS_FAILED.name() + ExceptionUtils.getStackTrace(e));

            description.setMessage(PlatformErrorMessages.RPR_PUM_NGINX_ACCESS_FAILED.getMessage());
            description.setCode(PlatformErrorMessages.RPR_PUM_NGINX_ACCESS_FAILED.getCode());
        }
        // here we have to create our own DOCUMENTNOTFOUNDEXCEPTION and add it here.
        catch (FileEmptyException e) {
            registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
            registrationStatusDto.setStatusComment(StatusUtil.NAME_VALIDATION_FILES_EMPTY.getMessage());
            registrationStatusDto.setSubStatusCode(StatusUtil.NAME_VALIDATION_FILES_EMPTY.getCode());
            registrationStatusDto.setLatestTransactionStatusCode(
                    registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.OCR_EXCEPTION));
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    registrationId,
                    PlatformErrorMessages.RPR_OCR_NAME_VALIDATION_FILES_EMPTY.getMessage() + ExceptionUtils.getStackTrace(e));
            object.setInternalError(Boolean.TRUE);
            description.setCode(PlatformErrorMessages.RPR_OCR_NAME_VALIDATION_FILES_EMPTY.getCode());
            description.setMessage(PlatformErrorMessages.RPR_OCR_NAME_VALIDATION_FILES_EMPTY.getMessage());

        }
        catch (JsonProcessingException e)
        {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    registrationId, RegistrationStatusCode.FAILED.toString() + e.getMessage()
                            + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
            registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
            registrationStatusDto.setStatusComment(trimExceptionMsg
                    .trimExceptionMessage(StatusUtil.JSON_PARSING_EXCEPTION.getMessage() + e.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.JSON_PARSING_EXCEPTION.getCode());
            registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
                    .getStatusCode(RegistrationExceptionTypeCode.JSON_PROCESSING_EXCEPTION));
            description.setMessage(PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION.getCode());
            object.setInternalError(Boolean.TRUE);
            e.printStackTrace();
        }
        catch (IOException e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    registrationId, RegistrationStatusCode.FAILED.toString() + e.getMessage()
                            + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
            registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
            registrationStatusDto.setStatusComment(
                    trimExceptionMsg.trimExceptionMessage(StatusUtil.IO_EXCEPTION.getMessage() + e.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());
            registrationStatusDto.setLatestTransactionStatusCode(
                    registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION));
            description.setMessage(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode());
            object.setInternalError(Boolean.TRUE);
        }

        catch (PacketManagerException e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    registrationId, RegistrationStatusCode.FAILED.toString() + e.getMessage()
                            + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
            registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
            registrationStatusDto.setStatusComment(trimExceptionMsg
                    .trimExceptionMessage(StatusUtil.PACKET_MANAGER_EXCEPTION.getMessage() + e.getMessage()));
            registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_MANAGER_EXCEPTION.getCode());
            registrationStatusDto.setLatestTransactionStatusCode(
                    registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_EXCEPTION));
            description.setMessage(PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getCode());
            object.setInternalError(Boolean.TRUE);
        }
        catch (Exception e)
        {
            // error from tesseractOCR class.
        }

        /*
            Here the steps would be as follows

            1. Take the <name> from packet.
            2. Take the supporting proof doc from packet.
            3. Send the doc to OCR module to extract <name>.
            4. OCR module sends back the name
            5. As we have both names, we will match and apply corresponding tags.
            6. Return saying whether the validation is successful or not.


            *  Doubt in 1,2. How to do them??



         */
        return object;

    }
    // To implement levenshtein distance: uncomment below.
    /*
        public static int costOfSubstitution(char a, char b) {
            return a == b ? 0 : 1;
        }

        public static int calculate(String x, String y) {
            int[][] dp = new int[x.length() + 1][y.length() + 1];

            for (int i = 0; i <= x.length(); i++) {
                for (int j = 0; j <= y.length(); j++) {
                    if (i == 0) {
                        dp[i][j] = j;
                    }
                    else if (j == 0) {
                        dp[i][j] = i;
                    }
                    else {
                        dp[i][j] = (int) Math.min( Math.min(dp[i - 1][j - 1] + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)), dp[i - 1][j] + 1), dp[i][j - 1] + 1);
                    }
                }
            }

            return dp[x.length()][y.length()];
        }
     */
}
