package com.hms.acquisition.datamine.exception;

public abstract class DatamineException extends Exception {

    public DatamineException(String message) {
        super(message);
    }

    public DatamineException(String message, Throwable cause) {
        super(message, cause);
    }
}
