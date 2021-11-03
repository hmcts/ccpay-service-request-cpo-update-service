package uk.gov.hmcts.reform.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CpoUpdateException extends RuntimeException {

    private String server;

    private HttpStatus status;

    public static final long serialVersionUID = 333297431;

    public CpoUpdateException(String server, HttpStatus status, Throwable cause) {
        super(cause);
        this.server = server;
        this.status = status;
    }
}
