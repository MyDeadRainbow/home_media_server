package com.hms.stream.importmedia.pipeline;

import com.hms.stream.importmedia.ImportMediaEntry;

@FunctionalInterface
public interface ImportMediaHandler {
    /**
     * Handles the import of media based on the provided entry.
     *
     * @param entry The media import entry containing details about the media to be imported.
     */
    ImportMediaEntry handle(ImportMediaEntry entry) throws Exception;
}
