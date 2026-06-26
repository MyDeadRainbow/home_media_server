package com.hms.acquisition.datamine;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.hms.shared.messaging.datamining.DataMineRequest;
import com.hms.shared.messaging.metadata.MetaData;
import com.hms.shared.pipline.Handler;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public abstract class DatamineHandler<T extends DataMineRequest> implements Handler<T> {
    String getImdbUrl() {
        return "https://www.imdb.com/";
    }

    @Override
    public T handle(T entry) throws Exception {
        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                Page page = browser.newPage()) {

            page.navigate(getImdbUrl());
            ElementHandle searchBox = page.querySelector("input#suggestion-search");
            searchBox.fill(entry.title());

            page.waitForSelector("ul.react-autosuggest__suggestions-list[role=listbox] > li[role=option]");
            List<ElementHandle> suggestions = page
                    .querySelectorAll("ul.react-autosuggest__suggestions-list[role=listbox] > li[role=option]");

            ElementHandle matchingSuggestion = suggestions.stream()
                    .filter(suggestion -> suggestion.querySelector("div.searchResult__constTitle").innerText()
                            .toLowerCase().contains(entry.title().toLowerCase()))
                    .findFirst()
                    .orElseThrow(() -> {
                        return new Exception("No matching series found for title: " + entry.title());
                    });
            matchingSuggestion.click();
            
            return entryHandler(page, entry);
        }
    }

    protected abstract T entryHandler(Page page, T entry) throws Exception;
}
