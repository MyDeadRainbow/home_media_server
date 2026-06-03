package com.hms.catalog;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
// @CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/media")
public class MediaCatalogController {

    private final MediaCatalogService mediaCatalogService;

    public MediaCatalogController(MediaCatalogService mediaCatalogService) {
        this.mediaCatalogService = mediaCatalogService;
    }

    @GetMapping
    public List<MediaItem> search(@RequestParam(name = "query", required = false) String query) {
        return mediaCatalogService.search(query);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaItem> byId(@PathVariable String id) {
        return mediaCatalogService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MediaItem> create(@Valid @RequestBody CreateMediaRequest request) {
        
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaCatalogService.add(request));
    }
}
