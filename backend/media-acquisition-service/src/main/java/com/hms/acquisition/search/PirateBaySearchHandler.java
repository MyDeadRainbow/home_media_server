package com.hms.acquisition.search;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.hms.shared.media.MediaCategory;

import io.mikael.urlbuilder.UrlBuilder;

public class PirateBaySearchHandler implements SseEmitterHandler<SearchRequest> {

    public static void main(String[] args) throws Exception {
        new PirateBaySearchHandler().handle(new SseEmitter(), new SearchRequest("The Flash", MediaCategory.MOVIE));
    }

    private final String PIRATE_BAY_API_URL = "https://apibay.org";
    private final String SEARCH_PATH = "/q.php";

    private final String CATEGORY_PARAM = "cat";
    private final String HD_MOVIE_CATEGORY = "207";
    private final String HD_TV_SHOW_CATEGORY = "208";

    private final String QUERY_PARAM = "q";

    private final String REFERER_HEADER = "Referer";
    private final String PIRATE_BAY_REFERER = "https://thepiratebay.org/";

    private final String USER_AGENT_HEADER = "User-Agent";
    private final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";

    @Override
    public void handle(SseEmitter emitter, SearchRequest data) throws Exception {

        try (HttpClient client = HttpClient.newHttpClient();) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(UrlBuilder.fromString(PIRATE_BAY_API_URL)
                            .withPath(SEARCH_PATH)
                            .addParameter(QUERY_PARAM, data.query())
                            .addParameter(CATEGORY_PARAM,
                                    data.category() == MediaCategory.MOVIE ? HD_MOVIE_CATEGORY : HD_TV_SHOW_CATEGORY)
                            .toUri())
                    .header(REFERER_HEADER, PIRATE_BAY_REFERER)
                    .header(USER_AGENT_HEADER, USER_AGENT)
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            // Process the response body and extract search results
                            JsonArray results = JsonParser.parseString(response.body()).getAsJsonArray();
                            for (int i = 0; i < results.size(); i++) {
                                var result = results.get(i).getAsJsonObject();
                                String title = result.get("name").getAsString();
                                String infoHash = result.get("info_hash").getAsString();
                                String magnetLink = createMagnetForInfoHash(infoHash, title);
                                String sourceUrl = PIRATE_BAY_REFERER + "/description.php?id="
                                        + result.get("id").getAsString();
                                String size = toSize(result.get("size").getAsString());
                                String seeders = result.get("seeders").getAsString();
                                String leechers = result.get("leechers").getAsString();

                                try {
                                    // System.out.println(
                                    //         new SearchResponse(title, magnetLink, sourceUrl, "Pirate Bay", size,
                                    //                 seeders, leechers));
                                    emitter.send(new SearchResponse(title, magnetLink, "Pirate Bay", sourceUrl, size,
                                            seeders, leechers));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            System.err.println("Failed to access Pirate Bay. HTTP status: " + response.statusCode());
                        }
                        return null;
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    }).join();
        }
    }

    /**
     * magnet:?xt=urn:btih:'+ih+'&dn='+encodeURIComponent(name) + print_trackers()
     * let tr = '&tr=' + encodeURIComponent('udp://tracker.opentrackr.org:1337');
     * // tr += '&tr=' +
     * encodeURIComponent('udp://tracker.openbittorrent.com:6969/announce');
     * tr += '&tr=' + encodeURIComponent('udp://open.stealth.si:80/announce');
     * tr += '&tr=' +
     * encodeURIComponent('udp://tracker.torrent.eu.org:451/announce');
     * tr += '&tr=' + encodeURIComponent('udp://tracker.bittor.pw:1337/announce');
     * tr += '&tr=' +
     * encodeURIComponent('udp://public.popcorn-tracker.org:6969/announce');
     * tr += '&tr=' + encodeURIComponent('udp://tracker.dler.org:6969/announce');
     * tr += '&tr=' + encodeURIComponent('udp://exodus.desync.com:6969');
     * tr += '&tr=' + encodeURIComponent('udp://open.demonii.com:1337/announce');
     * // tr += '&tr=' + encodeURIComponent('udp://tracker2.dler.com/announce');
     * // tr += '&tr=' +
     * encodeURIComponent('udp://tracker3.dler.com:2710/announce');
     * tr += '&tr=' + encodeURIComponent('udp://glotorrents.pw:6969/announce');
     * tr += '&tr=' + encodeURIComponent('udp://tracker.coppersurfer.tk:6969');
     * tr += '&tr=' + encodeURIComponent('udp://torrent.gresille.org:80/announce');
     * tr += '&tr=' + encodeURIComponent('udp://p4p.arenabg.com:1337');
     * tr += '&tr=' + encodeURIComponent('udp://tracker.internetwarriors.net:1337')
     * 
     * @param infoHash
     * @param name
     * @return
     */
    private String createMagnetForInfoHash(String infoHash, String name) {
        // UrlBuilder magnetUrl = UrlBuilder.fromString("magnet:?xt=urn:btih:" +
        // infoHash)
        // .addParameter("dn", name)
        // .addParameter("tr", "udp://tracker.opentrackr.org:1337")
        // .addParameter("tr", "udp://open.stealth.si:80/announce")
        // .addParameter("tr", "udp://tracker.torrent.eu.org:451/announce")
        // .addParameter("tr", "udp://tracker.bittor.pw:1337/announce")
        // .addParameter("tr", "udp://public.popcorn-tracker.org:6969/announce")
        // .addParameter("tr", "udp://tracker.dler.org:6969/announce")
        // .addParameter("tr", "udp://exodus.desync.com:6969/announce")
        // .addParameter("tr", "udp://open.demonii.com:1337/announce")
        // .addParameter("tr", "udp://glotorrents.pw:6969/announce")
        // .addParameter("tr", "udp://tracker.coppersurfer.tk:6969/announce")
        // .addParameter("tr", "udp://torrent.gresille.org:80/announce")
        // .addParameter("tr", "udp://p4p.arenabg.com:1337/announce")
        // .addParameter("tr", "udp://tracker.internetwarriors.net:1337/announce");
        // return magnetUrl.toString();

        MagnetBuilder magnetBuilder = MagnetBuilder.newBuilder(infoHash)
                .addParameter("dn", name)
                .addParameter("tr", "udp://tracker.opentrackr.org:1337")
                .addParameter("tr", "udp://open.stealth.si:80/announce")
                .addParameter("tr", "udp://tracker.torrent.eu.org:451/announce")
                .addParameter("tr", "udp://tracker.bittor.pw:1337/announce")
                .addParameter("tr", "udp://public.popcorn-tracker.org:6969/announce")
                .addParameter("tr", "udp://tracker.dler.org:6969/announce")
                .addParameter("tr", "udp://exodus.desync.com:6969/announce")
                .addParameter("tr", "udp://open.demonii.com:1337/announce")
                .addParameter("tr", "udp://glotorrents.pw:6969/announce")
                .addParameter("tr", "udp://tracker.coppersurfer.tk:6969/announce")
                .addParameter("tr", "udp://torrent.gresille.org:80/announce")
                .addParameter("tr", "udp://p4p.arenabg.com:1337/announce")
                .addParameter("tr", "udp://tracker.internetwarriors.net:1337/announce");
        return magnetBuilder.build();
    }

    /**
     * function print_size(size, f) {
     * let e='';
     * if (f) {
     * e='&nbsp;(' + size + ' Bytes)';
     * }
     * if (size >= 1125899906842624) return
     * round_to_precision(size/1125899906842624, 0.01) + '&nbsp;PiB' + e;
     * if (size >= 1099511627776) return round_to_precision(size/1099511627776,
     * 0.01) + '&nbsp;TiB' + e;
     * if (size >= 1073741824) return round_to_precision(size/1073741824, 0.01) +
     * '&nbsp;GiB' + e;
     * if (size >= 1048576) return round_to_precision(size/1048576, 0.01) +
     * '&nbsp;MiB' + e;
     * if (size >= 1024) return round_to_precision(size/1024, 0.01) + '&nbsp;KiB' +
     * e;
     * return size+'&nbsp;B';
     * }
     * function round_to_precision(x, precision) {
     * let y = +x + (precision === undefined ? 0.5 : precision/2);
     * // Fix 1.4000000000000001 like results from rounding.
     * let sz = y - (y % (precision === undefined ? 1 : +precision)) + '';
     * if (sz.indexOf('.') == -1) return sz;
     * else return sz.substring(0, sz.indexOf('.')+3);
     * }
     * 
     * @param sizeInBytes
     * @return
     */
    private String toSize(String sizeInBytes) {
        long size = Long.parseLong(sizeInBytes);
        if (size >= 1125899906842624L) {
            return roundToPrecision((double) size / 1125899906842624L, 0.01) + " PiB";
        }
        if (size >= 1099511627776L) {
            return roundToPrecision((double) size / 1099511627776L, 0.01) + " TiB";
        }
        if (size >= 1073741824L) {
            return roundToPrecision((double) size / 1073741824L, 0.01) + " GiB";
        }
        if (size >= 1048576L) {
            return roundToPrecision((double) size / 1048576L, 0.01) + " MiB";
        }
        if (size >= 1024L) {
            return roundToPrecision((double) size / 1024L, 0.01) + " KiB";
        }
        return size + " B";
    }

    private String roundToPrecision(double x, double precision) {
        double y = x + (precision == 0 ? 0.5 : precision / 2);
        double rounded = y - (y % (precision == 0 ? 1 : precision));
        return String.format("%.2f", rounded);
    }
}

