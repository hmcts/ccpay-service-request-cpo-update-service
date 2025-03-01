package uk.gov.hmcts.reform.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.reform.dtos.responses.IdamTokenResponse;
import uk.gov.hmcts.reform.exceptions.CpoUpdateException;

import static org.springframework.http.HttpHeaders.EMPTY;

@Service
public class IdamServiceImpl implements IdamService {

    @Value("${idam.serviceAccount.clientId}")
    private String clientId;

    @Value("${idam.serviceAccount.clientSecret}")
    private String clientSecret;

    @Value("${idam.serviceAccount.grantType}")
    private String grantType;

    @Value("${idam.serviceAccount.username}")
    private String username;

    @Value("${idam.serviceAccount.password}")
    private String password;

    @Value("${idam.serviceAccount.scope}")
    private String scope;

    @Autowired
    @Qualifier("restTemplateIdam")
    private RestTemplate restTemplateIdam;

    @Value("${idam.url}")
    private String idamBaseUrl;

    private static final String TOKEN_ENDPOINT_PATH = "/o/token";

    private static final Logger LOG = LoggerFactory.getLogger(IdamServiceImpl.class);


    @Override
    public IdamTokenResponse getSecurityTokens() {

        if (null != idamBaseUrl) {
            idamBaseUrl = idamBaseUrl.replace(".prod", "");
        }
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
            .fromUriString(idamBaseUrl + TOKEN_ENDPOINT_PATH)
            .queryParam("client_id",clientId)
            .queryParam("client_secret",clientSecret)
            .queryParam("grant_type",grantType)
            .queryParam("password",password)
            .queryParam("scope",scope)
            .queryParam("username",username);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<IdamTokenResponse> idamTokenResponse = restTemplateIdam
                .exchange(
                    builder.build(false).toUriString(),
                    HttpMethod.POST,
                    new HttpEntity<>(httpHeaders, EMPTY),
                    IdamTokenResponse.class
                );
            return idamTokenResponse.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            LOG.error("exception {}",exception.getMessage());
            throw new CpoUpdateException("IDAM",exception.getStatusCode(),exception);
        }

    }
}
