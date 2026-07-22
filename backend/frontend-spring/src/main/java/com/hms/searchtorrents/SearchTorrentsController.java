package com.hms.searchtorrents;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.HtmlRestController;

@RestController
public class SearchTorrentsController extends HtmlRestController {
    
    @GetMapping(path = "/search", produces = "text/html")
    public ResponseEntity<String> getSearchTorrentsPage() {
        try {
            return ResponseEntity.ok(buildDocument("templates/search/index.html").html());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error reading search/index.html");
        }
    }
}