// public class PirateBaySearchHandler
// implements Handler<SearchResponsePipelineEntry>,
// SseEmitterHandler<SearchRequest> {
// private final org.slf4j.Logger LOG =
// org.slf4j.LoggerFactory.getLogger(PirateBaySearchHandler.class);

// private final String PIRATE_BAY_BASE_URL = "https://thepiratebay.org";
// private final String PIRATE_BAY_SEARCH_PATH = "/search.php";

// private final String CATEGORY_PARAM = "cat";
// private final String HD_MOVIE_CATEGORY = "207";
// private final String HD_TV_SHOW_CATEGORY = "208";

// private final String QUERY_PARAM = "q";

// @Override
// public SearchResponsePipelineEntry handle(SearchResponsePipelineEntry entry)
// throws Exception {
// try (Page page = entry.getBrowser().newPage();) {

// Response response = page.navigate(UrlBuilder.fromString(PIRATE_BAY_BASE_URL)
// .withPath(PIRATE_BAY_SEARCH_PATH)
// .addParameter(QUERY_PARAM, entry.getSearchResponseList().query())
// .addParameter(CATEGORY_PARAM, HD_MOVIE_CATEGORY)
// .toString());

// switch (response.status()) {
// case 200:
// break; // OK
// case 403:
// LOG.error("Access to Pirate Bay is forbidden. Check if the site is blocked in
// your region.");
// return entry;
// default:
// LOG.error("Failed to access Pirate Bay. HTTP status: {}", response.status());
// return entry;
// }
// // Wait for the search results to load
// page.waitForSelector("li.list-entry");

