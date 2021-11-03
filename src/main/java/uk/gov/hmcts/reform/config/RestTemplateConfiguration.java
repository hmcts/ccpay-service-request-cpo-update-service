package uk.gov.hmcts.reform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableRetry
public class RestTemplateConfiguration {

    @Bean("restTemplateIdam")
    public RestTemplate restTemplateIdam() {
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

    @Bean("restTemplateCpo")
    public RestTemplate restTemplateCpo() {
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

}
