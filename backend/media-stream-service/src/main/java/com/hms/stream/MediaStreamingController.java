package com.hms.stream;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hms.shared.json.ImportMediaRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/stream")
public class MediaStreamingController {

    private final MediaStreamingService mediaStreamingService;
    private final TorrentDownloadService torrentAcquisitionService;

    public MediaStreamingController(MediaStreamingService mediaStreamingService,
            TorrentDownloadService torrentAcquisitionService) {
        this.mediaStreamingService = mediaStreamingService;
        this.torrentAcquisitionService = torrentAcquisitionService;
    }

    @GetMapping("/{mediaId}/manifest")
    public StreamManifestResponse manifest(
            @PathVariable String mediaId,
            @RequestParam(name = "playbackUrl", required = false) String playbackUrl) {
        return mediaStreamingService.manifest(mediaId, playbackUrl);
    }

    @GetMapping(value = "/{mediaId}/captions", produces = "text/vtt")
    public ResponseEntity<String> captions(@PathVariable String mediaId,
            @RequestParam(defaultValue = "en") String lang) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/vtt"))
                .body(mediaStreamingService.vtt(mediaId, lang));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadMediaResponse> upload(@RequestParam("file") MultipartFile file,
            @RequestParam String title, @RequestParam String type, @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String description) {
        return ResponseEntity
                .ok(mediaStreamingService.upload(file, new UploadMediaRequest(file, title, type, year, description)));
    }

    @PostMapping("/importRequest")
    public ResponseEntity<Boolean> importMediaRequest(@Valid @RequestBody ImportMediaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(torrentAcquisitionService.addImportRequest(request));
    }

    @GetMapping("/files/{storageId}")
    public ResponseEntity<Resource> file(@PathVariable String storageId) {
        return mediaStreamingService.file(storageId);
    }

}
