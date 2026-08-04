package com.hms.library;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.HtmlRestController;
import com.hms.html.DocumentBuilderFactory;
import com.hms.shared.media.Movie;
import com.hms.shared.media.Series;

@RestController
public class LibraryController extends HtmlRestController {

    @Value("${API_GATEWAY_URL:http://localhost:8080}")
    private String apiGatewayUrl;

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
            // Document doc = buildDocument("templates/library/index.html");

            // doc.selectFirst("[rid=nav-links]").children().forEach((li) -> {
            //     Element a = li.selectFirst("a");
            //     if (a != null) {
            //         String rid = a.attr("rid");
            //         if ("library".equals(rid)) {
            //             a.addClass("active");
            //         } else {
            //             a.removeClass("active");
            //         }
            //     }
            // });

            Document doc = DocumentBuilderFactory.getDocumentBuilder("library").buildDocument();

            Element seriesSection = doc.selectFirst("[rid=series-list]");

            Element mediaCardTemplate = buildComponent("components/media_card.html");
            Element mediaCardDetailTemplate = buildComponent("components/card_detail.html");

            List<Series> seriesList = seriesService.getSeries(query);
            if (seriesList.isEmpty()) {
                Element noResultsMessage = seriesSection.selectFirst("[rid=no-result-message]");
                if (noResultsMessage == null) {
                    noResultsMessage = doc.createElement("p").attr("rid", "no-result-message").addClass("muted");
                    noResultsMessage.text("No series found.");
                    seriesSection.appendChild(noResultsMessage);
                }
            } else {
                Element noResultsMessage = seriesSection.selectFirst("[rid=no-result-message]");
                if (noResultsMessage != null) {
                    noResultsMessage.remove();
                }
            }
            Element seriesCardsContainer = seriesSection.selectFirst("[rid=cards]");
            for (Series series : seriesList) {
                Element mediaCard = mediaCardTemplate.clone();
                mediaCard.selectFirst("[rid=poster]").attr("src", series.poster().url());
                mediaCard.selectFirst("[rid=displayTitle]").text(series.title());
                mediaCard.selectFirst("[rid=displaySummary]").text(series.metaData().plotSummary());
                Element cardDetails = mediaCard.selectFirst("[rid=card-details]");

                Element mediaCardDetail = mediaCardDetailTemplate.clone();
                mediaCardDetail.selectFirst("[rid=displayTitle]").text("Air Date");
                mediaCardDetail.selectFirst("[rid=displayValue]")
                        .text(series.metaData().airDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                cardDetails.appendChild(mediaCardDetail);

                mediaCardDetail = mediaCardDetailTemplate.clone();
                mediaCardDetail.selectFirst("[rid=displayTitle]").text("Rating");
                mediaCardDetail.selectFirst("[rid=displayValue]").text(series.metaData().rating().toString());
                cardDetails.appendChild(mediaCardDetail);

                mediaCardDetail = mediaCardDetailTemplate.clone();
                mediaCardDetail.selectFirst("[rid=displayTitle]").text("Status");
                mediaCardDetail.selectFirst("[rid=displayValue]").text(series.metaData().status().toString());
                cardDetails.appendChild(mediaCardDetail);

                seriesCardsContainer.appendChild(mediaCard);
            }

            Element moviesSection = doc.selectFirst("[rid=movies-list]");

            List<Movie> movieList = movieService.getMovies(query);
            if (movieList.isEmpty()) {
                Element noResultsMessage = moviesSection.selectFirst("[rid=no-result-message]");
                if (noResultsMessage == null) {
                    noResultsMessage = doc.createElement("p").attr("rid", "no-result-message").addClass("muted");
                    noResultsMessage.text("No movies found.");
                    moviesSection.appendChild(noResultsMessage);
                }
            } else {
                Element noResultsMessage = moviesSection.selectFirst("[rid=no-result-message]");
                if (noResultsMessage != null) {
                    noResultsMessage.remove();
                }
            }

            Element moviesCardsContainer = moviesSection.selectFirst("[rid=cards]");
            for (Movie movie : movieList) {
                Element mediaCard = mediaCardTemplate.clone();
                mediaCard.selectFirst("[rid=poster]").attr("src", movie.poster().url());
                mediaCard.selectFirst("[rid=displayTitle]").text(movie.title());
                mediaCard.selectFirst("[rid=displaySummary]").text(movie.metaData().plotSummary());
                Element cardDetails = mediaCard.selectFirst("[rid=card-details]");

                Element mediaCardDetail = mediaCardDetailTemplate.clone();
                mediaCardDetail.selectFirst("[rid=displayTitle]").text("Air Date");
                mediaCardDetail.selectFirst("[rid=displayValue]")
                        .text(movie.metaData().airDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                cardDetails.appendChild(mediaCardDetail);

                mediaCardDetail = mediaCardDetailTemplate.clone();
                mediaCardDetail.selectFirst("[rid=displayTitle]").text("Rating");
                mediaCardDetail.selectFirst("[rid=displayValue]").text(movie.metaData().rating().toString());
                cardDetails.appendChild(mediaCardDetail);

                mediaCardDetail = mediaCardDetailTemplate.clone();
                mediaCardDetail.selectFirst("[rid=displayTitle]").text("Status");
                mediaCardDetail.selectFirst("[rid=displayValue]").text(movie.metaData().status().toString());
                cardDetails.appendChild(mediaCardDetail);

                Element mediaCardLink = mediaCard.selectFirst("[rid=watch-link]");
                mediaCardLink.attr("href", "/movie/" + movie.movieId());

                moviesCardsContainer.appendChild(mediaCard);
            }

            return ResponseEntity.ok(doc.html());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error reading library/index.html");
        }
    }

