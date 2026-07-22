package com.hms.library;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.hms.shared.media.Movie;
import com.hms.shared.media.Series;
import com.hms.shared.messaging.JsonSerializable;

@Service
public class MovieService {
    @Value("${API_GATEWAY_URL:http://localhost:8080}")
    private String apiGatewayUrl;

    @Value("${API_KEY:dev-local-key}")
    private String apiKey;

    public List<Movie> getMovies(String query) {
        // Implement the logic to fetch movies from the API Gateway using the provided
        // query
        // You can use RestTemplate or WebClient to make HTTP requests
        // Make sure to include the API key in the request headers for authentication
        // Return a list of Movie objects representing the movies
        List<Movie> movieList = new ArrayList<>();
        try (HttpClient client = HttpClient.newHttpClient();) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiGatewayUrl + "/api/media/movies?query=" + query))
                    .GET()
                    .header("X-API-Key", apiKey)
                    .build();
            String response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            JsonArray jsonResponse = JsonParser.parseString(response).getAsJsonArray();
            for (int i = 0; i < jsonResponse.size(); i++) {
                movieList.add(JsonSerializable.fromJson(jsonResponse.get(i).toString(), Movie.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return movieList;
    }
}
