package com.hms.acquisition.datamine;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hms.shared.messaging.datamining.DataMineRequest;
import com.hms.shared.messaging.datamining.DataMineRequest.Series;
import com.hms.shared.messaging.metadata.MetaData;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

import io.mikael.urlbuilder.UrlBuilder;

public class DatamineSeriesHandler extends DatamineHandler<DataMineRequest.Series> {

    @Override
    protected Series entryHandler(Page page, Series entry) throws Exception {

        String plotSummary = "";
        ElementHandle plotElement = page.querySelector("p[data-testid=plot] > span > span > span");
        if (plotElement != null) {
            plotSummary = plotElement.innerText();
        }

        LocalDate airDate = null;

        List<ElementHandle> seasonTabs = page
                .querySelectorAll("div.SeasonSelect_seasonSelect__hVgX2 > div > ul[role=tablist] > li");
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

        List<MetaData.Season> seasonsMetadata = new ArrayList<>();
        for (DataMineRequest.Season season : entry.seasons()) {

            List<MetaData.Episode> episodesMetadata = new ArrayList<>();
            for (DataMineRequest.Episode episode : season.episodes()) {

                if (season.seasonNumber() > seasonTabs.size()) {
                    throw new Exception("Season number " + season.seasonNumber() + " does not exist for series: "
                            + entry.seriesTitle());
                }

                ElementHandle seasonTab = seasonTabs.get(season.seasonNumber() - 1);
                seasonTab.click();

                page.waitForSelector("div.Carousel_scrollContainer__OHvrx > div.Carousel_item__d6t0m");
                List<ElementHandle> episodeElements = page
                        .querySelectorAll("div.Carousel_scrollContainer__OHvrx > div.Carousel_item__d6t0m");

                if (episode.episodeNumber() > episodeElements.size()) {
                    throw new Exception("Episode number " + episode.episodeNumber() + " does not exist for season "
                            + season.seasonNumber() + " of series: " + entry.seriesTitle());
                }

                ElementHandle episodeElement = episodeElements.get(episode.episodeNumber() - 1);

                ElementHandle episodeTitleElement = episodeElement
                        .querySelector("div.EpisodeRatingCard_title__7ltRw > div > a > h3.ipc-title__text");
                String episodeTitle = episodeTitleElement.innerText();

                ElementHandle episodePlotElement = episodeElement
                        .querySelector("div.EpisodeRatingCard_plot__tpuIw > p");
                String episodePlotSummary = episodePlotElement != null ? episodePlotElement.innerText() : "";

                ElementHandle airDateElement = episodeElement
                        .querySelector("ul.EpisodeRatingCard_episodeInfo__2QFJU > li:nth-child(2)");
                LocalDate episodeAirDate = null;
                if (airDateElement != null) {
                    episodeAirDate = LocalDate.parse(airDateElement.innerText(),
                            DateTimeFormatter.ofPattern("E, MMM d, yyyy", Locale.ENGLISH));
                }

                ElementHandle epsiodeRatingElement = episodeElement
                        .querySelector("div.EpisodeRatingCard_ratings___bPTN > span > span.ipc-rating-star--rating");
                Float episodeRating = null;
                if (epsiodeRatingElement != null) {
                    try {
                        rating = Float.parseFloat(epsiodeRatingElement.innerText());
                    } catch (NumberFormatException e) {
                        // Handle the case where the rating is not a valid float
                        rating = null;
                    }
                }

                MetaData.Episode episodeMetadata = new MetaData.Episode(episode.episodeId(), episodeTitle,
                        episodePlotSummary, episodeAirDate, episodeRating);
                episodesMetadata.add(episodeMetadata);
            }
            MetaData.Season seasonMetadata = new MetaData.Season(season.seriesId(),
                    season.seasonNumber(), episodesMetadata);
            seasonsMetadata.add(seasonMetadata);
        }

        MetaData.Series seriesMetadata = new MetaData.Series(
                entry.seriesId(),
                entry.seriesTitle(),
                plotSummary,
                airDate,
                rating,
                seasonsMetadata);
        MetaDataProducer.postMessage(seriesMetadata);
        return entry;
    }

    @Override
    protected String searchUrl(Series entry) {
        UrlBuilder urlBuilder = UrlBuilder.fromString(IMDB_BASE_URL).withPath(SEARCH_PATH)
                .addParameter(TITLE_PARAM, entry.imdbSearchTitle())
                .addParameter(TITLE_TYPE_PARAM, String.join(",", TITLE_TYPE_SERIES, TITLE_TYPE_MINI_SERIES));
        return urlBuilder.toString();
    }

}
