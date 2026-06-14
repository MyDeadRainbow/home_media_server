package com.hms.acquisition;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.acquisition.importmedia.ImportMediaRequest;

@RestController
@RequestMapping("/api/acquisition")
public class MediaAcquisitionController {

    private final TorrentAcquisitionService torrentAcquisitionService;

    public MediaAcquisitionController(TorrentAcquisitionService torrentAcquisitionService) {
        this.torrentAcquisitionService = torrentAcquisitionService;
    }

    @PostMapping("/importRequest")
    public ResponseEntity<Boolean> importMediaRequest(@Valid @RequestBody ImportMediaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(torrentAcquisitionService.addImportRequest(request));
    }

}
