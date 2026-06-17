package com.hms.acquisition;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.hms.acquisition.search.SearchRequest;
import com.hms.acquisition.search.SearchResponseList;
import com.hms.acquisition.search.TorrentSearchService;
import com.hms.shared.media.MediaCategory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/acquisition")
public class MediaAcquisitionController {

    private final TorrentSearchService torrentSearchService;

    public MediaAcquisitionController(TorrentSearchService torrentSearchService) {
        this.torrentSearchService = torrentSearchService;
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
