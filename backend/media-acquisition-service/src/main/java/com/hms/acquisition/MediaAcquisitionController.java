package com.hms.acquisition;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/acquisition")
public class MediaAcquisitionController {

    private final TorrentAcquisitionService torrentAcquisitionService;

    public MediaAcquisitionController(TorrentAcquisitionService torrentAcquisitionService) {
        this.torrentAcquisitionService = torrentAcquisitionService;
    }

    @PostMapping("/import")
    public ResponseEntity<ImportMediaResponse> importMedia(@Valid @RequestBody ImportMediaRequest request) {
        return ResponseEntity.ok(torrentAcquisitionService.importMedia(request));
    }
}
