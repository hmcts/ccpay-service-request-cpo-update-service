package uk.gov.hmcts.reform.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.dtos.requests.CpoUpdateServiceRequest;
import uk.gov.hmcts.reform.exceptions.InvalidCpoUpdateRequestException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ServiceBusMessageServiceTest {

    @Mock
    private AtomicBoolean result;

    @Mock
    private CpoUpdateService cpoUpdateService;

    @InjectMocks
    private ServiceBusMessageServiceImpl serviceBusMessageService;

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void processMessageFromTopicTest() throws IOException {
        CpoUpdateServiceRequest cpoUpdateServiceRequest = CpoUpdateServiceRequest.CpoUpdateServiceRequest()
                                                            .caseId(Long.valueOf(123))
                                                            .orderReference("order-reference")
                                                            .responsibleParty("responsible-party")
                                                            .action("action")
                                                            .build();
        byte[] objAsBytes = objectMapper.writeValueAsString(cpoUpdateServiceRequest).getBytes();
        List<byte[]> body = Arrays.asList(objAsBytes);
        Mockito.doNothing().when(cpoUpdateService).updateCpoServiceWithPayment(cpoUpdateServiceRequest);
        serviceBusMessageService.processMessageFromTopic(body,result);
        Mockito.verify(cpoUpdateService).updateCpoServiceWithPayment(cpoUpdateServiceRequest);
    }

    @Test
    void processMessageFromTopicWithInvalidRequestBodyShouldThrowInvalidCpoUpdateRequestException() throws IOException {
        CpoUpdateServiceRequest cpoUpdateServiceRequest = CpoUpdateServiceRequest.CpoUpdateServiceRequest()
            .caseId(Long.valueOf(123))
            .orderReference("order-reference")
            .build();
        byte[] objAsBytes = cpoUpdateServiceRequest.toString().getBytes();
        List<byte[]> body = Arrays.asList(objAsBytes);
        InvalidCpoUpdateRequestException exception = assertThrows(InvalidCpoUpdateRequestException.class,
            () -> serviceBusMessageService.processMessageFromTopic(body, result)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus(),"Status should be Bad Request");
    }
}
