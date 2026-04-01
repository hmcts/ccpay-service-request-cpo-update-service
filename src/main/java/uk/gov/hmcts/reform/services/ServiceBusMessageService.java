package uk.gov.hmcts.reform.services;

import com.microsoft.azure.servicebus.IMessage;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public interface ServiceBusMessageService {
    void processMessageFromTopic(IMessage message, AtomicBoolean result) throws IOException;
}
