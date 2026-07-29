package com.hms.stream.torrentinfo;

import com.hms.shared.json.ImportMediaStatus;

public record TorrentInfoUpdate(String infoHash, long downloadedSize, long uploadSpeed, long downloadSpeed, int numPeers, ImportMediaStatus importMediaStatus) {
    
}
