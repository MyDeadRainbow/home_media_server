package com.hms.acquisition.search;

import java.util.List;

public record SearchResponseList(String query, List<SearchResponse> searchResponses) {
    // public static SearchResponseList of(SearchResponse... searchResponses) {
    //     return new SearchResponseList("", List.of(searchResponses));
    // }

    public SearchResponseList(String query) {
        this(query, List.of());
    }

    public SearchResponseList with(SearchResponse... additionalResponses) {
        SearchResponse[] combined = new SearchResponse[searchResponses.size() + additionalResponses.length];
        System.arraycopy(searchResponses.toArray(new SearchResponse[0]), 0, combined, 0, searchResponses.size());
        System.arraycopy(additionalResponses, 0, combined, searchResponses.size(), additionalResponses.length);
        return new SearchResponseList(query, List.of(combined));
    }

    public SearchResponseList with(List<SearchResponse> additionalResponses) {
        SearchResponse[] combined = new SearchResponse[searchResponses.size() + additionalResponses.size()];
        System.arraycopy(searchResponses.toArray(new SearchResponse[0]), 0, combined, 0, searchResponses.size());
        System.arraycopy(additionalResponses.toArray(new SearchResponse[0]), 0, combined, searchResponses.size(), additionalResponses.size());
        return new SearchResponseList(query, List.of(combined));
    }
    // public static SearchResponseList of(List<SearchResponse> searchResponses, SearchResponse... additionalResponses) {
    //     SearchResponse[] combined = new SearchResponse[searchResponses.size() + additionalResponses.length];
    //     System.arraycopy(searchResponses.toArray(new SearchResponse[0]), 0, combined, 0, searchResponses.size());
    //     System.arraycopy(additionalResponses, 0, combined, searchResponses.size(), additionalResponses.length);
    //     return new SearchResponseList("", List.of(combined));
    // }

    // public static SearchResponseList of(SearchResponseList searchResponses, SearchResponse... additionalResponses) {
    //     return of(searchResponses.searchResponses(), additionalResponses);
    // }
}
