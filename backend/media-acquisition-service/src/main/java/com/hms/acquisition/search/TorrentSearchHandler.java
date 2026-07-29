package com.hms.acquisition.search;

import java.util.List;

import com.hms.shared.json.SearchResponse;

@FunctionalInterface
public interface TorrentSearchHandler {
    public List<SearchResponse> searchTorrentsJson(SearchRequest data) throws Exception;
}
