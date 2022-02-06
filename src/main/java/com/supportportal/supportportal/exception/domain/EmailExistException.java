package com.supportportal.supportportal.exception.domain;

public class EmailExistException extends RuntimeException {
    public EmailExistException(String msg) {
        super(msg);
    }
}
