package uk.gov.hmcts.reform.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.dtos.requests.CpoUpdateServiceRequest;
import uk.gov.hmcts.reform.exceptions.InvalidCpoUpdateRequestException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceBusMessageServiceTest {
    private static final String HMAC_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String SENDER = "ccpay-payment";
    private static final String LABEL = "Service Callback Message";
    private static final String CONTENT_TYPE = "application/json";

    @Mock
    private AtomicBoolean result;

    @Mock
    private CpoUpdateService cpoUpdateService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IMessage message;

    @InjectMocks
    private ServiceBusMessageServiceImpl serviceBusMessageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void processMessageFromTopicTest() throws IOException {
        CpoUpdateServiceRequest request = CpoUpdateServiceRequest.CpoUpdateServiceRequest()
            .caseId(123L)
            .orderReference("order-reference")
            .responsibleParty("responsible-party")
            .action("action")
            .build();

        configureSigningSecret(HMAC_SECRET);
        configureValidMessage(request, Instant.now().toString());
        doNothing().when(cpoUpdateService).updateCpoServiceWithPayment(request);

        serviceBusMessageService.processMessageFromTopic(message, result);

        verify(cpoUpdateService).updateCpoServiceWithPayment(request);
        verify(result).set(Boolean.TRUE);
    }

    @Test
    void processMessageFromTopicWithInvalidRequestBodyShouldThrowInvalidCpoUpdateRequestException() {
        configureSigningSecret(HMAC_SECRET);
        CpoUpdateServiceRequest cpoUpdateServiceRequest = CpoUpdateServiceRequest.CpoUpdateServiceRequest()
            .caseId(123L)
            .orderReference("order-reference")
            .build();
        String invalidBody = cpoUpdateServiceRequest.toString();
        configureMessage(
            invalidBody.getBytes(StandardCharsets.UTF_8),
            signedProperties(invalidBody, Instant.now().toString())
        );

        InvalidCpoUpdateRequestException exception = assertThrows(
            InvalidCpoUpdateRequestException.class,
            () -> serviceBusMessageService.processMessageFromTopic(message, result)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus(), "Status should be Bad Request");
        assertEquals("Bad Request", exception.getServer(), "Exception should be Bad Request");
    }

    @Test
    void processMessageFromTopicWithInvalidSignatureShouldThrowSecurityException() throws IOException {
        CpoUpdateServiceRequest request = CpoUpdateServiceRequest.CpoUpdateServiceRequest()
            .caseId(123L)
            .orderReference("order-reference")
            .responsibleParty("responsible-party")
            .action("action")
            .build();

        configureSigningSecret(HMAC_SECRET);
        String body = objectMapper.writeValueAsString(request);
        Map<String, Object> properties = signedProperties(body, Instant.now().toString());
        properties.put("X-Message-Signature",
            signedProperties("different-body", Instant.now().toString()).get("X-Message-Signature"));
        configureMessage(body.getBytes(StandardCharsets.UTF_8), properties);

        SecurityException exception = assertThrows(
            SecurityException.class,
            () -> serviceBusMessageService.processMessageFromTopic(message, result)
        );

        assertEquals("Invalid message signature", exception.getMessage());
    }

    @Test
    void processMessageFromTopicWithMissingSecurityHeadersShouldThrowSecurityException() throws IOException {
        CpoUpdateServiceRequest request = CpoUpdateServiceRequest.CpoUpdateServiceRequest()
            .caseId(123L)
            .orderReference("order-reference")
            .responsibleParty("responsible-party")
            .action("action")
            .build();

        configureSigningSecret(HMAC_SECRET);
        configureSecurityProperties(Map.of());

        SecurityException exception = assertThrows(
            SecurityException.class,
            () -> serviceBusMessageService.processMessageFromTopic(message, result)
        );

        assertEquals("Missing required security headers", exception.getMessage());
    }

    @Test
    void processMessageFromTopicWithUnexpectedSenderShouldThrowSecurityException() throws IOException {
        CpoUpdateServiceRequest request = CpoUpdateServiceRequest.CpoUpdateServiceRequest()
            .caseId(123L)
            .orderReference("order-reference")
            .responsibleParty("responsible-party")
            .action("action")
            .build();

        configureSigningSecret(HMAC_SECRET);
        String body = objectMapper.writeValueAsString(request);
        Map<String, Object> properties = signedProperties(body, Instant.now().toString());
        properties.put("X-Sender-Service", "unexpected-sender");
        configureSecurityProperties(properties);

        SecurityException exception = assertThrows(
            SecurityException.class,
            () -> serviceBusMessageService.processMessageFromTopic(message, result)
        );

        assertEquals("Unexpected sender: unexpected-sender", exception.getMessage());
    }

    @Test
    void processMessageFromTopicWithSignatureNotBase64ShouldThrowSecurityException() throws IOException {
        CpoUpdateServiceRequest request = CpoUpdateServiceRequest.CpoUpdateServiceRequest()
            .caseId(123L)
            .orderReference("order-reference")
            .responsibleParty("responsible-party")
            .action("action")
            .build();

        configureSigningSecret(HMAC_SECRET);
        String body = objectMapper.writeValueAsString(request);
        Map<String, Object> properties = signedProperties(body, Instant.now().toString());
        properties.put("X-Message-Signature", "not-base64");
        configureMessage(body.getBytes(StandardCharsets.UTF_8), properties);

        SecurityException exception = assertThrows(
            SecurityException.class,
            () -> serviceBusMessageService.processMessageFromTopic(message, result)
        );

        assertEquals("Invalid message signature", exception.getMessage());
    }

    @Test
    void processMessageFromTopicWithExpiredTimestampShouldStillProcessMessage() throws IOException {
        CpoUpdateServiceRequest request = CpoUpdateServiceRequest.CpoUpdateServiceRequest()
            .caseId(123L)
            .orderReference("order-reference")
            .responsibleParty("responsible-party")
            .action("action")
            .build();

        configureSigningSecret(HMAC_SECRET);
        String body = objectMapper.writeValueAsString(request);
        configureMessage(
            body.getBytes(StandardCharsets.UTF_8),
            signedProperties(body, Instant.now().minusSeconds(31 * 60L).toString())
        );
        doNothing().when(cpoUpdateService).updateCpoServiceWithPayment(request);

        serviceBusMessageService.processMessageFromTopic(message, result);

        verify(cpoUpdateService).updateCpoServiceWithPayment(request);
        verify(result).set(Boolean.TRUE);
    }

    @Test
    void processMessageFromTopicWithBlankSecretShouldThrowIllegalStateException() throws IOException {
        CpoUpdateServiceRequest request = CpoUpdateServiceRequest.CpoUpdateServiceRequest()
            .caseId(123L)
            .orderReference("order-reference")
            .responsibleParty("responsible-party")
            .action("action")
            .build();

        configureSigningSecret("");
        configureValidMessage(request, Instant.now().toString());

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> serviceBusMessageService.processMessageFromTopic(message, result)
        );

        assertEquals("hmac.secrets.ccpay-message-signing-key must be configured", exception.getMessage());
    }

    private void configureSigningSecret(String signingSecret) {
        ReflectionTestUtils.setField(serviceBusMessageService, "ccpayMessageSigningKey", signingSecret);
    }

    private void configureValidMessage(CpoUpdateServiceRequest request, String timestamp) throws IOException {
        String body = objectMapper.writeValueAsString(request);
        configureMessage(body.getBytes(StandardCharsets.UTF_8), signedProperties(body, timestamp));
    }

    private void configureMessage(byte[] bodyBytes, Map<String, Object> properties) {
        configureSecurityProperties(properties);
        when(message.getMessageBody().getBinaryData()).thenReturn(List.of(bodyBytes));
        when(message.getBody()).thenReturn(bodyBytes);
        when(message.getLabel()).thenReturn(LABEL);
        when(message.getContentType()).thenReturn(CONTENT_TYPE);
    }

    private void configureSecurityProperties(Map<String, Object> properties) {
        when(message.getProperties()).thenReturn(properties);
    }

    private Map<String, Object> signedProperties(String body, String timestamp) {
        String payload = String.join("|",
            "v1",
            SENDER,
            timestamp,
            LABEL,
            CONTENT_TYPE,
            Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8))
        );

        Map<String, Object> properties = new HashMap<>();
        properties.put("X-Message-Signature", serviceBusMessageService.hmacSha256Base64(payload, HMAC_SECRET));
        properties.put("X-Sender-Service", SENDER);
        properties.put("X-Timestamp", timestamp);
        return properties;
    }
}
