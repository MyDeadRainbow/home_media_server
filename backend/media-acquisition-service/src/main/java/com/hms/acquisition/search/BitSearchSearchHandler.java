package com.hms.acquisition.search;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.mikael.urlbuilder.UrlBuilder;

public class BitSearchSearchHandler implements SseEmitterHandler<SearchRequest> {

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

    private final String USER_AGENT_HEADER = "User-Agent";
    private final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";

    @Override
    public void handle(SseEmitter emitter, SearchRequest request) throws Exception {
        
        String categoryValue = switch (request.category()) {
                case MOVIE -> MOVIE_CATEGORY_VALUE;
                case SERIES -> TV_SHOW_CATEGORY_VALUE;
                default -> throw new IllegalArgumentException("Unsupported media category: " + request.category());
            };
        String url = UrlBuilder.fromString(BIT_SEARCH_BASE_URL)
                    .withPath(BIT_SEARCH_SEARCH_PATH)
                    .addParameter(QUERY_PARAM, request.query())
                    .addParameter(SORT_BY_PARAM, SORT_BY_VALUE)
                    .addParameter(PAGE_PARAM, "1")
                    .addParameter(CATEGORY_PARAM, categoryValue)
                    .toString();
        
        Connection session = Jsoup.newSession();
            
        session.url(url);
        session.header(USER_AGENT_HEADER, USER_AGENT);

        Document doc = session.get();
        doc.select("main.max-w-7xl > div.space-y-4 > div").forEach(item -> {
            String title = item.select("div > div > div > h3 > a").text();
            String sourceUrl = BIT_SEARCH_BASE_URL + item.select("div > div > div > h3 > a").attr("href");
            String magnetLink = item.select("div > div > a[href*=magnet]").attr("href");
            String size = item.select("div > div > div > span > i.fa-download + span").text();
            String seeders = item.select("div > div > div > span > i.fa-arrow-up + span").text();
            String leechers = item.select("div > div > div > span > i.fa-arrow-down + span").text();
            try {
                emitter.send(new SearchResponse(title, magnetLink, "BitSearch", sourceUrl, size, seeders, leechers));
            } catch (Exception e) {
                LOG.error("Error sending search response to emitter", e);
            }
        });
    }
}
