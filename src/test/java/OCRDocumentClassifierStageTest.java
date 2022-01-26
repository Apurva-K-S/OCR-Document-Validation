import io.mosip.registration.processor.core.abstractverticle.*;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.stages.ocr_document_classifier.OCRDocumentClassificationProcessor;
import io.mosip.registration.processor.stages.ocr_document_classifier.OCRDocumentClassifierStage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class OCRDocumentClassifierStageTest {
    MessageDTO dto = new MessageDTO();

    @Mock
    private OCRDocumentClassificationProcessor ocrDocumentClassificationProcessor;

    @Mock
    private MosipRouter router;
    @Mock
    MosipEventBus mosipEventBus;

    @InjectMocks
    private OCRDocumentClassifierStage ocrDocumentClassifierStage = new OCRDocumentClassifierStage() {
        @Override
        public MosipEventBus getEventBus(Object verticleName, String url, int instanceNumber) {
            vertx = Vertx.vertx();

            return new MosipEventBus() {

                @Override
                public Vertx getEventbus() {
                    return vertx;
                }

                @Override
                public void consume(MessageBusAddress fromAddress,
                                    EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {
                }

                @Override
                public void consumeAndSend(MessageBusAddress fromAddress, MessageBusAddress toAddress,
                                           EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {
                }

                @Override
                public void send(MessageBusAddress toAddress, MessageDTO message) {
                }
            };
        }

        @Override
        public Integer getPort() {
            return 8080;
        };

        @Override
        public void consumeAndSend(MosipEventBus eventbus, MessageBusAddress addressbus1,
                                   MessageBusAddress addressbus2, long messageExpiryTimeLimit) {
        }

        @Override
        public Router postUrl(Vertx vertx, MessageBusAddress consumeAddress, MessageBusAddress sendAddress) {
            return null;

        }
        @Override
        public void createServer(Router router, int port) {

        }
    };

    @Test
    public void testStart()
    {
        System.out.println("Inside testStart()");
        Mockito.doNothing().when(router).setRoute(any());
        ocrDocumentClassifierStage.start();
    }

    @Test
    public void testDeployVerticle() {
        System.out.println("Inside testDeployVerticle()");
        ReflectionTestUtils.setField(ocrDocumentClassifierStage, "workerPoolSize", 10);
        ReflectionTestUtils.setField(ocrDocumentClassifierStage, "clusterManagerUrl", "/dummyPath");
        ReflectionTestUtils.setField(ocrDocumentClassifierStage, "messageExpiryTimeLimit", Long.valueOf(0));
        ocrDocumentClassifierStage.deployVerticle();
    }

    @Test
    public void testProcess() {
        MessageDTO result = new MessageDTO();
        result.setIsValid(true);
        Mockito.when(ocrDocumentClassificationProcessor.process(any(), any())).thenReturn(result);
        dto = ocrDocumentClassifierStage.process(dto);

        boolean output = dto.getIsValid();
        System.out.println("Inside testProcess(). output is = " + output);

        assertTrue(output);
    }
}
