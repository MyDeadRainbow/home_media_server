package com.hms.acquisition.datamine;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.hms.shared.messaging.datamining.DataMineRequest;
import com.hms.shared.messaging.datamining.DataMineRequest.Series;
import com.hms.shared.messaging.metadata.MetaData;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

public class DatamineSeriesHandler extends DatamineHandler<DataMineRequest.Series> {

    @Override
    protected Series entryHandler(Page page, Series entry) throws Exception {
        page.waitForSelector("p[data-testid=plot] > span > span > span");
        String plotSummary = "";
        ElementHandle plotElement = page.querySelector("p[data-testid=plot] > span > span > span");
        if (plotElement != null) {
            plotSummary = plotElement.innerText();
        }

        LocalDate airDate = null;

        List<ElementHandle> seasonTabs = page
                .querySelectorAll("div.SeasonSelect_seasonSelect__hVgX2 > ul[role=tablist] > li");
        if (!seasonTabs.isEmpty()) {

            ElementHandle seasonTab = seasonTabs.get(0);
            seasonTab.click();

            page.waitForSelector("div.Carousel_scrollContainer__OHvrx > div.Carousel_item__d6t0m");
            List<ElementHandle> episodeElements = page
                    .querySelectorAll("div.Carousel_scrollContainer__OHvrx > div.Carousel_item__d6t0m");
            if (!episodeElements.isEmpty()) {

                ElementHandle episodeElement = episodeElements.get(0);
                ElementHandle airDateElement = episodeElement
                        .querySelector("ul.EpisodeRatingCard_episodeInfo__2QFJU > li:nth-child(2)");
                if (airDateElement != null) {
                    airDate = LocalDate.parse(airDateElement.innerText(),
                            DateTimeFormatter.ofPattern("E, MMM d, yyyy"));
                }
            }
        }

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

        MetaData.Series seriesMetadata = new MetaData.Series(
                entry.seriesId(),
                entry.seriesTitle(),
                plotSummary,
                airDate,
                rating);
        MetaDataProducer.postMessage(seriesMetadata);
        return entry;
    }

}
