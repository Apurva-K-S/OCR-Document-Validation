
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.dto.Document;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.stages.ocr_document_classifier.OCRDocumentClassificationProcessor;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.analysis.function.Pow;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ JsonUtil.class, IOUtils.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "javax.net.ssl.*" })
public class OCRDocumentClassificationProcessorTest {

    @InjectMocks
    private OCRDocumentClassificationProcessor ocrDocumentClassificationProcessor;

    @Mock
    private Utilities utility;

    /** The registration status service. */
    @Mock
    RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

    /** The audit log request builder. */
    @Mock
    private AuditLogRequestBuilder auditLogRequestBuilder;

    /** The dto. */
    MessageDTO messageDTO = new MessageDTO();

    private String stageName;

    /** The registration status dto. */
    InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();

    @Mock
    public PriorityBasedPacketManagerService priorityBasedPacketManagerService = PowerMockito.mock(PriorityBasedPacketManagerService.class,
            Mockito.withSettings()
                    .name("PriorityBasedPacketManagerServiceMock"));

    @Mock
    RegistrationExceptionMapperUtil registrationStatusMapperUtil;

    JSONObject regProcessorIdentityJson=mock(JSONObject.class);
    JSONObject documentLabel=mock(JSONObject.class);
    String label="label33";
    String source="source";
    String name="Apurva";
    JSONObject demographicIdentityJSONObject=mock(JSONObject.class);
    JSONObject proofOfDocument;

//    @Before
//    public void setup(){
//        MockitoAnnotations.initMocks(this);
//    }

    @Before
    public void setUp() throws Exception {

        registrationStatusDto = new InternalRegistrationStatusDto();
        registrationStatusDto.setRegistrationId("123456789");
        Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
        Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());

        //priorityBasedPacketManagerService = mock(PriorityBasedPacketManagerService.class);

        /*
        make an object to send it to process.
        in that object we need to set all object variable's values.

        to check create another object with the actual output values.


        now coming to document:
         */

        Map<String,String> map=new HashMap<>();
        map.put("value", "documentValue22");
        proofOfDocument=new JSONObject(map);

        when(utility.getRegistrationProcessorMappingJson(anyString())).thenReturn(regProcessorIdentityJson);

        PowerMockito.mockStatic(JsonUtil.class);
        //Mockito.when(priorityBasedPacketManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any())).thenReturn("Apurva");
        //PowerMockito.whenNew(PriorityBasedPacketManagerService.class).withAnyArguments().thenReturn(priorityBasedPacketManagerService);

        // -------------------------------
        //Mockito.when(priorityBasedPacketManagerService.getFieldByMappingJsonKey(any(), any(), any(), any())).thenReturn("LENGKAP NAmA");

        //PowerMockito.when(priorityBasedPacketManagerService, "getFieldByMappingJsonKey", anyString(), anyString(), anyString(), any()).thenReturn(name);

        PowerMockito.when(JsonUtil.class, "getJSONObject", any(), any()).thenReturn(proofOfDocument);
        PowerMockito.when(JsonUtil.class, "getJSONValue", any(), anyString()).thenReturn(label);

        //when(priorityBasedPacketManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any())).thenCallRealMethod().thenReturn("Apurva");

        //PowerMockito.when(PriorityBasedPacketManagerService.class, "getFieldByMappingJsonKey", anyString(), anyString(), anyString(), any()).thenReturn("Apurva");

        when(utility.getRegistrationProcessorMappingJson(anyString())).thenReturn(demographicIdentityJSONObject);
        Map<String, String> documentFields = new HashMap<>();
        documentFields.put("label33", "value");

        Mockito.when(priorityBasedPacketManagerService.getFields(any(), any(), any(), any())).thenReturn(documentFields);

        //when(docFields).thenReturn(documentFields);

        BufferedImage bImage = ImageIO.read(new File("E:\\SEM_3\\MOSIP PE\\repos\\actual working repo\\registration\\registration-processor\\pre-processor\\registration-processor-ocr-document-validator-stage\\src\\main\\resources\\Indonesian_passport.jpg"));
        //BufferedImage bImage = ImageIO.read(new File("F:\\Indonesian_passport_data_page_1.jpg"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(bImage, "jpg", bos );

        byte [] data = bos.toByteArray();


        Document documentObj = new Document();
        documentObj.setType(MappingJsonConstants.POI);
        documentObj.setValue(MappingJsonConstants.POI);
        documentObj.setDocument(data);

        Mockito.when(priorityBasedPacketManagerService.getDocument(any(), any(), any(), any())).thenReturn(documentObj);

        messageDTO = new MessageDTO();
        messageDTO.setRid("123456789");
        messageDTO.setInternalError(false);
        messageDTO.setIsValid(true);
        messageDTO.setReg_type(RegistrationType.NEW);
        stageName = "ocrDocumentValidationStage";
    }

    @Test
    public void ocrDocumentValidationSuccess() throws Exception {

        Mockito.when(priorityBasedPacketManagerService.getFieldByMappingJsonKey(any(), any(), any(), any())).thenReturn("LENGKAP NAmA");
        MessageDTO object = ocrDocumentClassificationProcessor.process(messageDTO, stageName);

        assertTrue(object.getIsValid());
        //assertTrue(true);
        assertFalse(object.getInternalError());
       // assertFalse(false);
    }

    @Test
    public void ocrDocumentValidationFailure() throws Exception {

        Mockito.when(priorityBasedPacketManagerService.getFieldByMappingJsonKey(any(), any(), any(), any())).thenReturn("LENGKAP");
        when(priorityBasedPacketManagerService.getDocument(anyString(),anyString(),anyString(), any())).thenReturn(null);

        MessageDTO object = ocrDocumentClassificationProcessor.process(messageDTO, stageName);

        assertFalse(object.getIsValid());
        assertTrue(object.getInternalError());
    }
}