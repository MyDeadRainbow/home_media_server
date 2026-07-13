package com.hms.stream.torrentinfo;

public record TorrentInfoUpdate(String infoHash, long downloadedSize, long uploadSpeed, long downloadSpeed, int numPeers) {
    
}
