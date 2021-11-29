package uk.gov.hmcts.reform.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;


@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Builder(builderMethodName = "CpoUpdateServiceRequest")
@Data
public class CpoUpdateServiceRequest {

    private String action;

    private Long caseId;

    private String orderReference;

    private String responsibleParty;

    public CpoUpdateServiceRequest(@JsonProperty(value = "action", required = true) String action,
                                   @JsonProperty(value = "case_id", required = true) Long caseId,
                                   @JsonProperty(value = "order_reference", required = true) String orderReference,
                                   @JsonProperty(value = "responsible_party", required = true)
                                       String responsibleParty) {
        this.action = action;
        this.caseId = caseId;
        this.orderReference = orderReference;
        this.responsibleParty = responsibleParty;
    }
}
