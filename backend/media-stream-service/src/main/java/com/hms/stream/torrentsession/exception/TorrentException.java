package com.hms.stream.torrentsession.exception;

public class TorrentException extends Exception {
    public TorrentException(String message) {
        super(message);
    }

    public TorrentException(String message, Throwable cause) {
        super(message, cause);
    }    
}
