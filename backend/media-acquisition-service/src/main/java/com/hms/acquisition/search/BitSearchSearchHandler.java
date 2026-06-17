package com.hms.acquisition.search;

import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Playwright.CreateOptions;

import io.mikael.urlbuilder.UrlBuilder;

public class BitSearchSearchHandler implements SseEmitterHandler<String> {

    private final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(BitSearchSearchHandler.class);

    // https://bitsearch.eu/search?q=The+land+before+time&sortBy=relevance&page=1&category=2
    private final String BIT_SEARCH_BASE_URL = "https://bitsearch.eu/";
    private final String BIT_SEARCH_SEARCH_PATH = "search";
    private final String QUERY_PARAM = "q";
    private final String SORT_BY_PARAM = "sortBy";
    private final String SORT_BY_VALUE = "relevance";
    private final String PAGE_PARAM = "page";
    private final String CATEGORY_PARAM = "category";
    private final String MOVIE_CATEGORY_VALUE = "2";
    private final String TV_SHOW_CATEGORY_VALUE = "3";

    @Override
    public void handle(SseEmitter emitter, String query) throws Exception {
        try (Playwright playwright = Playwright.create(new CreateOptions());
                Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                Page page = browser.newPage();) {

            Response response = page.navigate(UrlBuilder.fromString(BIT_SEARCH_BASE_URL)
                    .withPath(BIT_SEARCH_SEARCH_PATH)
                    .addParameter(QUERY_PARAM, query)
                    .addParameter(SORT_BY_PARAM, SORT_BY_VALUE)
                    .addParameter(PAGE_PARAM, "1")
                    .addParameter(CATEGORY_PARAM, MOVIE_CATEGORY_VALUE)
                    .toString());

            switch (response.status()) {
                case 200:
                    break; // OK
                case 403:
                    LOG.error("Access to BitSearch is forbidden. Check if the site is blocked in your region.");
                    return;
                default:
                    LOG.error("Failed to access BitSearch. HTTP status: {}", response.status());
                    return;
            }
            // Wait for the search results to load
            page.waitForSelector("main.max-w-7xl > div.space-y-4 > div");

            // Get the list of search result entries
            List<ElementHandle> listItems = page.querySelectorAll("main.max-w-7xl > div.space-y-4 > div");

            for (ElementHandle item : listItems) {
                String title = item.querySelector("div > div > div > h3 > a").innerText();
                String sourceUrl = BIT_SEARCH_BASE_URL + item.querySelector("div > div > div > h3 > a").getAttribute("href");
                String magnetLink = item.querySelector("div > div > a[href*=magnet]").getAttribute("href");
                String size = item.querySelector("div > div > div > span > i.fa-download + span").innerText();
                String seeders = item.querySelector("div > div > div > span > i.fa-arrow-up + span").innerText();
                String leechers = item.querySelector("div > div > div > span > i.fa-arrow-down + span").innerText();
                emitter.send(new SearchResponse(title, magnetLink, "BitSearch", sourceUrl, size, seeders, leechers));
            }
        }
    }

}
