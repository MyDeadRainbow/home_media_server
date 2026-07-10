package com.hms.acquisition.search;

import java.io.IOException;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.mikael.urlbuilder.UrlBuilder;

public class LimeTorrentSearchHandler
        implements SseEmitterHandler<SearchRequest> {

    private final Logger LOG = org.slf4j.LoggerFactory.getLogger(LimeTorrentSearchHandler.class);

    private final String LIME_TORRENT_BASE_URL = "https://limetorrent.store/";
    private final String LIME_TORRENT_SEARCH_PATH = "search/";

    private final String CATEGORY_PARAM = "catname";
    private final String MOVIE_CATEGORY_VALUE = "movies";
    private final String TV_SHOW_CATEGORY_VALUE = "tv";

    private final String QUERY_PARAM = "q";

    private final String ORDER_BY_PARAM = "orderby";
    private final String ORDER_BY_VALUE = "DESC";

    private final String ORDER_PARAM = "order";
    private final String ORDER_VALUE = "seeders";

    private final String USER_AGENT_HEADER = "User-Agent";
    private final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";

    @Override
    public void handle(SseEmitter emitter, SearchRequest request) throws Exception {
        String categoryValue = switch (request.category()) {
            case MOVIE -> MOVIE_CATEGORY_VALUE;
            case SERIES -> TV_SHOW_CATEGORY_VALUE;
            default -> throw new IllegalArgumentException("Unsupported media category: " + request.category());
        };

        String url = UrlBuilder.fromString(LIME_TORRENT_BASE_URL)
                .withPath(LIME_TORRENT_SEARCH_PATH)
                .addParameter(CATEGORY_PARAM, categoryValue)
                .addParameter(QUERY_PARAM, request.query())
                .addParameter(ORDER_BY_PARAM, ORDER_BY_VALUE)
                .addParameter(ORDER_PARAM, ORDER_VALUE)
                .toString();

        Connection session = Jsoup.newSession();
        session.url(url);
        session.header(USER_AGENT_HEADER, USER_AGENT);

        Document doc = session.post();
        doc.select("table.table2 > tbody.torsearch > tr").forEach(item -> {
            String title = item.select("td.tdleft > div.tt-name > a[class=openPopup]").text();
            String sourceUrl = LIME_TORRENT_BASE_URL
                    + item.select("td.tdleft > div.tt-name > a[class=openPopup]").attr("href");
            String size = item.select("td.tdnormal + .tdnormal").text();
            String seeders = item.select("td.tdseed").text();
            String leechers = item.select("td.tdleech").text();

            String magnetLink = null;
            String magnetLinkPage = item.select("td.tdleft > div.tt-name > a[class=openPopup]")
                    .attr("href");                    
            session.url(magnetLinkPage);
            session.header(USER_AGENT_HEADER, USER_AGENT);
            try {
                Document magnetDoc = session.get();
                magnetLink = magnetDoc.select("a[href*=magnet]").attr("href");
            } catch (IOException e) {
                LOG.error("Error fetching magnet link from LimeTorrent", e);
            }

            try {
                emitter.send(
                        new SearchResponse(title, magnetLink, "LimeTorrent", sourceUrl, size, seeders, leechers));
            } catch (Exception e) {
                LOG.error("Error sending search response to emitter", e);
            }
        });
    }
}
