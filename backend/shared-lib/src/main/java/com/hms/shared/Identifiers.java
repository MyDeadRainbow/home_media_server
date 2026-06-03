package com.hms.shared;

import java.util.UUID;

public final class Identifiers {

    private Identifiers() {
        // Utility class.
    }

    public static String newRequestId() {
        return UUID.randomUUID().toString();
    }
}
