package com.hms.shared.messaging.metadata;

import java.time.LocalDate;
import java.util.List;

import com.hms.shared.messaging.JsonSerializable;

// public interface MetaData extends JsonSerializable<MetaData> {
//     public static final String TOPIC = "media-metadata";

//     public static record Base(Status status, String message) implements MetaData {
//         public MetaData.Series asSeries() {
//             return new MetaData.Series(null, null, null, null, null, null, this);
//         }

//         public MetaData.Movie asMovie() {
//             return new MetaData.Movie(null, null, null, null, null, this);
//         }
//     }

//     /**
//      * (String episodeId, String title, String plotSummary, LocalDate airDate, Float
//      * rating)
//      */
//     public static record Episode(String episodeId, String title, String plotSummary, LocalDate airDate, Float rating,
//             Base base)
//             implements MetaData {

//         public Episode withEpisodeId(String episodeId) {
//             return new Episode(episodeId, this.title, this.plotSummary, this.airDate, this.rating, this.base);
//         }

//         public Episode withTitle(String title) {
//             return new Episode(this.episodeId, title, this.plotSummary, this.airDate, this.rating, this.base);
//         }

//         public Episode withPlotSummary(String plotSummary) {
//             return new Episode(this.episodeId, this.title, plotSummary, this.airDate, this.rating, this.base);
//         }

//         public Episode withAirDate(LocalDate airDate) {
//             return new Episode(this.episodeId, this.title, this.plotSummary, airDate, this.rating, this.base);
//         }

//         public Episode withRating(Float rating) {
//             return new Episode(this.episodeId, this.title, this.plotSummary, this.airDate, rating, this.base);
//         }

//         public Episode withBase(Base base) {
//             return new Episode(this.episodeId, this.title, this.plotSummary, this.airDate, this.rating, base);
//         }
//     }

//     /**
//      * (String movieId, String title, String plotSummary, LocalDate releaseDate,
//      * Float rating)
//      */
//     public static record Movie(String movieId, String title, String plotSummary, LocalDate releaseDate, Float rating,
//             Base base)
//             implements MetaData {

//         public Movie withMovieId(String movieId) {
//             return new Movie(movieId, this.title, this.plotSummary, this.releaseDate, this.rating, this.base);
//         }

//         public Movie withTitle(String title) {
//             return new Movie(this.movieId, title, this.plotSummary, this.releaseDate, this.rating, this.base);
//         }

//         public Movie withPlotSummary(String plotSummary) {
//             return new Movie(this.movieId, this.title, plotSummary, this.releaseDate, this.rating, this.base);
//         }

//         public Movie withReleaseDate(LocalDate releaseDate) {
//             return new Movie(this.movieId, this.title, this.plotSummary, releaseDate, this.rating, this.base);
//         }

//         public Movie withRating(Float rating) {
//             return new Movie(this.movieId, this.title, this.plotSummary, this.releaseDate, rating, this.base);
//         }

//         public Movie withBase(Base base) {
//             return new Movie(this.movieId, this.title, this.plotSummary, this.releaseDate, this.rating, base);
//         }
//     }

//     /**
//      * (String seriesId, String title, String plotSummary, LocalDate firstAirDate,
//      * Float rating)
//      */
//     public static record Series(String seriesId, String title, String plotSummary, LocalDate firstAirDate, Float rating,
//             List<Season> seasons, Base base)
//             implements MetaData {

//         public Series withSeriesId(String seriesId) {
//             return new Series(seriesId, this.title, this.plotSummary, this.firstAirDate, this.rating, this.seasons,
//                     this.base);
//         }

//         public Series withTitle(String title) {
//             return new Series(this.seriesId, title, this.plotSummary, this.firstAirDate, this.rating, this.seasons,
//                     this.base);
//         }

//         public Series withPlotSummary(String plotSummary) {
//             return new Series(this.seriesId, this.title, plotSummary, this.firstAirDate, this.rating, this.seasons,
//                     this.base);
//         }

//         public Series withFirstAirDate(LocalDate firstAirDate) {
//             return new Series(this.seriesId, this.title, this.plotSummary, firstAirDate, this.rating, this.seasons,
//                     this.base);
//         }

//         public Series withRating(Float rating) {
//             return new Series(this.seriesId, this.title, this.plotSummary, this.firstAirDate, rating, this.seasons,
//                     this.base);
//         }

//         public Series addSeason(Season season) {
//             List<Season> updatedSeasons = this.seasons == null ? List.of(season)
//                     : new java.util.ArrayList<>(this.seasons);
//             if (this.seasons != null) {
//                 updatedSeasons.add(season);
//             }
//             return new Series(this.seriesId, this.title, this.plotSummary, this.firstAirDate, this.rating,
//                     updatedSeasons, this.base);
//         }

//         public Series withSeasons(List<Season> seasons) {
//             return new Series(this.seriesId, this.title, this.plotSummary, this.firstAirDate, this.rating, seasons,
//                     this.base);
//         }

//         public Series withBase(Base base) {
//             return new Series(this.seriesId, this.title, this.plotSummary, this.firstAirDate, this.rating, this.seasons,
//                     base);
//         }
//     }

//     public static record Season(String seriesId, int seasonNumber, List<Episode> episodes, Base base)
//             implements MetaData {

//         public Season withSeriesId(String seriesId) {
//             return new Season(seriesId, this.seasonNumber, this.episodes, this.base);
//         }

//         public Season withSeasonNumber(int seasonNumber) {
//             return new Season(this.seriesId, seasonNumber, this.episodes, this.base);
//         }

//         public Season addEpisode(Episode episode) {
//             List<Episode> updatedEpisodes = this.episodes == null ? List.of(episode)
//                     : new java.util.ArrayList<>(this.episodes);
//             if (this.episodes != null) {
//                 updatedEpisodes.add(episode);
//             }
//             return new Season(this.seriesId, this.seasonNumber, updatedEpisodes, this.base);
//         }

//         public Season withEpisodes(List<Episode> episodes) {
//             return new Season(this.seriesId, this.seasonNumber, episodes, this.base);
//         }

//         public Season withBase(Base base) {
//             return new Season(this.seriesId, this.seasonNumber, this.episodes, base);
//         }
//     }

//     public static enum Status {
//         SUCCESS, FAILURE, NOT_FOUND
//     }
// }
