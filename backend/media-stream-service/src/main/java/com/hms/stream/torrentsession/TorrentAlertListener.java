package com.hms.stream.torrentsession;

import java.lang.reflect.Method;
import java.util.List;

import com.frostwire.jlibtorrent.AlertListener;
import com.frostwire.jlibtorrent.Sha1Hash;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.TorrentAlert;

public class TorrentAlertListener implements AlertListener {
    private final Sha1Hash torrentHash;
    private final List<TorrentAlertHandler<?>> alertHandlers;

    public TorrentAlertListener(Sha1Hash torrentHash, List<TorrentAlertHandler<?>> alertHandlers) {
        this.torrentHash = torrentHash;
        this.alertHandlers = alertHandlers;
    }

    protected Sha1Hash getTorrentHash() {
        return torrentHash;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void alert(Alert<?> alert) {
        if (alert instanceof TorrentAlert<?> ta) {
            if (!ta.handle().infoHash().equals(torrentHash)) {
                return; // Ignore alerts for other torrents
            }
            for (TorrentAlertHandler<?> handler : alertHandlers) {
                if (handler.type.isAssignableFrom(alert.getClass())) {
                    ((TorrentAlertHandler<TorrentAlert<?>>) handler).handle(ta);
                }
            }
        }
    }

    @Override
    public int[] types() {
        return null; // Listen to all alert types
    }

    // private void Torrent
}
