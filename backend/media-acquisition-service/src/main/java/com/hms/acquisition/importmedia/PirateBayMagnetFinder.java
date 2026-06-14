package com.hms.acquisition.importmedia;

import java.io.IOException;
import java.util.List;

import com.hms.acquisition.importmedia.pipeline.ImportMediaHandler;
import com.hms.shared.scrape.SearchParameter;
import com.hms.shared.scrape.SearchUrl;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Playwright.CreateOptions;

public class PirateBayMagnetFinder implements ImportMediaHandler{

    private static final String PIRATE_BAY_BASE_URL = "https://thepiratebay.org";
    private static final String PIRATE_BAY_SEARCH_URL = PIRATE_BAY_BASE_URL + "/search.php?";
    private static final String HD_MOVIE_CATEGORY = "207";
    private static final String HD_TV_SHOW_CATEGORY = "208";

    private static final SearchUrl pirateBaySearchUrl = new SearchUrl(PIRATE_BAY_SEARCH_URL);

    public PirateBayMagnetFinder() {
    }

    public String findBestMagnetLink(ImportMediaEntry entry) {

        try (Playwright playwright = Playwright.create(new CreateOptions());
                Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                Page page = browser.newPage();) {

            page.navigate(pirateBaySearchUrl.getSearchUrl(
                    new SearchParameter("q", entry.title()),
                    new SearchParameter("cat", HD_MOVIE_CATEGORY)));

            // Wait for the search results to load
            page.waitForSelector("li.list-entry");

            // Get the list of search result entries
            List<ElementHandle> listItems = page.querySelectorAll("li.list-entry");

            // Find the best candidate based on seeders
            ElementHandle bestCandidate = listItems.stream().sorted((a, b) -> {
                int aSeeds = Integer.parseInt(a.querySelector("span.item-seed").innerText());
                int bSeeds = Integer.parseInt(b.querySelector("span.item-seed").innerText());
                return Integer.compare(bSeeds, aSeeds); // Sort in descending order
            }).findFirst().orElse(null);

            if (bestCandidate == null) {
                return null;
            }

            String pageUrl = PIRATE_BAY_BASE_URL
                    + bestCandidate.querySelector("span.item-title a").getAttribute("href");
            page.navigate(pageUrl);
            page.waitForSelector("a[href*=magnet]");
            String magnetLink = page.querySelector("a[href*=magnet]").getAttribute("href");

            if (magnetLink != null && !magnetLink.isEmpty()) {
                return magnetLink;
            }
        }
        return null;
    }

    @Override
    public ImportMediaEntry handle(ImportMediaEntry entry) {
        try {
            String magnetLink = findBestMagnetLink(entry);
            if (magnetLink != null) {
                entry = new ImportMediaEntry(entry.id(), entry.title(), entry.status(), magnetLink);
            } else {
                entry = new ImportMediaEntry(entry.id(), entry.title(), ImportMediaStatus.MAGNET_NOT_FOUND, null);
            }
            entry.update();
        } catch (Exception e) {
            // Log the error or handle it accordingly
            e.printStackTrace();
        }
        return entry;
    }
}
