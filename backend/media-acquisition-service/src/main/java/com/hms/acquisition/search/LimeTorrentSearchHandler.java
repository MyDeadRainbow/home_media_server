package com.hms.acquisition.search;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;

import com.hms.acquisition.importmedia.ImportMediaEntry;
import com.hms.acquisition.importmedia.magnetfinder.ElementSearchScore;
import com.hms.acquisition.importmedia.magnetfinder.LimeTorrentMagnetFinder;
import com.hms.shared.pipline.Handler;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Playwright.CreateOptions;

import io.mikael.urlbuilder.UrlBuilder;

public class LimeTorrentSearchHandler implements Handler<SearchResponsePipelineEntry> {

    private final Logger LOG = org.slf4j.LoggerFactory.getLogger(LimeTorrentSearchHandler.class);

    private final String LIME_TORRENT_BASE_URL = "https://limetorrent.net/";
    private final String LIME_TORRENT_STORE_URL = "https://limetorrent.store/";
    private final String LIME_TORRENT_SEARCH_PATH = "search/";

    private final String CATEGORY_PARAM = "catname";
    private final String MOVIE_CATEGORY_VALUE = "movies";
    private final String TV_SHOW_CATEGORY_VALUE = "tv";

    private final String QUERY_PARAM = "q";

    private final String ORDER_BY_PARAM = "orderby";
    private final String ORDER_BY_VALUE = "DESC";

    private final String ORDER_PARAM = "order";
    private final String ORDER_VALUE = "seeders";

    @Override
    public SearchResponsePipelineEntry handle(SearchResponsePipelineEntry entry) throws Exception {
        try (Page page = entry.getBrowser().newPage();) {

            String url = UrlBuilder.fromString(LIME_TORRENT_BASE_URL)
                    .withPath(LIME_TORRENT_SEARCH_PATH)
                    .addParameter(CATEGORY_PARAM, MOVIE_CATEGORY_VALUE)
                    .addParameter(QUERY_PARAM, entry.getSearchResponseList().query())
                    .addParameter(ORDER_BY_PARAM, ORDER_BY_VALUE)
                    .addParameter(ORDER_PARAM, ORDER_VALUE)
                    .toString();

            Response response = page.navigate(url);

            switch (response.status()) {
                case 200:
                    break; // OK
                case 403:
                    LOG.error("Access to LimeTorrent is forbidden. Check if the site is blocked in your region.");
                    return null;
                default:
                    LOG.error("Failed to access LimeTorrent. HTTP status: {}", response.status());
                    return null;
            }
            // Wait for the search results to load
            page.waitForSelector("table.table2 > tbody.torsearch > tr");

            // Get the list of search result entries
            List<ElementHandle> listItems = page.querySelectorAll("table.table2 > tbody.torsearch > tr");

            List<SearchResponse> searchResults = listItems.stream().map(item -> {
                try (Page magPage = entry.getBrowser().newPage();) {
                    String title = item.querySelector("td.tdleft > div.tt-name > a[class=openPopup]").innerText();
                    String sourceUrl = LIME_TORRENT_BASE_URL
                            + item.querySelector("td.tdleft > div.tt-name > a[class=openPopup]").getAttribute("href");
    
                    URI pageUrl = URI.create(item.querySelector("td.tdleft > div.tt-name > a[class=openPopup]")
                            .getAttribute("href"));
                    UrlBuilder urlBuilder = UrlBuilder.fromString(LIME_TORRENT_STORE_URL).withPath(pageUrl.getPath());
    
                    String pageUrlString = urlBuilder.toString();
                    magPage.navigate(pageUrlString);
    
                    magPage.waitForSelector("a[href*=magnet]");
                    String magnetLink = magPage.querySelector("a[href*=magnet]").getAttribute("href");
    
                    return new SearchResponse(title, magnetLink, "LimeTorrent", sourceUrl);
                }
            }).toList();
            entry.setSearchResponseList(entry.getSearchResponseList().with(searchResults));

            return entry;
        }
    }

}
