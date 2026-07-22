package com.hms.upload;

import org.jsoup.nodes.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.HtmlRestController;

@RestController
public class UploadController extends HtmlRestController {
    
    @GetMapping(path = "/upload", produces = "text/html")
    public ResponseEntity<String> getUploadPage() {
        try {
            Document doc = buildDocument("templates/upload/index.html");
            return ResponseEntity.ok(doc.html());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error reading upload/index.html");
        }
    }
}
