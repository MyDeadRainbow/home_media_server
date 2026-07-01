package com.hms.acquisition.datamine.exception;

public class NoSearchResultException extends DatamineException {
    public NoSearchResultException(String message) {
        super(message);
    }

    public NoSearchResultException(String message, Throwable cause) {
        super(message, cause);
    }
}
