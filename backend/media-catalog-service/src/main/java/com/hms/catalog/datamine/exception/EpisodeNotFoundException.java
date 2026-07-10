package com.hms.catalog.datamine.exception;

public class EpisodeNotFoundException extends DatamineException {
    public EpisodeNotFoundException(String message) {
        super(message);
    }

    public EpisodeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }    
}
