package com.hms.stream.torrentinfo;

import com.hms.stream.importmedia.ImportMediaStatus;

public record TorrentInfoResponse(String name, String infoHash, int queuePosition, long totalSize, long downloadedSize,
        long uploadSpeed, long downloadSpeed, int numPeers, ImportMediaStatus importMediaStatus) {

}
