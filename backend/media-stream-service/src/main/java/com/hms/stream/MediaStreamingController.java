package com.hms.stream;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/stream")
public class MediaStreamingController {

    private final MediaStreamingService mediaStreamingService;

    public MediaStreamingController(MediaStreamingService mediaStreamingService) {
        this.mediaStreamingService = mediaStreamingService;
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

    @GetMapping("/files/{storageId}")
    public ResponseEntity<Resource> file(@PathVariable String storageId) {
        return mediaStreamingService.file(storageId);
    }
}