    @GetMapping(path = "/movie/{id}", produces = "text/html")
    public ResponseEntity<String> getMovieDetail(@PathVariable String id) {
        try {
            Document doc = buildDocument("templates/movie/index.html");
            Movie movie = movieService.getMovieById(id);
            if (movie == null) {
                return ResponseEntity.notFound().build();
            }
            doc.selectFirst("[rid=title]").text(movie.title());
            doc.selectFirst("[rid=plot-summary]").text(movie.metaData().plotSummary());

            Element detailGrid = doc.selectFirst("[rid=details]");
            Element detailRow = buildComponent("components/detail_row.html");
            detailRow.selectFirst("[rid=displayTitle]").text("Air Date");
            detailRow.selectFirst("[rid=displayValue]")
                    .text(movie.metaData().airDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            detailGrid.appendChild(detailRow);

            detailRow = buildComponent("components/detail_row.html");
            detailRow.selectFirst("[rid=displayTitle]").text("Rating");
            detailRow.selectFirst("[rid=displayValue]").text(movie.metaData().rating().toString());
            detailGrid.appendChild(detailRow);

            detailRow = buildComponent("components/detail_row.html");
            detailRow.selectFirst("[rid=displayTitle]").text("Status");
            detailRow.selectFirst("[rid=displayValue]").text(movie.metaData().status().toString());
            detailGrid.appendChild(detailRow);

            Element videoComponent = doc.selectFirst("[rid=media-player]").parent();
            videoComponent.selectFirst("[rid=media-player]").attr("src",
                    apiGatewayUrl + "/api/stream/files/" + movie.mediaItem().mediaId());

            return ResponseEntity.ok(doc.html());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error reading movie/index.html");
        }
    }

    @GetMapping(path = "/series/{id}", produces = "text/html")
    public ResponseEntity<String> getSeriesDetail(@PathVariable String id) {
        try {
            Document doc = buildDocument("templates/series/index.html");
            Series series = seriesService.getSeriesById(id);
            if (series == null) {
                return ResponseEntity.notFound().build();
            }
            doc.selectFirst("[rid=title]").text(series.title());
            doc.selectFirst("[rid=plot-summary]").text(series.metaData().plotSummary());

            Element detailGrid = doc.selectFirst("[rid=details]");
            Element detailRow = buildComponent("components/detail_row.html");
            detailRow.selectFirst("[rid=displayTitle]").text("Air Date");
            detailRow.selectFirst("[rid=displayValue]")
                    .text(series.metaData().airDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            detailGrid.appendChild(detailRow);

            detailRow = buildComponent("components/detail_row.html");
            detailRow.selectFirst("[rid=displayTitle]").text("Rating");
            detailRow.selectFirst("[rid=displayValue]").text(series.metaData().rating().toString());
            detailGrid.appendChild(detailRow);

            detailRow = buildComponent("components/detail_row.html");
            detailRow.selectFirst("[rid=displayTitle]").text("Status");
            detailRow.selectFirst("[rid=displayValue]").text(series.metaData().status().toString());
            detailGrid.appendChild(detailRow);

            return ResponseEntity.ok(doc.html());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error reading series/index.html");
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
