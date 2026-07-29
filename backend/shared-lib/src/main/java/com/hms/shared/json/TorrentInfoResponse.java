package com.hms.shared.json;

import com.hms.shared.messaging.JsonSerializable;

public record TorrentInfoResponse(String name, String infoHash, int queuePosition, long totalSize, long downloadedSize,
        long uploadSpeed, long downloadSpeed, int numPeers, ImportMediaStatus importMediaStatus) implements JsonSerializable {
}
