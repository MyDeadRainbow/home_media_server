package com.hms.stream.importmedia;

public enum ImportMediaStatus {
    PENDING,
    QUEUED,
    IN_PROGRESS,
    MAGNET_FOUND,
    MAGNET_NOT_FOUND,
    MAGNET_FETCH_FAILED,
    TORRENT_DOWNLOADING,
    TORRENT_DOWNLOADED,
    COMPLETED,
    FAILED
}
