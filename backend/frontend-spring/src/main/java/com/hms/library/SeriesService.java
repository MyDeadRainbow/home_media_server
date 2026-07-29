package com.hms.library;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.hms.shared.media.Series;
import com.hms.shared.messaging.JsonSerializable;

@Service
public class SeriesService {
    @Value("${API_GATEWAY_URL:http://localhost:8080}")
    private String apiGatewayUrl;

    @Value("${API_KEY:dev-local-key}")
    private String apiKey;

    public List<Series> getSeries(String query) {
        // Implement the logic to fetch movies from the API Gateway using the provided
        // query
        // You can use RestTemplate or WebClient to make HTTP requests
        // Make sure to include the API key in the request headers for authentication
        // Return a list of Series objects representing the movies
        List<Series> seriesList = new ArrayList<>();
        try (HttpClient client = HttpClient.newHttpClient();) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiGatewayUrl + "/api/media/series?query=" + query))
                    .GET()
                    .header("X-API-Key", apiKey)
                    .build();
            String response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            JsonArray jsonResponse = JsonParser.parseString(response).getAsJsonArray();
            for (int i = 0; i < jsonResponse.size(); i++) {
                seriesList.add(JsonSerializable.fromJson(jsonResponse.get(i).toString(), Series.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return seriesList;
    }

    public Series getSeriesById(String seriesId) {
        // Implement the logic to fetch a movie by its ID from the API Gateway
        // Make sure to include the API key in the request headers for authentication
        // Return a Series object representing the movie
        try (HttpClient client = HttpClient.newHttpClient();) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiGatewayUrl + "/api/media/series/" + seriesId))
                    .GET()
                    .header("X-API-Key", apiKey)
                    .build();
            String response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            return JsonSerializable.fromJson(response, Series.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
