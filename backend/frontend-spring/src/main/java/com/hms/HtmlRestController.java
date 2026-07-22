package com.hms;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.internal.SimpleStreamReader;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public abstract class HtmlRestController {

    @Autowired
    protected ResourceLoader resourceLoader;

    public Document buildDocument(String path) throws Exception {
        Resource resource = resourceLoader.getResource(resPath(path));
        try (InputStream inputStream = resource.getInputStream()) {
            // Read the contents of the file
            Document doc = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), baseUrl());
            doc.outputSettings(doc.outputSettings().prettyPrint(true));

            Elements headComponents = doc.select("head-component[src]");
            for (Element component : headComponents) {
                String src = component.attr("src");
                if (src == null || src.isEmpty()) {
                    throw new IllegalArgumentException("Component src attribute is missing or empty");
                } else if (!src.startsWith("components/")) {
                    src = "components/" + src;
                }
                Element head = doc.head();
                Elements headElements = buildHeadComponent(src);
                head.appendChildren(headElements);
                component.remove();
            }

            Elements components = doc.select("component[src]");
            for (Element component : components) {
                String src = component.attr("src");
                if (src == null || src.isEmpty()) {
                    throw new IllegalArgumentException("Component src attribute is missing or empty");
                } else if (!src.startsWith("components/")) {
                    src = "components/" + src;
                }
                Element componentElement = buildComponent(src);
                component.replaceWith(componentElement);
            }

            return doc;
        }
    }

    public Element buildComponent(String path) throws Exception {
        Resource resource = resourceLoader.getResource(resPath(path));
        try (InputStream inputStream = resource.getInputStream()) {
            // Read the contents of the file
            // List<Node> nodes = Parser.htmlParser().parseFragmentInput(reader, null, baseUrl());
            Document baseDoc = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), baseUrl());
            Element el = baseDoc.selectFirst("component").child(0);            
            return el;
        }
    }

    public Elements buildHeadComponent(String path) throws Exception {
        Resource resource = resourceLoader.getResource(resPath(path));
        try (InputStream inputStream = resource.getInputStream()) {
            // Read the contents of the file
            Document baseDoc = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), baseUrl());
            return baseDoc.select("head-component").first().children();
        }
    }

    public String resPath(String path) {
        return ResourceLoader.CLASSPATH_URL_PREFIX + path;
    }

    public String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .toUriString();
    }
}
