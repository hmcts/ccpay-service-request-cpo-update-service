package uk.gov.hmcts.reform.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.dtos.requests.CpoUpdateServiceRequest;
import uk.gov.hmcts.reform.dtos.responses.IdamTokenResponse;
import uk.gov.hmcts.reform.exceptions.CpoUpdateException;
import uk.gov.hmcts.reform.exceptions.MaxTryExceededException;

import java.util.Arrays;

@Service
public class CpoUpdateServiceImpl implements CpoUpdateService {

    @Autowired
    private IdamService idamService;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    @Qualifier("restTemplateCpo")
    private RestTemplate restTemplateCpo;

    @Value("${cpo.callbackBaseUrl}")
    private String cpoCallBackBaseUrl;

    @Value("${cpo.callbackPath}")
    private String callbackBasePath;

    private static final Logger LOG = LoggerFactory.getLogger(CpoUpdateServiceImpl.class);

    @Override
    @Retryable(CpoUpdateException.class)
    public void updateCpoServiceWithPayment(CpoUpdateServiceRequest cpoUpdateServiceRequest) {
        LOG.info("updateCpoServiceWithPayment");
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                                            .fromUriString(cpoCallBackBaseUrl + callbackBasePath);
        LOG.info("CPO URL {}",builder.toUriString());
        try {
            restTemplateCpo.exchange(builder.toUriString(), HttpMethod.POST,
                                     new HttpEntity<>(cpoUpdateServiceRequest, getHttpHeaders()), ResponseEntity.class);
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            LOG.info(" exception {}",exception.getMessage());
            throw new CpoUpdateException("CPO",exception.getStatusCode(),exception);
        } catch (ResourceAccessException exception) {
            LOG.info(" exception {}",exception.getMessage());
            throw new CpoUpdateException("CPO", HttpStatus.SERVICE_UNAVAILABLE,exception);
        }
    }

    @Override
    @Recover
    public void recover(CpoUpdateException exception, CpoUpdateServiceRequest cpoUpdateServiceRequest) {
        LOG.info("issue in connecting {}",exception.getServer());
        LOG.info("Recovery send to dl queue");
        throw new MaxTryExceededException(exception.getServer(),exception.getStatus(),exception);

    }

    private MultiValueMap<String,String> getHttpHeaders() {
        MultiValueMap<String, String> inputHeaders = new LinkedMultiValueMap<>();
        inputHeaders.put("content-type",Arrays.asList("application/json"));
        inputHeaders.put("Authorization", Arrays.asList(getAccessToken()));
        inputHeaders.put("ServiceAuthorization", Arrays.asList(getServiceAuthorisationToken()));
        return inputHeaders;
    }

    private String getServiceAuthorisationToken() {
        try {
            return authTokenGenerator.generate();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new CpoUpdateException("S2S",e.getStatusCode(),e);
        } catch (Exception e) {
            throw new CpoUpdateException("S2S",HttpStatus.SERVICE_UNAVAILABLE,e);
        }
    }

    private String getAccessToken() {
        IdamTokenResponse idamTokenResponse = idamService.getSecurityTokens();
        return idamTokenResponse.getAccessToken();
    }
}