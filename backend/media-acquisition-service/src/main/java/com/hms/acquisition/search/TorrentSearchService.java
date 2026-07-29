package com.hms.acquisition.search;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.hms.shared.json.SearchResponse;
import com.hms.shared.orchestrator.SseEmitterOrchestrator;
import com.hms.shared.util.Wrapper;

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

    public List<SearchResponse> searchTorrentsJson(SearchRequest request) throws Exception {
        Wrapper<Boolean> hasError = new Wrapper<>(false);
        List<SearchResponse> responses = Stream.<TorrentSearchHandler>of(
                new PirateBaySearchHandler(),
                // new LimeTorrentSearchHandler(),
                new BitSearchSearchHandler()
        )
        .parallel()
        .flatMap(handler -> {
            try {
                return handler.searchTorrentsJson(request).stream();
            } catch (Exception e) {
                e.printStackTrace();
                hasError.set(true);
                return Stream.empty();
            }
        })
        .sorted((s1, s2) -> Integer.valueOf(s2.seeders()).compareTo(Integer.valueOf(s1.seeders())))
        .toList();
        if (hasError.get()) {
            throw new Exception("One or more search handlers failed. Check logs for details.");
        }
        return responses;
    }
}
