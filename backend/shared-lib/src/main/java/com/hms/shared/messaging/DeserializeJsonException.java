package com.hms.shared.messaging;

public class DeserializeJsonException extends Exception {
    public DeserializeJsonException(String message) {
        super(message);
    }

    public DeserializeJsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
