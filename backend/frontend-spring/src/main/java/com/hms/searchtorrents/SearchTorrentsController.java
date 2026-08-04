package com.hms.searchtorrents;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hms.HtmlRestController;
import com.hms.shared.json.ImportMediaRequest;
import com.hms.shared.json.SearchResponse;
import com.hms.shared.messaging.JsonSerializable;

import io.mikael.urlbuilder.UrlBuilder;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class SearchTorrentsController extends HtmlRestController {

    @Value("${API_GATEWAY_URL:http://localhost:8080}")
    private String apiGatewayUrl;

    @Value("${API_KEY:dev-local-key}")
    private String apiKey;

    // @GetMapping(path = "/search", produces = "text/html")
    // public ResponseEntity<String> getSearchTorrentsPage() {
    // try {
    // return
    // ResponseEntity.ok(buildDocument("templates/search/index.html").html());
    // } catch (Exception e) {
    // e.printStackTrace();
    // return ResponseEntity.status(500).body("Error reading search/index.html");
    // }
    // }

    @GetMapping(path = "/search", produces = "text/html")
    public ResponseEntity<String> getSearch(@RequestParam(required = false) String query,
            @RequestParam(required = false) String category) {
        try {
            Document doc = buildDocument("templates/search/index.html");
            doc.selectFirst("[rid=nav-links]").children().forEach((li) -> {
                Element a = li.selectFirst("a");
                if (a != null) {
                    String rid = a.attr("rid");
                    if ("search".equals(rid)) {
                        a.addClass("active");
                    } else {
                        a.removeClass("active");
                    }
                }
            });
            if (query == null || query.isEmpty() || category == null || category.isEmpty()) {
                return ResponseEntity.ok(doc.html());
            }

            doc.selectFirst("[rid=search-query]").attr("value", query);
            // doc.selectFirst("[rid=search-category]").attr("value", category);

            doc.selectFirst("[rid=search-category]")
                    .select("option")
                    .removeAttr("selected")
                    .selectFirst("option[value=" + category + "]")
                    .attr("selected", "selected");

            try (HttpClient client = HttpClient.newHttpClient();) {
                UrlBuilder urlBuilder = UrlBuilder.fromString(apiGatewayUrl)
                        .withPath("/api/acquisition/search")
                        .addParameter("query", query)
                        .addParameter("category", category);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(urlBuilder.toUri())
                        .header("Accept", "application/json")
                        .header("Cache-Control", "no-cache")
                        .header("X-API-Key", apiKey)
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Failed to fetch search results: " + response.body());
                }
                String searchResultsJson = response.body();
                JsonArray searchResultsArray = JsonParser.parseString(searchResultsJson).getAsJsonArray();

                Element searchResultList = doc.selectFirst("[rid=search-results-list]");
                Element searchMessage = doc.selectFirst("[rid=search-message]");
                if (searchResultsArray.size() == 0) {
                    searchMessage.text("No results found for query: " + query);
                } else {
                    searchMessage.remove();
                }

                Element searchResultTemplate = buildComponent("components/torrent_search_result_item.html");
                for (JsonElement jsonElement : searchResultsArray) {
                    SearchResponse searchResponse = JsonSerializable.fromJsonObject(jsonElement.getAsJsonObject(),
                            SearchResponse.class);
                    Element searchResultItem = searchResultTemplate.clone();
                    searchResultItem.selectFirst("[rid=title]").text(searchResponse.title());
                    searchResultItem.selectFirst("[rid=magnet-link]").val(searchResponse.magnetLink());
                    searchResultItem.selectFirst("[rid=title-input]").val(searchResponse.title());
                    searchResultItem.selectFirst("[rid=category-input]").val(category);
                    // searchResultItem.selectFirst("[rid=source-url]").attr("href",
                    // searchResponse.sourceUrl());
                    searchResultItem.selectFirst("[rid=size]").text(searchResponse.size());
                    searchResultItem.selectFirst("[rid=seeders]").text(searchResponse.seeders());
                    searchResultItem.selectFirst("[rid=leechers]").text(searchResponse.leechers());
                    searchResultItem.selectFirst("[rid=category]").text(category);
                    searchResultItem.selectFirst("[rid=source]").text(searchResponse.source());
                    searchResultList.appendChild(searchResultItem);
                }

                return ResponseEntity.ok(doc.html());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error processing search request");
        }
    }

    @PostMapping("/import")
    public ResponseEntity<String> postImport(@ModelAttribute ImportMediaRequest importRequest) {
        try (HttpClient client = HttpClient.newHttpClient();) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiGatewayUrl + "/api/stream/importRequest"))
                    .header("Content-Type", "application/json")
                    .header("Cache-Control", "no-cache")
                    .header("X-API-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(importRequest.toJson().toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (Boolean.parseBoolean(response.body())) {
                return ResponseEntity.ok("Import request submitted successfully for: " + importRequest.title());
            } else {
                return ResponseEntity.status(500).body("Failed to submit import request for: " + importRequest.title());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception appropriately
            return ResponseEntity.status(500).body("Error processing import request for: " + importRequest.title());
        }
    }

}
