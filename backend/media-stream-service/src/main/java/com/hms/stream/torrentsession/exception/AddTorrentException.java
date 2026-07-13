package com.hms.stream.torrentsession.exception;

public class AddTorrentException extends TorrentException {
    public AddTorrentException(String message) {
        super(message);
    }

    public AddTorrentException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
