package com.hms.acquisition.search;

import java.io.IOException;
import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.hms.shared.pipline.Handler;
import com.hms.shared.scrape.SearchParameter;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Playwright.CreateOptions;

import io.mikael.urlbuilder.UrlBuilder;

public class PirateBaySearchHandler
        implements Handler<SearchResponsePipelineEntry>, SseEmitterHandler<SearchRequest> {
    private final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PirateBaySearchHandler.class);

    private final String PIRATE_BAY_BASE_URL = "https://thepiratebay.org";
    private final String PIRATE_BAY_SEARCH_PATH = "/search.php";

    private final String CATEGORY_PARAM = "cat";
    private final String HD_MOVIE_CATEGORY = "207";
    private final String HD_TV_SHOW_CATEGORY = "208";

    private final String QUERY_PARAM = "q";

    @Override
    public SearchResponsePipelineEntry handle(SearchResponsePipelineEntry entry) throws Exception {
        try (Page page = entry.getBrowser().newPage();) {

            Response response = page.navigate(UrlBuilder.fromString(PIRATE_BAY_BASE_URL)
                    .withPath(PIRATE_BAY_SEARCH_PATH)
                    .addParameter(QUERY_PARAM, entry.getSearchResponseList().query())
                    .addParameter(CATEGORY_PARAM, HD_MOVIE_CATEGORY)
                    .toString());

            switch (response.status()) {
                case 200:
                    break; // OK
                case 403:
                    LOG.error("Access to Pirate Bay is forbidden. Check if the site is blocked in your region.");
                    return entry;
                default:
                    LOG.error("Failed to access Pirate Bay. HTTP status: {}", response.status());
                    return entry;
            }
            // Wait for the search results to load
            page.waitForSelector("li.list-entry");

            // Get the list of search result entries
            List<ElementHandle> listItems = page.querySelectorAll("li.list-entry");

            List<SearchResponse> searchResults = listItems.stream().map(item -> {
                String title = item.querySelector("span.item-title a").innerText();
                String sourceUrl = PIRATE_BAY_BASE_URL + item.querySelector("span.item-title a").getAttribute("href");
                String magnetLink = item.querySelector("a[href*=magnet]").getAttribute("href");
                String size = item.querySelector("span.item-size").innerText();
                String seeders = item.querySelector("span.item-seeds").innerText();
                String leechers = item.querySelector("span.item-leechs").innerText();
                return new SearchResponse(title, magnetLink, "Pirate Bay", sourceUrl, size, seeders, leechers);
            }).toList();

            entry.setSearchResponseList(entry.getSearchResponseList().with(searchResults));
            return entry;
        }
    }

    @Override
    public void handle(SseEmitter emitter, SearchRequest request) throws Exception {
        try (Playwright playwright = Playwright.create(new CreateOptions());
                Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                Page page = browser.newPage();) {

            String categoryValue = switch (request.category()) {
                case MOVIE -> HD_MOVIE_CATEGORY;
                case SERIES -> HD_TV_SHOW_CATEGORY;
                default -> throw new IllegalArgumentException("Unsupported media category: " + request.category());
            };

            Response response = page.navigate(UrlBuilder.fromString(PIRATE_BAY_BASE_URL)
                    .withPath(PIRATE_BAY_SEARCH_PATH)
                    .addParameter(QUERY_PARAM, request.query())
                    .addParameter(CATEGORY_PARAM, categoryValue)
                    .toString());

            switch (response.status()) {
                case 200:
                    break; // OK
                case 403:
                    LOG.error("Access to Pirate Bay is forbidden. Check if the site is blocked in your region.");
                    return;
                default:
                    LOG.error("Failed to access Pirate Bay. HTTP status: {}", response.status());
                    return;
            }
            // Wait for the search results to load
            page.waitForSelector("li.list-entry");

            // Get the list of search result entries
            List<ElementHandle> listItems = page.querySelectorAll("li.list-entry");

            for (ElementHandle item : listItems) {
                String title = item.querySelector("span.item-title a").innerText();
                String sourceUrl = PIRATE_BAY_BASE_URL + item.querySelector("span.item-title a").getAttribute("href");
                String magnetLink = item.querySelector("a[href*=magnet]").getAttribute("href");
                String size = item.querySelector("span.item-size").innerText();
                String seeders = item.querySelector("span.item-seed").innerText();
                String leechers = item.querySelector("span.item-leech").innerText();
                emitter.send(new SearchResponse(title, magnetLink, "Pirate Bay", sourceUrl, size, seeders, leechers));
            }
        }
    }

}
