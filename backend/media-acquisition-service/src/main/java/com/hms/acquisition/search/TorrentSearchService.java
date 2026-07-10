package com.hms.acquisition.search;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class TorrentSearchService {

    public void searchTorrents(SearchRequest request, SseEmitter emitter) {
        SseEmitterOrchestrator<SearchRequest> orchestrator = SseEmitterOrchestrator
                .<SearchRequest>builder()
                .withEmitter(emitter)
                .withData(request)
                .addHandler(new PirateBaySearchHandler())
                .addHandler(new LimeTorrentSearchHandler())
                .addHandler(new BitSearchSearchHandler())
                .build();

        orchestrator.execute();
    }
}
