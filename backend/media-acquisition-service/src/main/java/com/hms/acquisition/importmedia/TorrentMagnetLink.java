package com.hms.acquisition.torrent;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.libtorrent4j.AlertListener;
import org.libtorrent4j.FileStorage;
import org.libtorrent4j.Priority;
import org.libtorrent4j.SessionManager;
import org.libtorrent4j.TorrentFlags;
import org.libtorrent4j.TorrentInfo;
import org.libtorrent4j.alerts.AddTorrentAlert;
import org.libtorrent4j.alerts.Alert;
import org.libtorrent4j.alerts.AlertType;
import org.libtorrent4j.alerts.BlockFinishedAlert;
// import org.libtorrent4j.AddTorrentParams;
// import org.libtorrent4j.AlertListener;
// import org.libtorrent4j.FileStorage;
// import org.libtorrent4j.Priority;
// import org.libtorrent4j.SessionManager;
// import org.libtorrent4j.TorrentFlags;
// import org.libtorrent4j.TorrentInfo;
// import org.libtorrent4j.alerts.AddTorrentAlert;
// import org.libtorrent4j.alerts.Alert;
// import org.libtorrent4j.alerts.AlertType;
// import org.libtorrent4j.alerts.BlockFinishedAlert;
// import org.libtorrent4j.swig.torrent_flags_t;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hms.acquisition.importmedia.ImportMediaEntry;
import com.hms.acquisition.importmedia.ImportMediaHandler;
import com.hms.acquisition.importmedia.ImportMediaStatus;
import com.hms.shared.dao.DBFileNotFoundException;
import com.hms.shared.dao.GetConnectionException;

// import bt.Bt;
// import bt.data.Storage;
// import bt.data.file.FileSystemStorage;
// import bt.dht.DHTConfig;
// import bt.dht.DHTModule;
// import bt.metainfo.TorrentFile;
// import bt.runtime.BtClient;
// import bt.runtime.Config;
// import bt.torrent.fileselector.FilePriority;
// import bt.torrent.fileselector.FilePrioritySelector;

public class TorrentMagnetLink implements ImportMediaHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TorrentMagnetLink.class);

    private static final Path TORRENT_DOWNLOAD_FOLDER = Path.of("torrents");
    private static final Path TEMP_FOLDER = Path.of("temp");

    // private String magnetLink;

    public TorrentMagnetLink() {
        // this.magnetLink = magnetLink;
        try {
            Files.createDirectories(TORRENT_DOWNLOAD_FOLDER);
            Files.createDirectories(TEMP_FOLDER);
        } catch (Exception e) {
            LOG.error("Failed to create torrent directory", e);
            return;
        }
    }

    // @Override
    // public void run() {

    // }

    @Override
    public ImportMediaEntry handle(ImportMediaEntry entry) {
        try {
            Files.createDirectories(TORRENT_DOWNLOAD_FOLDER);
            Files.createDirectories(TEMP_FOLDER);
        } catch (Exception e) {
            LOG.error("Failed to create torrent directory", e);
            return entry;
        }
        String magnetLink = entry.magnetLink();
        if (magnetLink == null || magnetLink.isEmpty()) {
            LOG.error("Magnet link is null or empty for entry: {}", entry);
            return entry;
        }
        // libtorrent4j does not work in docker container due to missing dependencies.
        // Will need to switch to a different torrent library or implement torrent
        // downloading in a separate service that runs on the host machine and can
        // access the torrent download directory directly.
        final SessionManager s = new SessionManager();

        final CountDownLatch signal = new CountDownLatch(1);

        final ImportMediaAlertListener listener = new ImportMediaAlertListener(entry);
        s.addListener(listener);

        s.start();

        // AddTorrentParams params = AddTorrentParams.parseMagnetUri(magnetLink);

        byte[] magnetData = null;
        for (int i = 0; i < 3; i++) {
            magnetData = s.fetchMagnet(magnetLink, 30, TEMP_FOLDER.toFile());
            if (magnetData != null) {
                break;
            }
        }
        if (magnetData == null) {
            LOG.error("Failed to fetch magnet data after 3 attempts");
            entry = new ImportMediaEntry(entry.id(), entry.title(), ImportMediaStatus.MAGNET_FETCH_FAILED, null);
            return listener.getUpdatedEntry();
        }

        TorrentInfo info = new TorrentInfo(magnetData);
        FileStorage storage = info.files();

        Priority[] priorities = Priority.array(Priority.IGNORE, info.numFiles());
        for (int i = 0; i < priorities.length; i++) {
            String filePath = storage.filePath(i);
            if (filePath.endsWith(".mkv") || filePath.endsWith(".mp4") ||
                    filePath.endsWith(".avi")) {
                priorities[i] = Priority.DEFAULT;
            }
        }
        s.download(info, TORRENT_DOWNLOAD_FOLDER.toFile(), null, priorities, null,
                TorrentFlags.SEQUENTIAL_DOWNLOAD);

        // s.download(info, TORRENT_DOWNLOAD_FOLDER.toFile());

        try {
            signal.await();
        } catch (InterruptedException e) {
            LOG.error("Torrent download interrupted", e);
            Thread.currentThread().interrupt();
        }

        s.stop();
        return listener.getUpdatedEntry();
    }

    class ImportMediaAlertListener implements AlertListener {
        private final ImportMediaEntry entry;
        private ImportMediaEntry updatedEntry;

        public ImportMediaAlertListener(ImportMediaEntry entry) {
            this.entry = entry;
            this.updatedEntry = entry;
        }

        public ImportMediaEntry getUpdatedEntry() {
            return updatedEntry;
        }

        @Override
        public int[] types() {
            return null; // Listen to all alert types
        }

        @Override
        public void alert(Alert<?> alert) {
            AlertType type = alert.type();

            switch (type) {
                case ADD_TORRENT:
                    LOG.info("Torrent added");
                    ((AddTorrentAlert) alert).handle().resume();
                    break;
                case BLOCK_FINISHED:
                    BlockFinishedAlert a = (BlockFinishedAlert) alert;
                    int p = (int) (a.handle().status().progress() * 100);
                    if (p % 10 == 0) {
                        LOG.info("Progress: {}% for torrent name: {}", p, a.handle().getName());
                    }
                    break;
                case TORRENT_FINISHED:
                    LOG.info("Torrent download finished for: {}",
                            ((org.libtorrent4j.alerts.TorrentFinishedAlert) alert).handle().getName());
                    updatedEntry = new ImportMediaEntry(entry.id(), entry.title(), ImportMediaStatus.TORRENT_DOWNLOADED,
                            entry.magnetLink());
                    try {
                        updatedEntry.update();
                    } catch (Exception e) {
                        LOG.error("Failed to update entry status to TORRENT_DOWNLOADED", e);
                    }
                    break;
            }
        }
    }
}
