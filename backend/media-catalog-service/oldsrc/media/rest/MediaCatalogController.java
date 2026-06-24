package com.hms.catalog.media.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.catalog.MediaItem;
import com.hms.catalog.media.MediaInfo;
import com.hms.shared.media.MediaCategory;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/media")
public class MediaCatalogController {

    private final MediaCatalogService mediaCatalogService;

    public MediaCatalogController(MediaCatalogService mediaCatalogService) {
        this.mediaCatalogService = mediaCatalogService;
    }

    @GetMapping
    public List<MediaInfo> search(@RequestParam(required = false) String query,
            @RequestParam(required = false) String category) {
        return mediaCatalogService.search(query,
                category != null ? MediaCategory.valueOf(category.toUpperCase()) : null);
    }

    // @GetMapping("/{id}")
    // public ResponseEntity<MediaItem> byId(@PathVariable String id) {
    //     return mediaCatalogService.findById(id)
    //             .map(ResponseEntity::ok)
    //             .orElseGet(() -> ResponseEntity.notFound().build());
    // }

    @PostMapping
    public ResponseEntity<MediaItem> create(@Valid @RequestBody CreateMediaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaCatalogService.add(request));
    }
}
