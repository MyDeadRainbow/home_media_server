package com.hms.stream.torrentinfo;

import com.hms.stream.importmedia.ImportMediaStatus;

public record MediaItemInfoUpdate(String mediaItemId,
                                    long fileSize,
                                    long bytesDownloaded,
                                    // long uploadRate,
                                    long deltaBytesDownloaded,
                                    long requiredDownloadRate,
                                    // int numPeers,
                                    ImportMediaStatus importMediaStatus) {
    
}
