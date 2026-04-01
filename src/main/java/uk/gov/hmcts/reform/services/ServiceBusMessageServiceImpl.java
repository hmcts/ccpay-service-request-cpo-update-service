package uk.gov.hmcts.reform.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.dtos.requests.CpoUpdateServiceRequest;
import uk.gov.hmcts.reform.exceptions.InvalidCpoUpdateRequestException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class ServiceBusMessageServiceImpl implements ServiceBusMessageService {
    private static final String HEADER_SIGNATURE = "X-Message-Signature";
    private static final String HEADER_SENDER = "X-Sender-Service";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String EXPECTED_INBOUND_SENDER = "ccpay-payment";
    private static final Duration MAX_MESSAGE_AGE = Duration.ofMinutes(5);

    @Autowired
    private CpoUpdateService cpoUpdateService;

    @Value("${hmac.secrets.ccpay-message-signing-key}")
    private String ccpayMessageSigningKey;

    private static final Logger LOG = LoggerFactory.getLogger(ServiceBusMessageServiceImpl.class);

    @Override
    public void processMessageFromTopic(IMessage message, AtomicBoolean result) throws IOException {
        validateMessageSecurity(message);

        var body = message.getMessageBody().getBinaryData();
        ObjectMapper mapper = new ObjectMapper();
        CpoUpdateServiceRequest cpoUpdateServiceRequest;
        try {
            cpoUpdateServiceRequest = mapper.readValue(body.get(0), CpoUpdateServiceRequest.class);
        } catch (Exception e) {
            throw new InvalidCpoUpdateRequestException("Bad Request", HttpStatus.BAD_REQUEST, e);
        }
        String cpoUpdateServiceRequestMessage = mapper.writeValueAsString(cpoUpdateServiceRequest);
        LOG.info("CPO message body: {}", cpoUpdateServiceRequestMessage);
        this.cpoUpdateService.updateCpoServiceWithPayment(cpoUpdateServiceRequest);
        result.set(Boolean.TRUE);
    }

    private void validateMessageSecurity(IMessage message) {
        Map<String, Object> properties = message.getProperties() == null ? Map.of() : message.getProperties();
        String signature = asString(properties.get(HEADER_SIGNATURE));
        String sender = asString(properties.get(HEADER_SENDER));
        String timestamp = asString(properties.get(HEADER_TIMESTAMP));

        if (signature == null || sender == null || timestamp == null) {
            throw new SecurityException("Missing required security headers");
        }

        if (!EXPECTED_INBOUND_SENDER.equals(sender)) {
            throw new SecurityException("Unexpected sender: " + sender);
        }

        if (isExpired(timestamp)) {
            throw new SecurityException("Message expired");
        }

        String payloadToSign = buildPayloadToSign(message, timestamp, sender);
        String expectedSignature = hmacSha256Base64(payloadToSign, ccpayMessageSigningKey);

        boolean matches = java.security.MessageDigest.isEqual(
            Base64.getDecoder().decode(signature),
            Base64.getDecoder().decode(expectedSignature)
        );

        if (!matches) {
            throw new SecurityException("Invalid message signature");
        }
    }

    private String buildPayloadToSign(IMessage message, String timestamp, String sender) {
        return String.join("|",
            "v1",
            sender,
            timestamp,
            asString(message.getLabel()),
            asString(message.getContentType()),
            Base64.getEncoder().encodeToString(message.getBody())
        );
    }

    private boolean isExpired(String timestamp) {
        Instant messageTime = Instant.parse(timestamp);
        Instant now = Instant.now();
        return messageTime.isBefore(now.minus(MAX_MESSAGE_AGE))
               || messageTime.isAfter(now.plusSeconds(30));
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    public String hmacSha256Base64(String payload, String base64Secret) {
        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException("hmac.secrets.ccpay-message-signing-key must be configured");
        }
        try {
            byte[] secretBytes = Base64.getDecoder().decode(base64Secret);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to calculate HMAC-SHA256", e);
        }
    }
}
