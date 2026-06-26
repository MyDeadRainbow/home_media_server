package com.hms.shared.messaging;

public class SerializeJsonException extends Exception {
    public SerializeJsonException(String message) {
        super(message);
    }

    public SerializeJsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
