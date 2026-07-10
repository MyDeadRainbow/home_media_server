package com.hms.catalog.datamine;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hms.shared.media.Movie;
import com.hms.shared.media.Series;
import com.hms.shared.media.metadata.MetaData;
import com.hms.shared.messaging.JsonSerializable;

import io.mikael.urlbuilder.UrlBuilder;

public class ImdbAdvancedSearchHandler {
    private final String API_URL = "https://caching.graphql.imdb.com/";

    private final String OPERATION_NAME_PARAMETER = "operationName";
    private final String OPERATION_NAME_VALUE = "AdvancedTitleSearch";

    private final String VARIABLES_PARAMETER = "variables";

    private record Variables(int first, String locale, String sortBy, String sortOrder,
            TitleTextConstraint titleTextConstraint, TitleTypeConstraint titleTypeConstraint)
            implements JsonSerializable {
        private record TitleTextConstraint(String searchTerm) implements JsonSerializable {
        }

        private record TitleTypeConstraint(String... anyTitleTypeIds) implements JsonSerializable {
        }

        Variables(int first, String locale, String sortBy, String sortOrder, String searchTerm,
                String... titleTypes) {
            this(first, locale, sortBy, sortOrder, new TitleTextConstraint(searchTerm),
                    new TitleTypeConstraint(titleTypes));
        }
    }

    private final String EXTENSIONS_PARAMETER = "extensions";

    private record Extensions(PersistedQuery persistedQuery) implements JsonSerializable {
        private record PersistedQuery(String sha256Hash, int version) implements JsonSerializable {
        }

        Extensions(String sha256Hash, int version) {
            this(new PersistedQuery(sha256Hash, version));
        }
    }

    private final String USER_AGENT_HEADER = "User-Agent";
    private final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";

    private final String REFERER_HEADER = "Referer";
    private final String REFERER = "https://www.imdb.com/";

    protected final String TITLE_TYPE_MOVIE = "feature";
    protected final String TITLE_TYPE_TV_MOVIE = "tv_movie";
    protected final String TITLE_TYPE_SERIES = "tv_series";
    protected final String TITLE_TYPE_MINI_SERIES = "tv_miniseries";

    public Series series(Series series) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(UrlBuilder.fromString(API_URL)
                            .addParameter(OPERATION_NAME_PARAMETER, OPERATION_NAME_VALUE)
                            .addParameter(VARIABLES_PARAMETER,
                                    new Variables(50, "en-US", "POPULARITY", "ASC", series.title(), TITLE_TYPE_SERIES,
                                            TITLE_TYPE_MINI_SERIES).toJson()
                                            .toString())
                            .toUri())
                    .header("Accept", "application/json")
                    .header(REFERER_HEADER, REFERER)
                    .header(USER_AGENT_HEADER, USER_AGENT)
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            // Handle non-200 response
                            System.err.println("Error: Received non-200 response: " + response.statusCode());
                            return;
                        }

                        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                        // Process the response body here
                        System.out.println("Response: " + json);
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    }).join();
        } catch (Exception e) {
            // Handle exceptions
        }
        return series;
    }

    public Movie movie(Movie movie) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(UrlBuilder.fromString(API_URL)
                            .addParameter(OPERATION_NAME_PARAMETER, OPERATION_NAME_VALUE)
                            .addParameter(VARIABLES_PARAMETER,
                                    new Variables(50, "en-US", "POPULARITY", "ASC", movie.title(), TITLE_TYPE_MOVIE,
                                            TITLE_TYPE_TV_MOVIE).toJson()
                                            .toString())
                            .addParameter(EXTENSIONS_PARAMETER, new Extensions("78932519bc74ceb6be628fe452c0e59a48bcf8ca91fc550dd5de43ab200acd52", 1).toJson().toString())
                            .toUri())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header(REFERER_HEADER, REFERER)
                    .header(USER_AGENT_HEADER, USER_AGENT)
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            // Handle non-200 response
                            System.err.println("Error: Received non-200 response: " + response.statusCode());
                            return;
                        }

                        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                        // Process the response body here
                        System.out.println("Response: " + json);
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    }).join();
        } catch (Exception e) {
            // Handle exceptions
        }
        return movie;
    }
}
