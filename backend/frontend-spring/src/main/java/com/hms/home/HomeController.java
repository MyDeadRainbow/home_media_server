package com.hms.home;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jsoup.nodes.Document;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.HtmlRestController;
import com.hms.html.DocumentBuilderFactory;

@RestController
public class HomeController extends HtmlRestController {

    @GetMapping(path = "/", produces = "text/html")
    public ResponseEntity<String> home() {
        try {
            Document doc = DocumentBuilderFactory.getDocumentBuilder("index").buildDocument();
            // Document doc = buildDocument("templates/index.html");
            // doc.selectFirst("[rid=nav-links]").children().forEach((li) -> {
            //     Element a = li.selectFirst("a");
            //     if (a != null) {
            //         String rid = a.attr("rid");
            //         if ("home".equals(rid)) {
            //             a.addClass("active");
            //         } else {
            //             a.removeClass("active");
            //         }
            //     }
            // });
            return ResponseEntity.ok(doc.html());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error reading index.html");
        }
    }

    @GetMapping(path = "/styles.css", produces = "text/css")
    public ResponseEntity<String> getStyle() {
        Resource resource = resourceLoader.getResource(resPath("templates/styles.css"));
        // resource.
        try (InputStream inputStream = resource.getInputStream()) {
            // Read the contents of the file
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            return ResponseEntity.ok(content);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error reading styles.css");
        }
    }

}
