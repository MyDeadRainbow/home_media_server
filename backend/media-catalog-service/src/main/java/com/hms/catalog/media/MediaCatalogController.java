package com.hms.catalog.media;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.catalog.MediaItem;
import com.hms.shared.media.Episode;
import com.hms.shared.media.MediaCategory;
import com.hms.shared.media.MediaInfo;
import com.hms.shared.media.Movie;
import com.hms.shared.media.Season;
import com.hms.shared.media.Series;

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

    @GetMapping("/series")
    public List<Series> getSeries(
            @RequestParam(required = false) String query) {
        return mediaCatalogService.getSeries(query);
    }
    
    @GetMapping("/series/{id}")
    public ResponseEntity<Series> getSeriesById(@PathVariable String id) {
        Series series = mediaCatalogService.getSeriesById(id);
        if (series != null) {
            return ResponseEntity.ok(series);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/seasons")
    public List<Season> getSeason(@RequestParam String seriesId, @RequestParam(required = false) String query) {
        return mediaCatalogService.getSeason(seriesId, query);
    }
    
    @GetMapping("/episodes")
    public List<Episode> getEpisodes(@RequestParam String seriesId, @RequestParam String seasonId, @RequestParam(required = false) String query) {
        return mediaCatalogService.getEpisodes(seriesId, seasonId, query);
    }

    @GetMapping("/movies")
    public List<Movie> getMovies(@RequestParam(required = false) String query) {
        return mediaCatalogService.getMovies(query);
    }

    @GetMapping("/movies/{id}")
    public ResponseEntity<Movie> getMovieById(@PathVariable String id) {
        Movie movie = mediaCatalogService.getMovieById(id);
        if (movie != null) {
            return ResponseEntity.ok(movie);
        } else {
            return ResponseEntity.notFound().build();
        }
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
