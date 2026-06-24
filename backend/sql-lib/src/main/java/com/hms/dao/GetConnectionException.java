package com.hms.dao;

public class GetConnectionException extends Exception {
    public GetConnectionException(String message) {
        super(message);
    }

    public GetConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
