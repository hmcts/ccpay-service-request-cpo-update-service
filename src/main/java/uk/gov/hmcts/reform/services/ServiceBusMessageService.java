package uk.gov.hmcts.reform.services;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public interface ServiceBusMessageService {
    void processMessageFromTopic(List<byte[]> body, AtomicBoolean result) throws IOException;
}
