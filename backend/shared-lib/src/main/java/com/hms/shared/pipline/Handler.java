package com.hms.shared.pipline;

@FunctionalInterface
public interface Handler<T> {
    /**
     * Handles the processing of the provided entry.
     *
     * @param entry The entry to be processed.
     * @return The processed entry.
     * @throws Exception If an error occurs during processing.
     */
    T handle(T entry) throws Exception;
}
