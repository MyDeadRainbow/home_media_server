package com.hms.catalog.datamine;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.hms.catalog.datamine.exception.DatamineException;
import com.hms.shared.media.Movie;
import com.hms.shared.media.metadata.MetaData;
import com.hms.shared.media.metadata.MetaDataStatus;
// import com.hms.shared.messaging.datamining.DataMineRequest;
// import com.hms.shared.messaging.datamining.DataMineRequest.Movie;
// import com.microsoft.playwright.ElementHandle;
// import com.microsoft.playwright.Page;

// import io.mikael.urlbuilder.UrlBuilder;

// public class DatamineMovieHandler extends DatamineHandler<Movie, Movie> {

//     @Override
//     protected Movie entryHandler(Page page, Movie entry) throws DatamineException {

//         // MetaData.Movie movieMetadata = new MetaData.Movie().withMovieId(entry.movieId());
//         MetaData movieMetadata = entry.metaData();

//         ElementHandle plotElement = page.querySelector("p[data-testid=plot] > span > span > span");
//         String plotSummary = plotElement.innerText();
//         movieMetadata = movieMetadata.withPlotSummary(plotSummary);

//         Float rating = null;
//         ElementHandle ratingElement = page
//                 .querySelector("span[data-testid=hero-rating-bar__aggregate-rating__score] > span");
//         if (ratingElement != null) {
//             try {
//                 rating = Float.parseFloat(ratingElement.innerText());
//             } catch (NumberFormatException e) {
//                 // Handle the case where the rating is not a valid float
//                 rating = null;
//             }
//         }
//         movieMetadata = movieMetadata.withRating(rating);

//         LocalDate releaseDate = null;
//         ElementHandle releaseDateElement = page.querySelector("li[data-testid=title-details-releasedate] > a");
//         releaseDateElement.click();
//         page.waitForLoadState();

//         page.waitForSelector("div[data-testid=sub-section-releases] > ul > li");
//         List<ElementHandle> releaseDateElements = page
//                 .querySelectorAll("div[data-testid=sub-section-releases] > ul > li");
//         releaseDateElement = releaseDateElements.stream()
//                 .filter(element -> element.querySelector("a").innerText().contains("United States"))
//                 .map(e -> e.querySelector("div > ul > li > span"))
//                 .findFirst()
//                 .orElse(null);
//         if (releaseDateElement != null) {
//             releaseDate = LocalDate.parse(releaseDateElement.innerText(), DateTimeFormatter.ofPattern("MMMM d, yyyy"));
//         }

//         movieMetadata = movieMetadata.withAirDate(releaseDate);

//         movieMetadata = movieMetadata.withStatus(MetaDataStatus.COMPLETE).withMessage("Complete");

//         // MetaDataProducer.postMessage(movieMetadata);
//         return entry.withMetaData(movieMetadata);
//     }

//     @Override
//     protected String searchUrl(Movie entry) {
//         UrlBuilder urlBuilder = UrlBuilder.fromString(IMDB_BASE_URL).withPath(SEARCH_PATH)
//                 .addParameter(TITLE_PARAM, entry.title())
//                 .addParameter(TITLE_TYPE_PARAM, String.join(",", TITLE_TYPE_MOVIE, TITLE_TYPE_TV_MOVIE));
//         return urlBuilder.toString();
//     }

// }
