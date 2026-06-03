package com.hms.stream;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
// @CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/stream")
public class MediaStreamingController {

    private final MediaStreamingService mediaStreamingService;

    public MediaStreamingController(MediaStreamingService mediaStreamingService) {
        this.mediaStreamingService = mediaStreamingService;
    }

    @GetMapping("/{mediaId}/manifest")
    public StreamManifestResponse manifest(@PathVariable String mediaId) {
        return mediaStreamingService.manifest(mediaId);
    }

    @GetMapping(value = "/{mediaId}/captions", produces = "text/vtt")
    public ResponseEntity<String> captions(@PathVariable String mediaId, @RequestParam(defaultValue = "en") String lang) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/vtt"))
                .body(mediaStreamingService.vtt(mediaId, lang));
    }
}
