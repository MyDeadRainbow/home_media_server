package com.hms.stream.torrentinfo;

import com.hms.stream.importmedia.ImportMediaStatus;

public record TorrentInfoUpdate(String infoHash, long downloadedSize, long uploadSpeed, long downloadSpeed, int numPeers, ImportMediaStatus importMediaStatus) {
    
}
