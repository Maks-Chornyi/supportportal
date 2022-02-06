package com.supportportal.supportportal.exception.domain;

public class UsernameExistException extends RuntimeException {
    public UsernameExistException(String msg) {
        super(msg);
    }
}
