package com.hms.stream.importmedia;

public enum ImportMediaStatus {
    PENDING,
    IN_PROGRESS,
    MAGNET_FOUND,
    MAGNET_NOT_FOUND,
    MAGNET_FETCH_FAILED,
    PAUSED,
    RESUME,
    COMPLETED,
    FAILED,
    DELETED
}
