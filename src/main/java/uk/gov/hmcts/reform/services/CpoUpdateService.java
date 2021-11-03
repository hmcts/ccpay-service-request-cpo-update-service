package uk.gov.hmcts.reform.services;

import uk.gov.hmcts.reform.dtos.requests.CpoUpdateServiceRequest;
import uk.gov.hmcts.reform.exceptions.CpoUpdateException;

public interface CpoUpdateService {
    void updateCpoServiceWithPayment(CpoUpdateServiceRequest cpoUpdateServiceRequest);

    void recover(CpoUpdateException exception, CpoUpdateServiceRequest cpoUpdateServiceRequest);
}
