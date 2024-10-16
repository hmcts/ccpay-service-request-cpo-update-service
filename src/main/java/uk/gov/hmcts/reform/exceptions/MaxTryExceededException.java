package uk.gov.hmcts.reform.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class MaxTryExceededException  extends RuntimeException {
    private String server;
    private HttpStatusCode status;
    public static final long serialVersionUID = 333297431;

    public MaxTryExceededException(String server, HttpStatusCode status, Throwable cause) {
        super(cause);
        this.server = server;
        this.status = status;
    }
}
