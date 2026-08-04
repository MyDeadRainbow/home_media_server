package com.hms.html;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.hms.html.component.DefaultComponentBuilder;

public abstract class AbstractDocumentBuilder extends AbstractBuilder implements DocumentBuilder {
    private final String src;

    protected AbstractDocumentBuilder(String src) {
        super();
        this.src = src;
    }

    protected String getSrc() {
        return src;
    }

    @Override
    public Document buildDocument() throws Exception {
        Resource resource = resourceLoader.getResource(resPath(src));
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
                Element componentElement = new DefaultComponentBuilder(src).build();// buildComponent(src);
                component.replaceWith(componentElement);
            }

            Elements customComponents = doc.select(ComponentBuilderFactory.componentsSelector());
            for (Element component : customComponents) {
                ComponentBuilder builder = ComponentBuilderFactory.getComponentBuilder(component);
                Element componentElement = builder.build();
                component.replaceWith(componentElement);
            }

            return doc;
        }
    }

    public Elements buildHeadComponent(String path) throws Exception {
        String resourcePath = path.startsWith("components") ? path : "components/" + path;
        Resource resource = resourceLoader.getResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            // Read the contents of the file
            Document baseDoc = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), baseUrl());
            return baseDoc.select("head-component").first().children();
        }
    }

    @Override
    public String resPath(String path) {
        if (path.startsWith("templates")) {
            return ResourceLoader.CLASSPATH_URL_PREFIX + path;
        }
        return ResourceLoader.CLASSPATH_URL_PREFIX + "templates/" + path;
    }
}
