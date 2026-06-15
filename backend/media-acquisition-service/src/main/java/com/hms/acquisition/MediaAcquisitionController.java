package com.hms.acquisition;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.acquisition.importmedia.ImportMediaRequest;
import com.hms.acquisition.search.SearchResponseList;
import com.hms.acquisition.search.TorrentSearchService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/acquisition")
public class MediaAcquisitionController {

    private final TorrentAcquisitionService torrentAcquisitionService;
    private final TorrentSearchService torrentSearchService;

    public MediaAcquisitionController(TorrentAcquisitionService torrentAcquisitionService, TorrentSearchService torrentSearchService) {
        this.torrentAcquisitionService = torrentAcquisitionService;
        this.torrentSearchService = torrentSearchService;
    }

    @PostMapping("/importRequest")
    public ResponseEntity<Boolean> importMediaRequest(@Valid @RequestBody ImportMediaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(torrentAcquisitionService.addImportRequest(request));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponseList> search(@RequestParam String query) {
        return ResponseEntity.ok(torrentSearchService.searchTorrents(query));
    }
    
}