// // Get the list of search result entries
// List<ElementHandle> listItems = page.querySelectorAll("li.list-entry");

// List<SearchResponse> searchResults = listItems.stream().map(item -> {
// String title = item.querySelector("span.item-title a").innerText();
// String sourceUrl = PIRATE_BAY_BASE_URL + item.querySelector("span.item-title
// a").getAttribute("href");
// String magnetLink =
// item.querySelector("a[href*=magnet]").getAttribute("href");
// String size = item.querySelector("span.item-size").innerText();
// String seeders = item.querySelector("span.item-seeds").innerText();
// String leechers = item.querySelector("span.item-leechs").innerText();
// return new SearchResponse(title, magnetLink, "Pirate Bay", sourceUrl, size,
// seeders, leechers);
// }).toList();

// entry.setSearchResponseList(entry.getSearchResponseList().with(searchResults));
// return entry;
// }
// }

// @Override
// public void handle(SseEmitter emitter, SearchRequest request) throws
// Exception {
// try (Playwright playwright = Playwright.create(new CreateOptions());
// Browser browser = playwright.chromium().launch(new
// BrowserType.LaunchOptions().setHeadless(true));
// Page page = browser.newPage();) {

// String categoryValue = switch (request.category()) {
// case MOVIE -> HD_MOVIE_CATEGORY;
// case SERIES -> HD_TV_SHOW_CATEGORY;
// default -> throw new IllegalArgumentException("Unsupported media category: "
// + request.category());
// };

// Response response = page.navigate(UrlBuilder.fromString(PIRATE_BAY_BASE_URL)
// .withPath(PIRATE_BAY_SEARCH_PATH)
// .addParameter(QUERY_PARAM, request.query())
// .addParameter(CATEGORY_PARAM, categoryValue)
// .toString());

// switch (response.status()) {
// case 200:
// break; // OK
// case 403:
// LOG.error("Access to Pirate Bay is forbidden. Check if the site is blocked in
// your region.");
// return;
// default:
// LOG.error("Failed to access Pirate Bay. HTTP status: {}", response.status());
// return;
// }
// // Wait for the search results to load
// page.waitForSelector("li.list-entry");

// // Get the list of search result entries
// List<ElementHandle> listItems = page.querySelectorAll("li.list-entry");

// for (ElementHandle item : listItems) {
// String title = item.querySelector("span.item-title a").innerText();
// String sourceUrl = PIRATE_BAY_BASE_URL + item.querySelector("span.item-title
// a").getAttribute("href");
// String magnetLink =
// item.querySelector("a[href*=magnet]").getAttribute("href");
// String size = item.querySelector("span.item-size").innerText();
// String seeders = item.querySelector("span.item-seed").innerText();
// String leechers = item.querySelector("span.item-leech").innerText();
// emitter.send(new SearchResponse(title, magnetLink, "Pirate Bay", sourceUrl,
// size, seeders, leechers));
// }
// }
// }

// }
