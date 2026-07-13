package com.hms.stream.torrentinfo;

import java.util.List;

import org.springframework.stereotype.Service;

import com.frostwire.jlibtorrent.SessionManager;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.TorrentStatus;
import com.hms.stream.torrentsession.TorrentSession;

@Service
public class TorrentInfoService {

    public TorrentInfoService() {
    }

    public List<TorrentInfoResponse> getTorrentInfo() {
        try (TorrentSession torrentSession = TorrentSession.getInstance()) {
            List<TorrentInfoResponse> responses = torrentSession.getTorrentHandles().stream().map(handle -> {
                // Process each torrent handle to extract information
                // For example, you can create a TorrentInfoResponse object for each handle
                // and add it to a list to return.
                TorrentStatus status = handle.status();
                status.totalDone();
                status.totalWanted();
                TorrentInfo info = handle.torrentFile();
                return new TorrentInfoResponse(
                        handle.name(),
                        handle.infoHash().toString(),
                        handle.queuePosition(),
                        status.totalWanted(),
                        status.totalDone(),
                        status.uploadRate(),
                        status.downloadRate(),
                        status.numPeers());
            }).sorted((ti1, ti2) -> Integer.compare(ti1.queuePosition(), ti2.queuePosition())).toList();

            return responses;
        }
    }

    public boolean pauseTorrent(String infoHash) {
        try (TorrentSession torrentSession = TorrentSession.getInstance()) {
            return torrentSession.pauseTorrent(infoHash);
        }
    }

    public boolean resumeTorrent(String infoHash) {
        try (TorrentSession torrentSession = TorrentSession.getInstance()) {
            return torrentSession.resumeTorrent(infoHash);
        }
    }

    public boolean deleteTorrent(String infoHash) {
        try (TorrentSession torrentSession = TorrentSession.getInstance()) {
            return torrentSession.deleteTorrent(infoHash);
        }
    }
}
