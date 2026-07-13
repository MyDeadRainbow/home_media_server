package com.hms.catalog.datamine.api;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hms.shared.media.Episode;
import com.hms.shared.media.Movie;
import com.hms.shared.media.Season;
import com.hms.shared.media.Series;
import com.hms.shared.media.metadata.MetaData;
import com.hms.shared.media.metadata.MetaDataStatus;
import com.hms.shared.media.poster.Poster;

import io.mikael.urlbuilder.UrlBuilder;

public class TMDBApi extends MediaDbApi {
    
    private static final Logger log = LoggerFactory.getLogger(TMDBApi.class);

    protected TMDBApi(String apiKey) {
        this.apiKey = apiKey;
    }

    private final String apiKey;
    private final String baseUrl = "https://api.themoviedb.org";
    private final String versionPath = "/3";
    private final String searchPath = "/search";
    private final String tvPath = "/tv";
    private final String moviePath = "/movie";
    private final String queryParameter = "query";
    private final String seasonPath = "/season";
    private String imageUrl = "https://image.tmdb.org"; // Base URL for TMDB images
    private String imageUrlPath = "/t/p/original"; // Path for original size images

    // https://api.themoviedb.org/3/search/tv
    @Override
    public Series searchSeriesImpl(Series series) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            UrlBuilder urlBuilder = UrlBuilder.fromString(baseUrl)
                    .withPath(versionPath + searchPath + tvPath)
                    .addParameter(queryParameter, series.title());

            HttpRequest request = addHeaders(HttpRequest.newBuilder())
                    .uri(urlBuilder.toUri())
                    .GET()
                    .build();

