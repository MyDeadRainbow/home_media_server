package com.hms.acquisition.importmedia;

@FunctionalInterface
public interface ImportMediaHandler {
    /**
     * Handles the import of media based on the provided entry.
     *
     * @param entry The media import entry containing details about the media to be imported.
     */
    ImportMediaEntry handle(ImportMediaEntry entry) throws Exception;

    // public static ImportMediaEntry executeHandlers(ImportMediaEntry entry, ImportMediaHandler... handlers) {
    //     for (ImportMediaHandler handler : handlers) {
    //         entry = handler.handleImport(entry);
    //     }
    //     return entry;
    // }
}
