package com.hms.shared.dao;

public class DBFileNotFoundException extends Exception {
    public DBFileNotFoundException(String message) {
        super(message);
    }

    public DBFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
