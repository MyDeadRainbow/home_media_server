package com.hms.acquisition.datamine;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hms.acquisition.datamine.exception.DatamineException;
import com.hms.acquisition.datamine.exception.NoSearchResultException;
import com.hms.shared.media.Title;
import com.hms.shared.messaging.datamining.DataMineRequest;
import com.hms.shared.pipline.Handler;
// import com.microsoft.playwright.Browser;
// import com.microsoft.playwright.BrowserContext;
// import com.microsoft.playwright.BrowserType;
// import com.microsoft.playwright.ElementHandle;
// import com.microsoft.playwright.Page;
// import com.microsoft.playwright.Playwright;
// import com.microsoft.playwright.options.LoadState;
// import com.microsoft.playwright.options.ViewportSize;

// public abstract class DatamineHandler<T extends Title, R> {
//     // https://www.imdb.com/search/title/?title=backrooms&title_type=feature,tv_series,tv_episode

//     public static void main(String[] args) throws Exception {
//         // DatamineMovieHandler handler = new DatamineMovieHandler();
//         // Movie entry = new Movie("123", "tt0111161", "The Shawshank Redemption");
//         // DatamineSeriesHandler handler = new DatamineSeriesHandler();
//         // DataMineRequest.Series entry = new DataMineRequest.Series("123", "The
//         // Office", List.of(
//         // new DataMineRequest.Season("123", 1, List.of(
//         // new DataMineRequest.Episode("123", "tt0944947", "Pilot", 1, "The Office", 1),
//         // new DataMineRequest.Episode("123", "tt0944948", "Diversity Day", 2, "The
//         // Office", 1))),
//         // new DataMineRequest.Season("123", 2, List.of(
//         // new DataMineRequest.Episode("123", "tt0944949", "The Dundies", 1, "The
//         // Office", 2),
//         // new DataMineRequest.Episode("123", "tt0944950", "Sexual Harassment", 2, "The
//         // Office", 2)))));
//         // handler.handle(entry);
//     }

//     protected final String SEARCH_PATH = "search/title/";
//     protected final String TITLE_PARAM = "title";
//     protected final String TITLE_TYPE_PARAM = "title_type";
//     protected final String TITLE_TYPE_MOVIE = "feature";
//     protected final String TITLE_TYPE_TV_MOVIE = "tv_movie";
//     protected final String TITLE_TYPE_SERIES = "tv_series";
//     protected final String TITLE_TYPE_MINI_SERIES = "tv_miniseries";
//     protected final String IMDB_BASE_URL = "https://www.imdb.com/";

//     // @Override
//     /**
//      * Handle with a stealth Playwright headless browser
//      */
//     public R handle(T entry) throws DatamineException {
//         try (Playwright playwright = Playwright.create();
//                 Browser browser = playwright.chromium()
//                         .launch(new BrowserType.LaunchOptions().setHeadless(true).setSlowMo(1000)
//                                 .setArgs(List.of("--disable-blink-features=AutomationControlled",
//                                         "--disable-infobars",
//                                         "--no-sandbox",
//                                         "--disable-dev-shm-usage")));) {
//             try (BrowserContext context = browser.newContext(new Browser.NewContextOptions()
//                     .setUserAgent(
//                             "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
//                     .setViewportSize(new ViewportSize(1920, 1080))
//                     .setLocale("en-US")
//                     .setTimezoneId("America/New_York"));) {
//                 context.addInitScript("""
//                             // Hide webdriver flag
//                             Object.defineProperty(navigator, 'webdriver', {
//                                 get: () => undefined
//                             });

//                             // Fake plugins
//                             Object.defineProperty(navigator, 'plugins', {
//                                 get: () => [1, 2, 3, 4, 5]
//                             });

//                             // Fake languages
//                             Object.defineProperty(navigator, 'languages', {
//                                 get: () => ['en-US', 'en']
//                             });

//                             // Add chrome object
//                             window.chrome = {
//                                 runtime: {},
//                                 loadTimes: function() {},
//                                 csi: function() {},
//                                 app: {},
//                             };
//                         """);
//                 try (Page page = context.newPage()) {
//                     return handle(page, entry);
//                 }
//             }
//         }
//     }

//     private R handle(Page page, T entry) throws DatamineException {
//         // need to use the find url on imdb, then check that list of loaded reults
//         // instead of trying to use the popup suggestions box

//         page.navigate(searchUrl(entry));
//         page.waitForLoadState(LoadState.NETWORKIDLE);
//         // page.waitForSelector("ul > li.ipc-metadata-list-summary-item");

//         List<ElementHandle> searchResults = page
//                 .querySelectorAll("ul > li.ipc-metadata-list-summary-item");

//         ElementHandle matchingResult = searchResults.stream()
//                 .filter(result -> {
//                     ElementHandle titleElement = result.querySelector("h4.ipc-title__text");
//                     String titleText = titleElement != null ? titleElement.innerText() : "";
//                     Matcher match = Pattern.compile("[0-9]{1,2}\\. (.+)").matcher(titleText);
//                     if (match.find()) {
//                         titleText = match.group(1);
//                     }
//                     return titleElement != null
//                             && titleText.toLowerCase().contains(entry.title().toLowerCase());
//                 })
//                 .findFirst()
//                 .orElseThrow(() -> {
//                     return new NoSearchResultException("No matching series found for title: " + entry.title());
//                 });
//         ElementHandle linkElement = matchingResult.querySelector("a.ipc-title-link-wrapper");
//         linkElement.click();
//         page.waitForLoadState(LoadState.NETWORKIDLE);
//         return entryHandler(page, entry);
//     }

//     protected abstract R entryHandler(Page page, T entry) throws DatamineException;

//     protected abstract String searchUrl(T entry);
// }
