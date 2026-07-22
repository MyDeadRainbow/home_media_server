package com.hms.library;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.HtmlRestController;
import com.hms.shared.media.Movie;
import com.hms.shared.media.Series;

@RestController
public class LibraryController extends HtmlRestController {

    public final SeriesService seriesService;
    public final MovieService movieService;

    public LibraryController(SeriesService seriesService, MovieService movieService) {
        this.seriesService = seriesService;
        this.movieService = movieService;
    }

    @GetMapping(path = "/library", produces = "text/html")
    public ResponseEntity<String> getLibrary(@RequestParam(required = false) String query) {
        query = (query == null) ? "" : query.trim();
        try {
            Document doc = buildDocument("templates/library/index.html");

            Element seriesSection = doc.selectFirst("#series-list");

            Element mediaCardTemplate = buildComponent("components/media_card.html");
            Element mediaCardDetailTemplate = buildComponent("components/card_detail.html");
            
            List<Series> seriesList = seriesService.getSeries(query);
            if (seriesList.isEmpty()) {
                Element noResultsMessage = seriesSection.selectFirst(".muted");
                if (noResultsMessage == null) {
                    noResultsMessage = doc.createElement("p").addClass("muted");
                    noResultsMessage.text("No series found.");
                    seriesSection.appendChild(noResultsMessage);
                }
            } else {
                Element noResultsMessage = seriesSection.selectFirst(".muted");
                if (noResultsMessage != null) {
                    noResultsMessage.remove();
                }
            }
            Element seriesCardsContainer = seriesSection.selectFirst(".cards");
            for (Series series : seriesList) {
                Element mediaCard = mediaCardTemplate.clone();
                mediaCard.selectFirst("#poster").attr("src", series.poster().url());
                mediaCard.selectFirst("#displayTitle").text(series.title());
                mediaCard.selectFirst("#displaySummary").text(series.metaData().plotSummary());
                Element cardDetails = mediaCard.selectFirst("#card-details");

                Element mediaCardDetail = mediaCardDetailTemplate.clone();
                mediaCardDetail.selectFirst("#displayTitle").text("Air Date");
                mediaCardDetail.selectFirst("#displayValue")
                        .text(series.metaData().airDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                cardDetails.appendChild(mediaCardDetail);

                mediaCardDetail = mediaCardDetailTemplate.clone();
                mediaCardDetail.selectFirst("#displayTitle").text("Rating");
                mediaCardDetail.selectFirst("#displayValue").text(series.metaData().rating().toString());
                cardDetails.appendChild(mediaCardDetail);

                mediaCardDetail = mediaCardDetailTemplate.clone();
                mediaCardDetail.selectFirst("#displayTitle").text("Status");
                mediaCardDetail.selectFirst("#displayValue").text(series.metaData().status().toString());
                cardDetails.appendChild(mediaCardDetail);

                seriesCardsContainer.appendChild(mediaCard);
            }

            Element moviesSection = doc.selectFirst("#movies-list");

            List<Movie> movieList = movieService.getMovies(query);
            if (movieList.isEmpty()) {
                Element noResultsMessage = moviesSection.selectFirst(".muted");
                if (noResultsMessage == null) {
                    noResultsMessage = doc.createElement("p").addClass("muted");
                    noResultsMessage.text("No movies found.");
                    moviesSection.appendChild(noResultsMessage);
                }
            } else {
                Element noResultsMessage = moviesSection.selectFirst(".muted");
                if (noResultsMessage != null) {
                    noResultsMessage.remove();
                }
            }

            Element moviesCardsContainer = moviesSection.selectFirst(".cards");
            for (Movie movie : movieList) {
                Element mediaCard = mediaCardTemplate.clone();
                mediaCard.selectFirst("#poster").attr("src", movie.poster().url());
                mediaCard.selectFirst("#displayTitle").text(movie.title());
                mediaCard.selectFirst("#displaySummary").text(movie.metaData().plotSummary());
                Element cardDetails = mediaCard.selectFirst("#card-details");

                Element mediaCardDetail = mediaCardDetailTemplate.clone();
                mediaCardDetail.selectFirst("#displayTitle").text("Air Date");
                mediaCardDetail.selectFirst("#displayValue")
                        .text(movie.metaData().airDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                cardDetails.appendChild(mediaCardDetail);

                mediaCardDetail = mediaCardDetailTemplate.clone();
                mediaCardDetail.selectFirst("#displayTitle").text("Rating");
                mediaCardDetail.selectFirst("#displayValue").text(movie.metaData().rating().toString());
                cardDetails.appendChild(mediaCardDetail);

                mediaCardDetail = mediaCardDetailTemplate.clone();
                mediaCardDetail.selectFirst("#displayTitle").text("Status");
                mediaCardDetail.selectFirst("#displayValue").text(movie.metaData().status().toString());
                cardDetails.appendChild(mediaCardDetail);

                moviesCardsContainer.appendChild(mediaCard);
            }

            return ResponseEntity.ok(doc.html());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error reading library/index.html");
        }
    }

    // @GetMapping(path = "/library/search", produces = "text/html")
    // public ResponseEntity<String> searchLibrary(SearchFormData formData) {
    // try {
    // Document doc = buildDocument("templates/library/index.html");

    // Element seriesSection = doc.selectFirst(".library-subsection #series-list");

    // Element mediaCardTemplate = buildComponent("components/media_card.html");

    // for (int i = 0; i < 10; i++) {
    // Element listItem = doc.createElement("li");
    // Element mediaCard = mediaCardTemplate.clone();
    // mediaCard.selectFirst("#displayTitle").text("Sample Title " + (i + 1));
    // mediaCard.selectFirst("#displaySummary").text("This is a sample summary for
    // media item " + (i + 1) + ".");
    // listItem.appendChild(mediaCard);
    // seriesSection.appendChild(listItem);
    // }

    // Element moviesSection = doc.selectFirst(".library-subsection #movies-list");

    // for (int i = 0; i < 10; i++) {
    // Element listItem = doc.createElement("li");
    // Element mediaCard = mediaCardTemplate.clone();
    // mediaCard.selectFirst("#displayTitle").text("Sample Movie Title " + (i + 1));
    // mediaCard.selectFirst("#displaySummary").text("This is a sample summary for
    // movie item " + (i + 1) + ".");
    // listItem.appendChild(mediaCard);
    // moviesSection.appendChild(listItem);
    // }

    // return ResponseEntity.ok(doc.html());
    // } catch (Exception e) {
    // e.printStackTrace();
    // return ResponseEntity.status(500).body("Error reading library/index.html");
    // }
    // }
}
