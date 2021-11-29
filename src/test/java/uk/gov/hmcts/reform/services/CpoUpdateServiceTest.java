package uk.gov.hmcts.reform.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.dtos.requests.CpoUpdateServiceRequest;
import uk.gov.hmcts.reform.dtos.responses.IdamTokenResponse;
import uk.gov.hmcts.reform.exceptions.CpoUpdateException;
import uk.gov.hmcts.reform.exceptions.MaxTryExceededException;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CpoUpdateServiceTest {

    @Mock
    private IdamService idamService;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private RestTemplate restTemplateCpo;

    @InjectMocks
    private final CpoUpdateServiceImpl cpoUpdateService = new CpoUpdateServiceImpl();

    IdamTokenResponse idamTokenResponse = IdamTokenResponse.idamFullNameRetrivalResponseWith()
        .refreshToken("refresh-token")
        .idToken("id-token")
        .accessToken("access-token")
        .expiresIn("10")
        .scope("openid profile roles")
        .tokenType("type")
        .build();
    private static final String MOCK_S2S_TOKEN = "mock-serv-auth-token";
    private static final String S2S_SERVER = "S2S";

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(cpoUpdateService, "cpoBaseUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(cpoUpdateService, "cpoPath", "/cpopath");
    }

    @Test
    void updateCpoServiceWithPaymentShouldUpdateCpoService() {
        Mockito.when(restTemplateCpo.exchange(anyString(),eq(HttpMethod.POST),Mockito.any(),eq(
            ResponseEntity.class))).thenReturn(ResponseEntity.status(HttpStatus.OK).build());
        Mockito.when(idamService.getSecurityTokens()).thenReturn(idamTokenResponse);
        Mockito.when(authTokenGenerator.generate()).thenReturn(MOCK_S2S_TOKEN);
        cpoUpdateService.updateCpoServiceWithPayment(getCpoUpdateServiceRequest());
        Mockito.verify(authTokenGenerator).generate();
        Mockito.verify(idamService).getSecurityTokens();
        HttpEntity entity = getHttpEntity();
        Mockito.verify(restTemplateCpo).exchange("http://localhost:3000/cpopath",HttpMethod.POST,entity,ResponseEntity.class);
    }


    @Test
    void updateCpoServiceWithPaymentWhenServiceUnavailableShouldThrowCpoUpdateSection() {

        Mockito.when(restTemplateCpo.exchange(anyString(),eq(HttpMethod.POST),Mockito.any(),eq(
            ResponseEntity.class))).thenThrow(new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE));
        Mockito.when(idamService.getSecurityTokens()).thenReturn(idamTokenResponse);
        Mockito.when(authTokenGenerator.generate()).thenReturn(MOCK_S2S_TOKEN);
        CpoUpdateException exception = assertThrows(CpoUpdateException.class,
            () -> cpoUpdateService.updateCpoServiceWithPayment(getCpoUpdateServiceRequest()));
        assertEquals("CPO",exception.getServer(),"Server should be CPO");
    }

    @Test
    void updateCpoServiceWithPaymentWhenResourceNotAccesibleShouldThrowCpoUpdateSection() {
        Mockito.when(restTemplateCpo.exchange(anyString(),eq(HttpMethod.POST),Mockito.any(),eq(
            ResponseEntity.class))).thenThrow(new ResourceAccessException(""));
        Mockito.when(idamService.getSecurityTokens()).thenReturn(idamTokenResponse);
        Mockito.when(authTokenGenerator.generate()).thenReturn(MOCK_S2S_TOKEN);
        CpoUpdateException exception = assertThrows(CpoUpdateException.class,
            () -> cpoUpdateService.updateCpoServiceWithPayment(getCpoUpdateServiceRequest()));
        assertEquals("CPO",exception.getServer(),"Server should be CPO");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,exception.getStatus(),"Status should be service unavailble");
    }

    @Test
    void updateCpoServiceWithPaymentWhenServiceAuthGeneratorIsDownShouldThrowCpoUpdateSection() {
        Mockito.when(idamService.getSecurityTokens()).thenReturn(idamTokenResponse);
        Mockito.when(authTokenGenerator.generate())
            .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));
        CpoUpdateException exception = assertThrows(CpoUpdateException.class,
            () -> cpoUpdateService.updateCpoServiceWithPayment(getCpoUpdateServiceRequest()));
        assertEquals(S2S_SERVER,exception.getServer(),"Server should be S2S");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,exception.getStatus(),"Status should be service unavailable");
    }

    @Test
    void updateCpoServiceWithPaymentWhenServiceAuthGeneratorNotAccesibleShouldThrowCpoUpdateSection() {
        Mockito.when(idamService.getSecurityTokens()).thenReturn(idamTokenResponse);
        Mockito.when(authTokenGenerator.generate()).thenThrow(new ResourceAccessException(""));
        CpoUpdateException exception = assertThrows(CpoUpdateException.class,
            () -> cpoUpdateService.updateCpoServiceWithPayment(getCpoUpdateServiceRequest()));
        assertEquals(S2S_SERVER,exception.getServer(),"Server should be S2S");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,exception.getStatus(),"Status should be service unavailble");
    }

    @Test
    void whenRetryIsExceedsMaxTryExceededExceptionIsThrown() {
        MaxTryExceededException exception = assertThrows(MaxTryExceededException.class,
            () -> cpoUpdateService.recover(
                 new CpoUpdateException(S2S_SERVER,HttpStatus.SERVICE_UNAVAILABLE,
                 mock(Throwable.class)), getCpoUpdateServiceRequest()));
        assertEquals(S2S_SERVER,exception.getServer(),"Server should be S2S");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,exception.getStatus(),"Status should be service unavailable");
    }

    private HttpEntity<CpoUpdateServiceRequest> getHttpEntity() {
        MultiValueMap<String, String> inputHeaders = new LinkedMultiValueMap<>();
        inputHeaders.put("content-type", Arrays.asList("application/json"));
        inputHeaders.put("Authorization", Arrays.asList("Bearer " + idamTokenResponse.getAccessToken()));
        inputHeaders.put("ServiceAuthorization", Arrays.asList(MOCK_S2S_TOKEN));
        CpoUpdateServiceRequest cpoUpdateServiceRequest = getCpoUpdateServiceRequest();
        return new HttpEntity<CpoUpdateServiceRequest>(cpoUpdateServiceRequest,inputHeaders);
    }

    private CpoUpdateServiceRequest getCpoUpdateServiceRequest() {
        return CpoUpdateServiceRequest.CpoUpdateServiceRequest()
            .action("Case Submit")
            .caseId(Long.valueOf(123))
            .orderReference("2021-11223344556")
            .responsibleParty("Jane Doe")
            .build();
    }

}
