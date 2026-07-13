package com.hms.stream.torrentinfo;

public record TorrentInfoResponse(String name, String infoHash, int queuePosition, long totalSize, long downloadedSize, long uploadSpeed, long downloadSpeed, int numPeers) {
    
}
