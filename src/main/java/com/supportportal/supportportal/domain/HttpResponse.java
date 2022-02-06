package com.supportportal.supportportal.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NonNull;
import org.springframework.http.HttpStatus;

import java.util.Date;

@Data
public class HttpResponse {
    @NonNull private final int httpStatusCode;
    @NonNull private final HttpStatus httpStatus;
    @NonNull private final String reason;
    @NonNull private final String message;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM-dd-yyyy hh:mm:ss", timezone = "Africa/Cairo")
    private Date timeStamp = new Date();
}
