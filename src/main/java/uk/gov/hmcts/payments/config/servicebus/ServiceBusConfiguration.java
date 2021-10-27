package uk.gov.hmcts.payments.config.servicebus;


import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.SubscriptionClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

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


}
