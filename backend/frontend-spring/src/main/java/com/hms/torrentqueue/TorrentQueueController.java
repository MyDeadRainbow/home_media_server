package com.hms.torrentqueue;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.HtmlRestController;

@RestController
public class TorrentQueueController extends HtmlRestController {
    
    @GetMapping(path = "/torrents", produces = "text/html")
    public ResponseEntity<String> getTorrentQueuePage() {
        try {
            return ResponseEntity.ok(buildDocument("templates/torrents/index.html").html());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error reading torrents/index.html");
        }
    }
}
