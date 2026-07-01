package com.hms.acquisition.datamine;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.hms.shared.messaging.datamining.DataMineRequest;
import com.hms.shared.messaging.datamining.DataMineRequest.Episode;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

import io.mikael.urlbuilder.UrlBuilder;

// public class DatamineEpisodeHandler extends DatamineHandler<DataMineRequest.Episode> {

//     @Override
//     protected Episode entryHandler(Page page, Episode entry) throws Exception {
        
//         List<ElementHandle> seasonTabs = page
//                 .querySelectorAll("div.SeasonSelect_seasonSelect__hVgX2 > div > ul[role=tablist] > li");

//         if (entry.seasonNumber() > seasonTabs.size()) {
//             throw new Exception("Season number " + entry.seasonNumber() + " does not exist for series: "
//                     + entry.seriesTitle());
//         }

//         ElementHandle seasonTab = seasonTabs.get(entry.seasonNumber() - 1);
//         seasonTab.click();

//         page.waitForSelector("div.Carousel_scrollContainer__OHvrx > div.Carousel_item__d6t0m");
//         List<ElementHandle> episodeElements = page
//                 .querySelectorAll("div.Carousel_scrollContainer__OHvrx > div.Carousel_item__d6t0m");

//         if (entry.episodeNumber() > episodeElements.size()) {
//             throw new Exception("Episode number " + entry.episodeNumber() + " does not exist for season "
//                     + entry.seasonNumber() + " of series: " + entry.seriesTitle());
//         }

//         ElementHandle episodeElement = episodeElements.get(entry.episodeNumber() - 1);

//         ElementHandle episodeTitleElement = episodeElement
//                 .querySelector("div.EpisodeRatingCard_title__7ltRw > div > a > h3.ipc-title__text");
//         String episodeTitle = episodeTitleElement.innerText();

//         ElementHandle plotElement = episodeElement.querySelector("div.EpisodeRatingCard_plot__tpuIw > p");
//         String plotSummary = plotElement != null ? plotElement.innerText() : "";

//         ElementHandle airDateElement = episodeElement
//                 .querySelector("ul.EpisodeRatingCard_episodeInfo__2QFJU > li:nth-child(2)");
//         LocalDate airDate = null;
//         if (airDateElement != null) {
//             airDate = LocalDate.parse(airDateElement.innerText(), DateTimeFormatter.ofPattern("E, MMM d, yyyy", Locale.ENGLISH));
//         }

//         ElementHandle ratingElement = episodeElement
//                 .querySelector("div.EpisodeRatingCard_ratings___bPTN > span > span.ipc-rating-star--rating");
//         Float rating = null;
//         if (ratingElement != null) {
//             try {
//                 rating = Float.parseFloat(ratingElement.innerText());
//             } catch (NumberFormatException e) {
//                 // Handle the case where the rating is not a valid float
//                 rating = null;
//             }
//         }

//         MetaData.Episode metaData = new MetaData.Episode(entry.episodeId(), episodeTitle, plotSummary, airDate, rating);
//         MetaDataProducer.postMessage(metaData);

//         // this is series information
//         // page.waitForSelector("p[data-testid=plot] > span > span > span");
//         // ElementHandle plotElement = page.querySelector("p[data-testid=plot] > span >
//         // span > span");
//         // String plotSummary = plotElement.innerText();
//         return entry; // Placeholder return statement
//     }

//     @Override
//     protected String searchUrl(Episode entry) {
//         UrlBuilder urlBuilder = UrlBuilder.fromString(IMDB_BASE_URL).withPath(SEARCH_PATH)
//                 .addParameter(TITLE_PARAM, entry.seriesTitle())
//                 .addParameter(TITLE_TYPE_PARAM, String.join(",", TITLE_TYPE_SERIES, TITLE_TYPE_MINI_SERIES));
//         return urlBuilder.toString();
//     }
// }
