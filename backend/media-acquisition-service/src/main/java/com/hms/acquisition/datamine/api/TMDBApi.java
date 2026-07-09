package com.hms.acquisition.datamine.api;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
import com.hms.shared.media.poster.Poster;

import io.mikael.urlbuilder.UrlBuilder;

public class TMDBApi implements MediaDbApi {

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

    // https://api.themoviedb.org/3/search/tv
    @Override
    public Series searchSeries(Series series) {
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
                    return series;
                }

                // Process the response body here
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject().get("results")
                        .getAsJsonArray().get(0).getAsJsonObject();
                MetaData metaData = series.metaData();
                metaData = metaData.withTitle(json.get("original_name").getAsString())
                        .withPlotSummary(json.get("overview").getAsString())
                        .withAirDate(
                                LocalDate.parse(json.get("first_air_date").getAsString(), DateTimeFormatter.ISO_DATE))
                        .withRating(json.get("vote_average").getAsFloat());

                series = series.withMetaData(metaData);
                tmdbSeriesId = json.get("id").getAsString();
                String posterPath = json.get("poster_path").getAsString();
                byte[] posterData = fetchImage(posterPath);
                if (posterData != null) {
                    Poster seriesPoster = Optional.ofNullable(series.poster()).orElse(Poster.create(null));
                    seriesPoster = seriesPoster.withImageData(posterData);
                    series = series.withPoster(seriesPoster);
                }
            } catch (IOException e) {
                log.error("IOException occurred while searching for series: {}", e.getMessage(), e);
            } catch (InterruptedException e) {
                log.error("InterruptedException occurred while searching for series: {}", e.getMessage(), e);
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
                            .withRating(jsonDetails.get("vote_average").getAsFloat());

                    season = season.withMetaData(metaData);
                    
                    String posterPath = jsonDetails.get("poster_path").getAsString();
                    byte[] posterData = fetchImage(posterPath);
                    if (posterData != null) {
                        Poster seasonPoster = Optional.ofNullable(season.poster()).orElse(Poster.create(null));
                        seasonPoster = seasonPoster.withImageData(posterData);
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
                            continue;
                        }

                        MetaData episodeMetaData = episode.metaData();
                        episodeMetaData = episodeMetaData.withTitle(episodeJson.get("name").getAsString())
                                .withPlotSummary(episodeJson.get("overview").getAsString())
                                .withAirDate(LocalDate.parse(episodeJson.get("air_date").getAsString(),
                                        DateTimeFormatter.ISO_DATE))
                                .withRating(episodeJson.get("vote_average").getAsFloat());

                        episode = episode.withMetaData(episodeMetaData);

                        String episodePosterPath = episodeJson.get("still_path").getAsString();
                        byte[] episodePosterData = fetchImage(episodePosterPath);
                        if (episodePosterData != null) {
                            Poster episodePoster = Optional.ofNullable(episode.poster()).orElse(Poster.create(null));
                            episodePoster = episodePoster.withImageData(episodePosterData);
                            episode = episode.withPoster(episodePoster);
                        }

                        season = season.replaceEpisode(episode);
                        series = series.replaceSeason(season);
                    }
                } catch (IOException e) {
                    log.error("IOException occurred while processing episode details: {}", e.getMessage(), e);
                } catch (InterruptedException e) {
                    log.error("InterruptedException occurred while processing episode details: {}", e.getMessage(), e);
                }
            }
        }
        return series;
    }

    // https://api.themoviedb.org/3/search/movie
    @Override
    public Movie searchMovie(Movie movie) {
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
                return movie;
            }

            JsonElement firstResult = JsonParser.parseString(response.body()).getAsJsonObject()
                    .getAsJsonArray("results").get(0);
            if (firstResult == null || !firstResult.isJsonObject()) {
                log.error("Error: No results found for movie: {}", movie.title());
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
            byte[] posterData = fetchImage(posterPath);
            if (posterData != null) {
                Poster moviePoster = Optional.ofNullable(movie.poster()).orElse(Poster.create(null));
                moviePoster = moviePoster.withImageData(posterData);
                movie = movie.withPoster(moviePoster);
            }
        } catch (Exception e) {
            log.error("Exception occurred while searching for movie: {}", e.getMessage(), e);
        }
        return movie;
    }

    private HttpRequest.Builder addHeaders(HttpRequest.Builder request) {
        return request.header("Authorization", "Bearer " + apiKey).header("Accept", "application/json");
    }

    private String imageUrl = "https://image.tmdb.org"; // Base URL for TMDB images
    private String imageUrlPath = "/t/p/original"; // Path for original size images

    private byte[] fetchImage(String imagePath) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            UrlBuilder urlBuilder = UrlBuilder.fromString(imageUrl).withPath(imageUrlPath + imagePath);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(urlBuilder.toUri())
                    .GET()
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                log.error("Error: Received non-200 response while fetching image: {}", response.statusCode());
                return null;
            }
            return response.body();
        } catch (Exception e) {
            log.error("Exception occurred while fetching image: {}", e.getMessage(), e);
            return null;
        }
    }
}
