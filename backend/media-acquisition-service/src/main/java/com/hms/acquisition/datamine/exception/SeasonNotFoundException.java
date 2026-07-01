package com.hms.acquisition.datamine.exception;

public class SeasonNotFoundException extends DatamineException {
    public SeasonNotFoundException(String message) {
        super(message);
    }

    public SeasonNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
