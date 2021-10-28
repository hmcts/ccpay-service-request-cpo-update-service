package uk.gov.hmcts.payments.config.servicebus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.SubscriptionClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ServiceBusConfiguration {

    @Value("${amqp.host}")
    private String host;

    @Value("${amqp.sharedAccessKeyName}")
    private String sharedAccessKeyName;

    @Value("${amqp.jrd.topic}")
    private String topic;

    @Value("${amqp.jrd.sharedAccessKeyValue}")
    private String sharedAccessKeyValue;

    @Value("${amqp.jrd.subscription}")
    private String subscription;

    private static Logger log = LoggerFactory.getLogger(ServiceBusConfiguration.class);

    @Bean
    public SubscriptionClient getSubscriptionClient() throws URISyntaxException, ServiceBusException,
        InterruptedException {
        log.info(" host {}",host);
        log.info(" sharedAccessKeyName {}",sharedAccessKeyName);
        log.info(" topic {}",topic);
        log.info(" sharedAccessKeyValue {}",sharedAccessKeyValue);
        log.info(" subscription {}",subscription);
        URI endpoint = new URI("sb://" + host);

        String destination = topic.concat("/subscriptions/").concat(subscription);

        ConnectionStringBuilder connectionStringBuilder = new ConnectionStringBuilder(
            endpoint,
            destination,
            sharedAccessKeyName,
            sharedAccessKeyValue);
        connectionStringBuilder.setOperationTimeout(Duration.ofMinutes(10));
        return new SubscriptionClient(connectionStringBuilder, ReceiveMode.PEEKLOCK);
    }

    @Bean
    CompletableFuture<Void> registerMessageHandlerOnClient(@Autowired SubscriptionClient receiveClient)
        throws ServiceBusException, InterruptedException {

        log.info("Calling registerMessageHandlerOnClient");

        IMessageHandler messageHandler = new IMessageHandler() {

            @Override
            public CompletableFuture<Void> onMessageAsync(IMessage message) {
                log.info("RECEIVED");
                List<byte[]> body = message.getMessageBody().getBinaryData();
                try {
                    processMessage(body);
                    return receiveClient.completeAsync(message.getLockToken());
                } catch (Exception e) {
                    log.info("e");
                }
                return null;

            }

            @Override
            public void notifyException(Throwable throwable, ExceptionPhase exceptionPhase) {
                log.error("Exception occurred.");
                log.error(exceptionPhase + "-" + throwable.getMessage());
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        receiveClient.registerMessageHandler(
            messageHandler, new MessageHandlerOptions(1,
                                                      false, Duration.ofHours(1), Duration.ofMinutes(5)),
            executorService);
        return null;
    }

    private void processMessage(List<byte[]> body) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String message = mapper.writeValueAsString(mapper.readValue(body.get(0), Object.class));
        log.info(message);
    }
}
