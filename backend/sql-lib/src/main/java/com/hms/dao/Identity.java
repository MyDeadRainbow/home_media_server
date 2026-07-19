package com.hms.dao;

import io.github.robsonkades.uuidv7.UUIDv7;

public record Identity(String id) {    
    public static Identity generate() {
        return new Identity(UUIDv7.randomUUID().toString());
    }
}
