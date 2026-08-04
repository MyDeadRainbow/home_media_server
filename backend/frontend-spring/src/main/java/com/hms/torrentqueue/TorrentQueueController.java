package com.hms.torrentqueue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hms.HtmlRestController;
import com.hms.shared.json.TorrentInfoResponse;
import com.hms.shared.messaging.JsonSerializable;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class TorrentQueueController extends HtmlRestController {

    @Value("${API_GATEWAY_URL:http://localhost:8080}")
    private String apiGatewayUrl;

    @Value("${API_KEY:dev-local-key}")
    private String apiKey;

    @GetMapping(path = "/torrents", produces = "text/html")
    public ResponseEntity<String> getTorrentQueuePage() {
        try (HttpClient client = HttpClient.newHttpClient();) {
            Document doc = buildDocument("templates/torrents/index.html");
            doc.selectFirst("[rid=nav-links]").children().forEach((li) -> {
                Element a = li.selectFirst("a");
                if (a != null) {
                    String rid = a.attr("rid");
                    if ("torrents".equals(rid)) {
                        a.addClass("active");
                    } else {
                        a.removeClass("active");
                    }
                }
            });

            JsonArray torrentInfoArray;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiGatewayUrl + "/api/stream/torrent/info"))
                        .header("Content-Type", "application/json")
                        .header("X-API-Key", apiKey)
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                torrentInfoArray = JsonParser.parseString(response.body()).getAsJsonArray();
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(500).body("Error reading torrents/index.html");
            }

            Element torrentListElement = doc.selectFirst("[rid=torrent-list]");
            Element torrentCardTemplate = buildComponent("components/torrent/torrent_card.html");
            Element torrentActionsTemplate = buildComponent("components/torrent/torrent_card_action_button.html");

            for (JsonElement jsonElement : torrentInfoArray) {
                TorrentInfoResponse torrentInfo = JsonSerializable.fromJsonObject(jsonElement.getAsJsonObject(),
                        TorrentInfoResponse.class);
                Element torrentCard = torrentCardTemplate.clone();
                torrentCard.selectFirst("[rid=title]").text(torrentInfo.name());
                torrentCard.selectFirst("[rid=queue-position]").text(String.valueOf(torrentInfo.queuePosition()));
                torrentCard.selectFirst("[rid=torrent-hash]")
                        .text(Optional.ofNullable(torrentInfo.infoHash()).orElse(""));
                torrentCard.selectFirst("[rid=torrent-hash-input]")
                        .val(Optional.ofNullable(torrentInfo.infoHash()).orElse(""));
                torrentCard.selectFirst("[rid=current-status-input]")
                        .val(Optional.ofNullable(torrentInfo.importMediaStatus().name()).orElse(""));
                torrentCard.selectFirst("[rid=total]").text(formatBytes(torrentInfo.totalSize()));
                torrentCard.selectFirst("[rid=downloaded]").text(formatBytes(torrentInfo.downloadedSize()));
                torrentCard.selectFirst("[rid=download-speed]").text(formatBytes(torrentInfo.downloadSpeed()) + "/s");
                torrentCard.selectFirst("[rid=upload-speed]").text(formatBytes(torrentInfo.uploadSpeed()) + "/s");
                torrentCard.selectFirst("[rid=peers]").text(String.valueOf(torrentInfo.numPeers()));
                torrentCard.selectFirst("[rid=import-status]").text(torrentInfo.importMediaStatus().name());

                double percentage = (torrentInfo.totalSize() > 0)
                        ? (torrentInfo.downloadedSize() * 100.0 / torrentInfo.totalSize())
                        : 0.0;
                torrentCard.selectFirst("[rid=progress-fill]").attr("style", "width: " + percentage + "%;");
                torrentCard.selectFirst("[rid=progress-percentage]").text(
                        String.format("%.2f%%", percentage));

                Element torrentActionsList = torrentCard.selectFirst("[rid=torrent-actions]");
                Element torrentActionsForm = torrentActionsTemplate.clone();
                torrentActionsForm.attr("action", "/pause/" + torrentInfo.infoHash());
                torrentActionsForm.selectFirst("[rid=torrent-hash]").val(torrentInfo.infoHash());
                torrentActionsForm.selectFirst("[rid=submit]").text("Pause");
                torrentActionsList.appendChild(torrentActionsForm);

                torrentActionsForm = torrentActionsTemplate.clone();
                torrentActionsForm.attr("action", "/resume/" + torrentInfo.infoHash());
                torrentActionsForm.selectFirst("[rid=torrent-hash]").val(torrentInfo.infoHash());
                torrentActionsForm.selectFirst("[rid=submit]").text("Resume");
                torrentActionsList.appendChild(torrentActionsForm);

                torrentActionsForm = torrentActionsTemplate.clone();
                torrentActionsForm.attr("action", "/delete/" + torrentInfo.infoHash());
                torrentActionsForm.selectFirst("[rid=torrent-hash]").val(torrentInfo.infoHash());
                torrentActionsForm.selectFirst("[rid=submit]").text("Delete");
                torrentActionsList.appendChild(torrentActionsForm);

                torrentListElement.appendChild(torrentCard);
            }

            return ResponseEntity.ok(doc.html());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error reading torrents/index.html");
        }
    }

    @GetMapping(path = "/torrentInfo", produces = "text/html")
    public ResponseEntity<String> getTorrentInfo(@RequestParam String torrentHash, @RequestParam String currentStatus) {
        if (torrentHash == null || torrentHash.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing required parameter: torrentHash");
        }
        try (HttpClient client = HttpClient.newHttpClient();) {
            Element torrentInfoElement = buildComponent("components/torrent/torrent_card_info.html");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiGatewayUrl + "/api/stream/torrent/info/" + torrentHash))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            TorrentInfoResponse torrentInfo = JsonSerializable.fromJsonObject(
                    JsonParser.parseString(response.body()).getAsJsonObject(),
                    TorrentInfoResponse.class);

            switch (torrentInfo.importMediaStatus()) {
                case COMPLETED:
                case MAGNET_NOT_FOUND:
                case MAGNET_FETCH_FAILED:
                case FAILED:
                    return ResponseEntity.noContent().build();
                default:
                    break;
            }

            torrentInfoElement.selectFirst("[rid=torrent-hash-input]")
                    .val(Optional.ofNullable(torrentInfo.infoHash()).orElse(""));
            torrentInfoElement.selectFirst("[rid=current-status-input]")
                    .val(Optional.ofNullable(torrentInfo.importMediaStatus().name()).orElse(""));

            torrentInfoElement.selectFirst("[rid=total]").text(formatBytes(torrentInfo.totalSize()));
            torrentInfoElement.selectFirst("[rid=downloaded]").text(formatBytes(torrentInfo.downloadedSize()));
            torrentInfoElement.selectFirst("[rid=download-speed]")
                    .text(formatBytes(torrentInfo.downloadSpeed()) + "/s");
            torrentInfoElement.selectFirst("[rid=upload-speed]").text(formatBytes(torrentInfo.uploadSpeed()) + "/s");
            torrentInfoElement.selectFirst("[rid=peers]").text(String.valueOf(torrentInfo.numPeers()));
            torrentInfoElement.selectFirst("[rid=import-status]").text(torrentInfo.importMediaStatus().name());

            double percentage = (torrentInfo.totalSize() > 0)
                    ? (torrentInfo.downloadedSize() * 100.0 / torrentInfo.totalSize())
                    : 0.0;
            torrentInfoElement.selectFirst("[rid=progress-fill]").attr("style", "width: " + percentage + "%;");
            torrentInfoElement.selectFirst("[rid=progress-percentage]").text(
                    String.format("%.2f%%", percentage));

            return ResponseEntity.ok(torrentInfoElement.outerHtml());
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception appropriately
            return ResponseEntity.status(500).body("Error processing request for torrent hash: " + torrentHash);
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @PostMapping("/pause/{torrentHash}")
    public ResponseEntity<String> pauseTorrent(@PathVariable String torrentHash) {
        try (HttpClient client = HttpClient.newHttpClient();) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiGatewayUrl + "/api/stream/torrent/pause/" + torrentHash))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return ResponseEntity.status(response.statusCode()).body("Failed to pause torrent: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception appropriately
            return ResponseEntity.status(500).body("Error processing request for torrent hash: " + torrentHash);
        }
        return ResponseEntity.ok(torrentHash);
    }

    @PostMapping("/resume/{torrentHash}")
    public ResponseEntity<String> resumeTorrent(@PathVariable String torrentHash) {
        try (HttpClient client = HttpClient.newHttpClient();) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiGatewayUrl + "/api/stream/torrent/resume/" + torrentHash))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return ResponseEntity.status(response.statusCode())
                        .body("Failed to resume torrent: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception appropriately
            return ResponseEntity.status(500).body("Error processing request for torrent hash: " + torrentHash);
        }

        return ResponseEntity.ok(torrentHash);
    }

    @PostMapping("/delete/{torrentHash}")
    public ResponseEntity<String> deleteTorrent(@PathVariable String torrentHash) {
        try (HttpClient client = HttpClient.newHttpClient();) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiGatewayUrl + "/api/stream/torrent/delete/" + torrentHash))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return ResponseEntity.status(response.statusCode())
                        .body("Failed to delete torrent: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception appropriately
            return ResponseEntity.status(500).body("Error processing request for torrent hash: " + torrentHash);
        }

        return ResponseEntity.ok(torrentHash);
    }

}

record GetTorrentInfo(String torrentHash) {
}