package com.hms.acquisition.datamine;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.hms.acquisition.datamine.exception.DatamineException;
import com.hms.acquisition.datamine.exception.EpisodeNotFoundException;
import com.hms.acquisition.datamine.exception.SeasonNotFoundException;
import com.hms.shared.media.Episode;
import com.hms.shared.media.Season;
import com.hms.shared.media.Series;
import com.hms.shared.media.metadata.MetaData;
import com.hms.shared.media.metadata.MetaDataStatus;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

import io.mikael.urlbuilder.UrlBuilder;

public class DatamineSeriesHandler extends DatamineHandler<Series, Series> {

    @Override
    protected Series entryHandler(Page page, Series entry) throws DatamineException {
        Series series = entry;
        MetaData seriesMetadata = entry.metaData();

        String plotSummary = "";
        ElementHandle plotElement = page.querySelector("p[data-testid=plot] > span > span > span");
        if (plotElement != null) {
            plotSummary = plotElement.innerText();
        }

        seriesMetadata = seriesMetadata.withPlotSummary(plotSummary);

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

        seriesMetadata = seriesMetadata.withAirDate(airDate);

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

        seriesMetadata = seriesMetadata.withRating(rating);

        for (Season season : entry.seasons()) {
            if (season.seasonNumber() > seasonTabs.size()) {
                throw new SeasonNotFoundException(
                        "Season number " + season.seasonNumber() + " does not exist for series: "
                                + entry.title());
            }

            for (Episode episode : season.episodes()) {

                MetaData episodeMetadata = episode.metaData();

                ElementHandle seasonTab = seasonTabs.get(season.seasonNumber() - 1);
                seasonTab.click();

                page.waitForSelector("div.Carousel_scrollContainer__OHvrx > div.Carousel_item__d6t0m");
                List<ElementHandle> episodeElements = page
                        .querySelectorAll("div.Carousel_scrollContainer__OHvrx > div.Carousel_item__d6t0m");

                if (episode.episodeNumber() > episodeElements.size()) {
                    throw new EpisodeNotFoundException(
                            "Episode number " + episode.episodeNumber() + " does not exist for season "
                                    + season.seasonNumber() + " of series: " + entry.title());
                }

                ElementHandle episodeElement = episodeElements.get(episode.episodeNumber() - 1);

                ElementHandle episodeTitleElement = episodeElement
                        .querySelector("div.EpisodeRatingCard_title__7ltRw > div > a > h3.ipc-title__text");
                String episodeTitle = episodeTitleElement.innerText();
                episodeMetadata = episodeMetadata.withTitle(episodeTitle);

                ElementHandle episodePlotElement = episodeElement
                        .querySelector("div.EpisodeRatingCard_plot__tpuIw > p");
                String episodePlotSummary = episodePlotElement != null ? episodePlotElement.innerText() : "";
                episodeMetadata = episodeMetadata.withPlotSummary(episodePlotSummary);

                ElementHandle airDateElement = episodeElement
                        .querySelector("ul.EpisodeRatingCard_episodeInfo__2QFJU > li:nth-child(2)");
                LocalDate episodeAirDate = null;
                if (airDateElement != null) {
                    episodeAirDate = LocalDate.parse(airDateElement.innerText(),
                            DateTimeFormatter.ofPattern("E, MMM d, yyyy", Locale.ENGLISH));
                }
                episodeMetadata = episodeMetadata.withAirDate(episodeAirDate);

                ElementHandle epsiodeRatingElement = episodeElement
                        .querySelector("div.EpisodeRatingCard_ratings___bPTN > span > span.ipc-rating-star--rating");
                Float episodeRating = null;
                if (epsiodeRatingElement != null) {
                    try {
                        episodeRating = Float.parseFloat(epsiodeRatingElement.innerText());
                    } catch (NumberFormatException e) {
                        // Handle the case where the rating is not a valid float
                        episodeRating = null;
                    }
                }
                episodeMetadata = episodeMetadata.withRating(episodeRating);

                episode = episode.withMetaData(episodeMetadata);
                season = season.replaceEpisode(episode);
                // season
                // seasonMetadata = seasonMetadata.addEpisode(episodeMetadata);
            }
            series = series.replaceSeason(season);
            // seriesMetadata = seriesMetadata.addSeason(seasonMetadata);
        }

        seriesMetadata = seriesMetadata.withStatus(MetaDataStatus.COMPLETE).withMessage("Complete");
        series = series.withMetaData(seriesMetadata);
        // MetaDataProducer.postMessage(seriesMetadata);
        return series;
    }

    @Override
    protected String searchUrl(Series entry) {
        UrlBuilder urlBuilder = UrlBuilder.fromString(IMDB_BASE_URL).withPath(SEARCH_PATH)
                .addParameter(TITLE_PARAM, entry.title())
                .addParameter(TITLE_TYPE_PARAM, String.join(",", TITLE_TYPE_SERIES, TITLE_TYPE_MINI_SERIES));
        return urlBuilder.toString();
    }

}
