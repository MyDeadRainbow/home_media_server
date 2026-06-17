package com.hms.stream.importmedia;

import java.sql.Date;

import com.hms.shared.dao.PrimaryKey;
import com.hms.shared.dao.SQLiteSerializable;

public record ImportMediaEntry(@PrimaryKey String id, String title, ImportMediaStatus status, String magnetLink, Date createdAt, String torrentFolderPath)
        implements SQLiteSerializable {

    @Override
    public String getDbPath() {
        return "media_requests.db";
    }

    @Override
    public String getTableName() {
        return "media_requests";
    }

    ImportMediaEntry withStatus(ImportMediaStatus newStatus) {
        return new ImportMediaEntry(this.id, this.title, newStatus, this.magnetLink, this.createdAt, this.torrentFolderPath);
    }

    ImportMediaEntry withMagnetLink(String newMagnetLink) {
        return new ImportMediaEntry(this.id, this.title, this.status, newMagnetLink, this.createdAt, this.torrentFolderPath);
    }    

    ImportMediaEntry withTorrentFolderPath(String newTorrentFolderPath) {
        return new ImportMediaEntry(this.id, this.title, this.status, this.magnetLink, this.createdAt, newTorrentFolderPath);
    }
}
