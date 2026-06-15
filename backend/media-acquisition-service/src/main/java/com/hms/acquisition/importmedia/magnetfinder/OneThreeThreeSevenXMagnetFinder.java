package com.hms.acquisition.importmedia.magnetfinder;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;

import com.hms.acquisition.importmedia.ImportMediaEntry;
import com.hms.acquisition.importmedia.pipeline.ImportMediaHandler;
import com.hms.shared.scrape.SearchParameter;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Playwright.CreateOptions;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.SameSiteAttribute;

import io.github.kihdev.playwright.stealth4j.Stealth4j;
import io.mikael.urlbuilder.UrlBuilder;

public class OneThreeThreeSevenXMagnetFinder extends MagnetFinder {

    private final Logger LOG = org.slf4j.LoggerFactory.getLogger(OneThreeThreeSevenXMagnetFinder.class);
    private final String BASE_URL = "https://1337x.to";
    private final String SEARCH_PATH = "/category-search";
    private final String MOVIE_SUFFIX_PATH = "/Movies/1/";
    private final String SERIES_SUFFIX_PATH = "/TV/1/";

    @Override
    protected String findBestMagnetLink(ImportMediaEntry entry) throws Exception {
        Page page = null;
        try (Playwright playwright = Playwright.create(new CreateOptions());
                Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                        .setHeadless(false) // Headless mode triggers strict Turnstile checks
                        .setArgs(Arrays.asList(
                                "--disable-blink-features=AutomationControlled", // Hides navigator.webdriver
                                "--start-maximized",
                                "--no-sandbox",
                                "--disable-infobars",
                                "--window-size=1920,1080")));) {

            var context = Stealth4j.newStealthContext(browser);
            // Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
            // .setUserAgent(
            // "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like
            // Gecko) Chrome/124.0.0.0 Safari/537.36")
            // .setViewportSize(1920, 1080)
            // .setDeviceScaleFactor(1)
            // .setJavaScriptEnabled(true)
            // .setHasTouch(false)
            // .setIsMobile(false);

            // BrowserContext context = browser.newContext(contextOptions);
            // context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: ()
            // => undefined})");
            var cookie = new Cookie("cf_clearance",
                    "hIvsy9h7w.NUxAGLLT22tBQqVT7MMbt8TRKSXS9l27o-1781512468-1.2.1.1-EZ7xi4R5jKw.9V0GSBhmqHqdK59g2H4V4c4B4QZMyKyjIr0RXqV869P0MATsbXftIHafBEa0guBFwl1Bc0BQSeqMA5l8OosIuJ76.lybNGikk_9zz6GD8h9sYn1EUar9kzirCryEBm1fLWMqXbH9cyTkKYmgarg6Am0JY5Dfez_8XE4je2bfJK5MnuRXP..UiUBeteVbgyqeXMSfBOW1NvQ5S_UyrjHGYWgvQmOEndVpEk2H4UlEnFETBGDJ1IYHsBDlLWPrhgeugEtIi58_ZqC6OG0QwDP8TU9QcW9j3Ap_NDAz4MGRInzYddxiCq6.YdkIa5p1A7RIM4wEBB9gXBTw7lXrfrtzo6i9Gyk0syViKDoxjh3LJ939I9KwidBDAnrW4dtCtXqtNWVB9Kg6dM.28BPQj0LkPhrv_H.JfwM");
            cookie.setDomain(BASE_URL);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setSameSite(SameSiteAttribute.NONE);
            cookie.setPartitionKey(BASE_URL);

            context.addCookies(List.of(cookie));
            page = context.newPage();
            UrlBuilder urlBuilder = UrlBuilder.fromString(BASE_URL)
                    .withPath(SEARCH_PATH + "/" + entry.title() + MOVIE_SUFFIX_PATH);
            Response response = page
                    .navigate(urlBuilder.toString());

            switch (response.status()) {
                case 200:
                    break; // OK
                case 403:

                    LOG.error("Access to 1337x is forbidden. Check if the site is blocked in your region. Response: "
                            + response.text());
                    return null;
                default:
                    LOG.error("Failed to access 1337x. HTTP status: {}", response.status());
                    return null;
            }

            // Wait for the search results to load
            page.waitForSelector("table.table-list.table.table-responsive.table-striped > tbody > tr");

            // Get the list of search result entries
            List<ElementHandle> listItems = page
                    .querySelectorAll("table.table-list.table.table-responsive.table-striped > tbody > tr");

            // Find the best candidate based on name match. seeders are ordered by default
            // by 1337x, so we can just take the first one that matches the title.
            ElementHandle bestCandidate = listItems.stream()
                    .map((e) -> {
                        String title = entry.title().toLowerCase();
                        String elementTitle = e.querySelector("td.coll-1.name > a").innerText().toLowerCase();

                        int score = 0;
                        for (int i = 0; i < elementTitle.length(); i++) {
                            char c = elementTitle.charAt(i);
                            if (c == title.charAt(Math.min(i, title.length() - 1))) {
                                score++;
                            } else {
                                score--;
                            }
                        }

                        return new ElementSearchScore(score, e);
                    })
                    .sorted((a, b) -> {
                        return Integer.compare(b.score(), a.score()); // Sort in descending order
                    }).map(ElementSearchScore::element).findFirst().orElse(null);

            if (bestCandidate == null) {
                return null;
            }

            LOG.info("Best candidate found: {}", bestCandidate.querySelector("td.coll-1.name > a").innerText());

            String pageUrl = BASE_URL + bestCandidate.querySelector("td.coll-1.name > a").getAttribute("href");
            page.navigate(pageUrl);

            page.waitForSelector("a[href^='magnet:']");
            String magnetLink = page.querySelector("a[href^='magnet:']").getAttribute("href");
            return magnetLink;
        } finally {
            if (page != null) {
                page.context().browser().close();
                page.context().close();
                page.close();
            }
        }
    }
}
