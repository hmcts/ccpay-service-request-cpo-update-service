package uk.gov.hmcts.reform.services;

import uk.gov.hmcts.reform.dtos.responses.IdamTokenResponse;

public interface IdamService {
    IdamTokenResponse getSecurityTokens();
}
