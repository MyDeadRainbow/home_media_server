package com.hms.acquisition.datamine;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.hms.shared.messaging.datamining.DataMineRequest;
import com.hms.shared.messaging.datamining.DataMineRequest.Movie;
import com.hms.shared.messaging.metadata.MetaData;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

public class DatamineMovieHandler extends DatamineHandler<DataMineRequest.Movie> {


    @Override
    protected Movie entryHandler(Page page, Movie entry) throws Exception {
        page.waitForSelector("p[data-testid=plot] > span > span > span");
        ElementHandle plotElement = page.querySelector("p[data-testid=plot] > span > span > span");
        String plotSummary = plotElement.innerText();

        Float rating = null;
        ElementHandle ratingElement = page
                .querySelector("span[data-testid=hero-rating-bar__aggregate-rating__score] > span");
        if (ratingElement != null) {
            try {
                rating = Float.parseFloat(ratingElement.innerText());
            } catch (NumberFormatException e) {
                // Handle the case where the rating is not a valid float
                rating = null;
            }
        }

        LocalDate releaseDate = null;
        ElementHandle releaseDateElement = page.querySelector("li[data-testid=title-details-releasedate] > a");
        releaseDateElement.click();

        page.waitForSelector("div[data-testid=sub-section-releases] > ul > li");
        List<ElementHandle> releaseDateElements = page
                .querySelectorAll("div[data-testid=sub-section-releases] > ul > li");
        releaseDateElement = releaseDateElements.stream()
                .filter(element -> element.querySelector("a").innerText().contains("United States"))
                .map(e -> e.querySelector("div > ul > li > span"))
                .findFirst()
                .orElse(null);
        if (releaseDateElement != null) {
            releaseDate = LocalDate.parse(releaseDateElement.innerText(), DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        }

        MetaData.Movie movieMetadata = new MetaData.Movie(
                entry.movieId(),
                entry.movieTitle(),
                plotSummary,
                releaseDate,
                rating);

        MetaDataProducer.postMessage(movieMetadata);
        return entry;
    }

}
