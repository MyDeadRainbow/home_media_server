// package com.hms.acquisition.importmedia.magnetfinder;

// import java.net.URI;
// import java.util.List;

// import org.slf4j.Logger;

// import com.hms.acquisition.importmedia.ImportMediaEntry;
// import com.microsoft.playwright.Browser;
// import com.microsoft.playwright.BrowserType;
// import com.microsoft.playwright.ElementHandle;
// import com.microsoft.playwright.Page;
// import com.microsoft.playwright.Playwright;
// import com.microsoft.playwright.Playwright.CreateOptions;
// import com.microsoft.playwright.Response;

// import io.mikael.urlbuilder.UrlBuilder;

// public class LimeTorrentMagnetFinder extends MagnetFinder {

//     private final Logger LOG = org.slf4j.LoggerFactory.getLogger(LimeTorrentMagnetFinder.class);

//     private final String LIME_TORRENT_BASE_URL = "https://limetorrent.net/";
//     private final String LIME_TORRENT_STORE_URL = "https://limetorrent.store/";
//     private final String LIME_TORRENT_SEARCH_PATH = "search/";

//     private final String CATEGORY_PARAM = "catname";
//     private final String MOVIE_CATEGORY_VALUE = "movies";
//     private final String TV_SHOW_CATEGORY_VALUE = "tv";

//     private final String QUERY_PARAM = "q";

//     private final String ORDER_BY_PARAM = "orderby";
//     private final String ORDER_BY_VALUE = "DESC";

//     private final String ORDER_PARAM = "order";
//     private final String ORDER_VALUE = "seeders";

//     @Override
//     protected String findBestMagnetLink(ImportMediaEntry entry) throws Exception {
//         try (Playwright playwright = Playwright.create(new CreateOptions());
//                 Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
//                 Page page = browser.newPage();) {

//             String url = UrlBuilder.fromString(LIME_TORRENT_BASE_URL)
//                     .withPath(LIME_TORRENT_SEARCH_PATH)
//                     .addParameter(CATEGORY_PARAM, MOVIE_CATEGORY_VALUE)
//                     .addParameter(QUERY_PARAM, entry.title())
//                     .addParameter(ORDER_BY_PARAM, ORDER_BY_VALUE)
//                     .addParameter(ORDER_PARAM, ORDER_VALUE)
//                     .toString();

//             Response response = page.navigate(url);

//             switch (response.status()) {
//                 case 200:
//                     break; // OK
//                 case 403:
//                     LOG.error("Access to LimeTorrent is forbidden. Check if the site is blocked in your region.");
//                     return null;
//                 default:
//                     LOG.error("Failed to access LimeTorrent. HTTP status: {}", response.status());
//                     return null;
//             }
//             // Wait for the search results to load
//             page.waitForSelector("table.table2 > tbody.torsearch > tr");

//             // Get the list of search result entries
//             List<ElementHandle> listItems = page.querySelectorAll("table.table2 > tbody.torsearch > tr");

//             // Find the best candidate based on seeders
//             ElementHandle bestCandidate = listItems.stream()
//                     .map((e) -> {
//                         String title = entry.title().toLowerCase();
//                         String elementTitle = e.querySelector("td.tdleft > div.tt-name > a[class=openPopup]").innerText().toLowerCase();

//                         int score = 0;
//                         for (int i = 0; i < elementTitle.length(); i++) {
//                             char c = elementTitle.charAt(i);
//                             char t = i < title.length() ? title.charAt(i) : '\0';
//                             if (c == t) {
//                                 score++;
//                             } else {
//                                 score--;
//                             }
//                         }

//                         return new ElementSearchScore(score, e);
//                     })
//                     .sorted((a, b) -> {
//                         return Integer.compare(b.score(), a.score()); // Sort in descending order
//                     }).map(ElementSearchScore::element).findFirst().orElse(null);

//             if (bestCandidate == null) {
//                 return null;
//             }
//             URI pageUrl = URI.create(bestCandidate.querySelector("td.tdleft > div.tt-name > a[class=openPopup]").getAttribute("href"));
//             UrlBuilder urlBuilder = UrlBuilder.fromString(LIME_TORRENT_STORE_URL).withPath(pageUrl.getPath());

//             String pageUrlString = urlBuilder.toString();
//             page.navigate(pageUrlString);
            
//             page.waitForSelector("a[href*=magnet]");
//             String magnetLink = page.querySelector("a[href*=magnet]").getAttribute("href");

//             if (magnetLink != null && !magnetLink.isEmpty()) {
//                 return magnetLink;
//             }
//         }
//         return null;
//     }

// }