            String tmdbSeriesId = null;
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    // Handle non-200 response
                    log.error("Error: Received non-200 response: {}", response.statusCode());
                    series = series.withMetaData(series.metaData().withStatus(MetaDataStatus.ERROR)
                            .withMessage("Failed to fetch series details from TMDB"));
                    return series;
                }

                // Process the response body here
                JsonArray results = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("results");
                if (results.size() == 0) {
                    log.error("Error: No results found for series: {}", series.title());
                    series = series.withMetaData(series.metaData().withStatus(MetaDataStatus.INCOMPLETE)
                            .withMessage("No results found for title"));
                    return series;
                }

                JsonObject json = results.get(0).getAsJsonObject();
                if (json == null || !json.isJsonObject()) {
                    log.error("Error: No results found for series: {}", series.title());
                    series = series.withMetaData(series.metaData().withStatus(MetaDataStatus.INCOMPLETE)
                            .withMessage("No results found for title"));
                    return series;
                }

                MetaData metaData = series.metaData();
                metaData = metaData.withTitle(json.get("original_name").getAsString())
                        .withPlotSummary(json.get("overview").getAsString())
                        .withAirDate(
                                LocalDate.parse(json.get("first_air_date").getAsString(), DateTimeFormatter.ISO_DATE))
                        .withRating(json.get("vote_average").getAsFloat())
                        .withStatus(MetaDataStatus.COMPLETE);

                series = series.withMetaData(metaData);
                String posterPath = json.get("poster_path").getAsString();
                if (posterPath != null && !posterPath.isEmpty()) {
                    posterPath = UrlBuilder.fromString(imageUrl).withPath(imageUrlPath + posterPath).toString();
                    Poster seriesPoster = Optional.ofNullable(series.poster()).orElse(Poster.create(null));
                    seriesPoster = seriesPoster.withUrl(posterPath);
                    series = series.withPoster(seriesPoster);
                }

                tmdbSeriesId = json.get("id").getAsString();
            } catch (Exception e) {
                log.error("Exception occurred while searching for series: {}", e.getMessage(), e);
                series = series.withMetaData(series.metaData().withStatus(MetaDataStatus.ERROR)
                        .withMessage("Failed to fetch series details from TMDB"));
                return series;
            }

            if (tmdbSeriesId == null) {
                return series;
            }

            for (Season season : series.seasons()) {
                int seasonNumber = season.seasonNumber();
                try {
                    UrlBuilder urlBuilderDetails = UrlBuilder.fromString(baseUrl)
                            .withPath(versionPath + tvPath + "/" + tmdbSeriesId + seasonPath + "/" + seasonNumber);

                    HttpRequest requestDetails = addHeaders(HttpRequest.newBuilder())
                            .uri(urlBuilderDetails.toUri())
                            .GET()
                            .build();

                    HttpResponse<String> responseDetails = client.send(requestDetails,
                            HttpResponse.BodyHandlers.ofString());
                    if (responseDetails.statusCode() != 200) {
                        log.error("Error: Received non-200 response for season details: {}",
                                responseDetails.statusCode());
                        season = season.withMetaData(season.metaData().withStatus(MetaDataStatus.ERROR)
                                .withMessage("Failed to fetch season details from TMDB"));
                        series = series.replaceSeason(season);
                        return series;
                    }

                    // Process the response body here
                    JsonObject jsonDetails = JsonParser.parseString(responseDetails.body()).getAsJsonObject();
                    MetaData metaData = season.metaData();
                    metaData = metaData.withTitle(jsonDetails.get("name").getAsString())
                            .withPlotSummary(jsonDetails.get("overview").getAsString())
                            .withAirDate(
                                    LocalDate.parse(jsonDetails.get("air_date").getAsString(),
                                            DateTimeFormatter.ISO_DATE))
                            .withRating(jsonDetails.get("vote_average").getAsFloat())
                            .withStatus(MetaDataStatus.COMPLETE);

                    season = season.withMetaData(metaData);

                    String posterPath = jsonDetails.get("poster_path").getAsString();
                    if (posterPath != null && !posterPath.isEmpty()) {
                        posterPath = UrlBuilder.fromString(imageUrl).withPath(imageUrlPath + posterPath).toString();
                        Poster seasonPoster = Optional.ofNullable(season.poster()).orElse(Poster.create(null));
                        seasonPoster = seasonPoster.withUrl(posterPath);
                        season = season.withPoster(seasonPoster);
                    }

                    series = series.replaceSeason(season);

                    JsonArray episodesArray = jsonDetails.getAsJsonArray("episodes");                    
                    for (Episode episode : season.episodes()) {
                        int episodeNumber = episode.episodeNumber();
                        JsonObject episodeJson = episodesArray.asList().stream()
                                .map(e -> e.getAsJsonObject())
                                .filter(e -> e.get("episode_number").getAsInt() == episodeNumber)
                                .findFirst()
                                .orElse(null);
                        if (episodeJson == null) {
                            log.error("Error: No details found for episode number {} of season {} of series: {}",
                                    episodeNumber, seasonNumber, series.title());
                            episode = episode.withMetaData(episode.metaData().withStatus(MetaDataStatus.ERROR)
                                    .withMessage("Failed to fetch episode details from TMDB"));
                            season = season.replaceEpisode(episode);
                            series = series.replaceSeason(season);
                            continue;
                        }

                        MetaData episodeMetaData = episode.metaData();
                        episodeMetaData = episodeMetaData.withTitle(episodeJson.get("name").getAsString())
                                .withPlotSummary(episodeJson.get("overview").getAsString())
                                .withAirDate(LocalDate.parse(episodeJson.get("air_date").getAsString(),
                                        DateTimeFormatter.ISO_DATE))
                                .withRating(episodeJson.get("vote_average").getAsFloat())
                                .withStatus(MetaDataStatus.COMPLETE);

                        episode = episode.withMetaData(episodeMetaData);

                        String episodePosterPath = episodeJson.get("still_path").getAsString();
                        if (episodePosterPath != null && !episodePosterPath.isEmpty()) {
                            episodePosterPath = UrlBuilder.fromString(imageUrl)
                                    .withPath(imageUrlPath + episodePosterPath).toString();
                            Poster episodePoster = Optional.ofNullable(episode.poster()).orElse(Poster.create(null));
                            episodePoster = episodePoster.withUrl(episodePosterPath);
                            episode = episode.withPoster(episodePoster);
                        }

                        season = season.replaceEpisode(episode);
                        series = series.replaceSeason(season);
                    }
                } catch (Exception e) {
                    log.error("Exception occurred while processing episode details: {}", e.getMessage(), e);
                    season = season.withMetaData(season.metaData().withStatus(MetaDataStatus.ERROR)
                            .withMessage("Failed to fetch episode details from TMDB"));
                    series = series.replaceSeason(season);
                }
            }
        }
        return series;
    }

    // https://api.themoviedb.org/3/search/movie
    @Override
    public Movie searchMovieImpl(Movie movie) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            UrlBuilder urlBuilder = UrlBuilder.fromString(baseUrl)
                    .withPath(versionPath + searchPath + moviePath)
                    .addParameter(queryParameter, movie.title());

            HttpRequest request = addHeaders(HttpRequest.newBuilder())
                    .uri(urlBuilder.toUri())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                // Handle non-200 response
                log.error("Error: Received non-200 response: {}", response.statusCode());
                movie = movie.withMetaData(movie.metaData().withStatus(MetaDataStatus.ERROR)
                        .withMessage("Failed to fetch movie details from TMDB"));
                return movie;
            }

            JsonArray results = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("results");
            if (results.size() == 0) {
                log.error("Error: No results found for movie: {}", movie.title());
                movie = movie.withMetaData(movie.metaData().withStatus(MetaDataStatus.INCOMPLETE)
                        .withMessage("No results found for title"));
                return movie;
            }

            JsonElement firstResult = results.get(0);
            if (firstResult == null || !firstResult.isJsonObject()) {
                log.error("Error: No results found for movie: {}", movie.title());
                movie = movie.withMetaData(movie.metaData().withStatus(MetaDataStatus.INCOMPLETE)
                        .withMessage("No results found for title"));
                return movie;
            }

            JsonObject json = firstResult.getAsJsonObject();
            // Process the response body here
            MetaData metaData = movie.metaData();
            metaData = metaData.withTitle(json.get("original_title").getAsString())
                    .withPlotSummary(json.get("overview").getAsString())
                    .withAirDate(LocalDate.parse(json.get("release_date").getAsString(), DateTimeFormatter.ISO_DATE))
                    .withRating(json.get("vote_average").getAsFloat());
            movie = movie.withMetaData(metaData);

            String posterPath = json.get("poster_path").getAsString();
            if (posterPath != null && !posterPath.isEmpty()) {
                posterPath = UrlBuilder.fromString(imageUrl).withPath(imageUrlPath + posterPath).toString();
                Poster moviePoster = Optional.ofNullable(movie.poster()).orElse(Poster.create(null));
                moviePoster = moviePoster.withUrl(posterPath);
                movie = movie.withPoster(moviePoster);
            }
        } catch (Exception e) {
            log.error("Exception occurred while searching for movie: {}", e.getMessage(), e);
            movie = movie.withMetaData(movie.metaData().withStatus(MetaDataStatus.ERROR)
                    .withMessage("Failed to fetch movie details from TMDB"));
        }
        return movie;
    }

    private HttpRequest.Builder addHeaders(HttpRequest.Builder request) {
        return request.header("Authorization", "Bearer " + apiKey).header("Accept", "application/json");
    }

}
