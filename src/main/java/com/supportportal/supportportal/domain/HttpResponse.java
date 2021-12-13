package com.supportportal.supportportal.domain;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class HttpResponse {
    private int httpStatusCode;
    private HttpStatus httpStatus;
    private String reason;
    private String message;
}
