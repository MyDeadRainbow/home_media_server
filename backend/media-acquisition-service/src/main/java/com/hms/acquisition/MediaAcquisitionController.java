package com.hms.acquisition;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.hms.acquisition.search.SearchRequest;
import com.hms.acquisition.search.TorrentSearchService;
import com.hms.shared.media.MediaCategory;


@RestController
@RequestMapping("/api/acquisition")
public class MediaAcquisitionController {

    private final TorrentSearchService torrentSearchService;
    // private final DatamineGeminiAiHandler datamineGeminiAiHandler;

    public MediaAcquisitionController(TorrentSearchService torrentSearchService) {
        this.torrentSearchService = torrentSearchService;
        // this.datamineGeminiAiHandler = datamineGeminiAiHandler;
        // String output = datamineGeminiAiHandler.prompt("Provide me with the complete series information and every seasons and every episodes information for the tv series: The Office. Do this for every season in the series. Provide the information in a json object with the following format: {\"series\": \"seriesName\", \"synopsis\": \"officialSeriesSynopsis\", \"rating\": seriesImdbRatingFloatValue, \"seasons\": [ { \"number\": integerValue, \"firstAirDate\": \"firstEpisodeAirDate\", \"lastAirDate\": \"lastEpisodeAirDate\", \"episodes\": [ { \"episode\": \"episodeName\", \"number\": integerValue, \"synopsis\": \"officialEpisodeSynopsis\", \"rating\": episodeImdbRatingFloatValue, \"airDate\": \"episodeAirDate\", \"runtime\": runtimeInSeconds } ] } ] }");
        // System.out.println(output);
    }

    // @PostMapping("/importRequest")
    // public ResponseEntity<Boolean> importMediaRequest(@Valid @RequestBody ImportMediaRequest request) {
    //     return ResponseEntity.status(HttpStatus.CREATED).body(torrentAcquisitionService.addImportRequest(request));
    // }

    @GetMapping("/search")
    public SseEmitter search(@RequestParam String query, @RequestParam MediaCategory category) {
        SseEmitter emitter = new SseEmitter(1000*60*2L); // 2 minutes timeout
        torrentSearchService.searchTorrents(new SearchRequest(query, category), emitter);        
        return emitter;
    }
    
}
