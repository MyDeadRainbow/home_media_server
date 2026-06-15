package com.hms.acquisition.search;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;

public class SearchResponsePipelineEntry {
    private final Playwright playwright;
    private final Browser browser;
    private SearchResponseList searchResponseList;

    public SearchResponsePipelineEntry(Playwright playwright, Browser browser, SearchResponseList searchResponseList) {
        this.playwright = playwright;
        this.browser = browser;
        this.searchResponseList = searchResponseList;
    }

    public Playwright getPlaywright() {
        return playwright;
    }

    public Browser getBrowser() {
        return browser;
    }

    public SearchResponseList getSearchResponseList() {
        return searchResponseList;
    }

    public void setSearchResponseList(SearchResponseList searchResponseList) {
        this.searchResponseList = searchResponseList;
    }
}
