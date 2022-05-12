package uk.gov.hmcts.reform.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.dtos.requests.CpoUpdateServiceRequest;
import uk.gov.hmcts.reform.exceptions.InvalidCpoUpdateRequestException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ServiceBusMessageServiceImpl implements ServiceBusMessageService {

    @Autowired
    private CpoUpdateService cpoUpdateService;

    private static final Logger LOG = LoggerFactory.getLogger(ServiceBusMessageServiceImpl.class);

    @Override
    public void processMessageFromTopic(List<byte[]> body, AtomicBoolean result) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        CpoUpdateServiceRequest cpoUpdateServiceRequest;
        try {
            cpoUpdateServiceRequest = mapper.readValue(body.get(0), CpoUpdateServiceRequest.class);
            if (cpoUpdateServiceRequest.getAction().equalsIgnoreCase("DLQTest"))
            {
                throw new InvalidCpoUpdateRequestException("Internal Server Error", HttpStatus.SERVICE_UNAVAILABLE, new IOException());
            }
        } catch (Exception e) {
            throw new InvalidCpoUpdateRequestException("Bad Request", HttpStatus.BAD_REQUEST,e);
        }
        String message = mapper.writeValueAsString(cpoUpdateServiceRequest);
        LOG.info(message);
        this.cpoUpdateService.updateCpoServiceWithPayment(cpoUpdateServiceRequest);
        result.set(Boolean.TRUE);
    }
}
