package com.hms.acquisition.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class TorrentSearchService {
    private final Logger LOG = LoggerFactory.getLogger(TorrentSearchService.class);

    public void searchTorrents(String query, SseEmitter emitter) {
        SseEmitterOrchestrator<String> orchestrator = SseEmitterOrchestrator
                .<String>builder()
                .withEmitter(emitter)
                .withData(query)
                .addHandler(new PirateBaySearchHandler())
                .addHandler(new LimeTorrentSearchHandler())
                .addHandler(new BitSearchSearchHandler())
                .build();

        orchestrator.execute();
        // Pipeline<SearchResponsePipelineEntry> pipeline =
        // Pipeline.<SearchResponsePipelineEntry>builder()
        // .addHandler(new SearchPirateBayHandler())
        // .addHandler(new LimeTorrentSearchHandler())
        // .onError((entry, e) -> {
        // // Handle errors during the search process
        // LOG.error("Error during torrent search: " + e.getMessage(), e);
        // })
        // .build();
        // return pipeline.handle(new SearchResponsePipelineEntry(playwright, browser,
        // new SearchResponseList(query)))
        // .getSearchResponseList();
    }
}
