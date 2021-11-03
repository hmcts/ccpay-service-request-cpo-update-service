package uk.gov.hmcts.reform.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.dtos.responses.IdamTokenResponse;
import uk.gov.hmcts.reform.exceptions.CpoUpdateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class IdamServiceTest {

    @Mock
    private RestTemplate restTemplateIdam;

    @InjectMocks
    private IdamServiceImpl idamService;

    @Test
    void methodGetAccessTokenShouldMakeCallToIdamApi() {
        IdamTokenResponse idamTokenResponse = IdamTokenResponse.idamFullNameRetrivalResponseWith()
                                                    .refreshToken("refresh-token")
                                                    .idToken("id-token")
                                                    .accessToken("access-token")
                                                    .expiresIn("10")
                                                    .scope("openid profile roles")
                                                    .tokenType("type")
                                                    .build();
        Mockito.when(restTemplateIdam.exchange(anyString(), eq(HttpMethod.POST), Mockito.any(), eq(
            IdamTokenResponse.class))).thenReturn(ResponseEntity.ok(idamTokenResponse));
        IdamTokenResponse actualResponse = idamService.getSecurityTokens();
        assertThat(idamTokenResponse).usingRecursiveComparison().isEqualTo(actualResponse);
    }

    @Test
    void methodGetAccessTokenWhenIdamSendsBadRequestShouldThrowCpoUpdateException() {
        Mockito.when(restTemplateIdam.exchange(anyString(), eq(HttpMethod.POST), Mockito.any(), eq(
            IdamTokenResponse.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        CpoUpdateException cpoUpdateException = assertThrows(CpoUpdateException.class,
            () -> idamService.getSecurityTokens());
        assertEquals("IDAM",cpoUpdateException.getServer(),"Server should be IDAM");
    }

    @Test
    void methodGetAccessTokenWhenIdamIsUnavailableShouldThrowCpoUpdateException() {
        Mockito.when(restTemplateIdam.exchange(anyString(), eq(HttpMethod.POST), Mockito.any(), eq(
            IdamTokenResponse.class))).thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));
        CpoUpdateException cpoUpdateException = assertThrows(CpoUpdateException.class,
            () -> idamService.getSecurityTokens());
        assertEquals("IDAM",cpoUpdateException.getServer(),"Server should be IDAM");
    }
}
