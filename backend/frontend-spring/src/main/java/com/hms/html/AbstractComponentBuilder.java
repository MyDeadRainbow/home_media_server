package com.hms.html;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public abstract class AbstractComponentBuilder extends AbstractBuilder implements ComponentBuilder {
    private final String tagName;
    private final String src;

    protected AbstractComponentBuilder(String tagName, String src) {
        super();
        this.tagName = tagName;
        this.src = src;
    }

    protected String getTagName() {
        return tagName;
    }

    protected String getSrc() {
        return src;
    }

    @Override
    public Element build() throws Exception {
        Resource resource = resourceLoader.getResource(resPath(getSrc()));
        try (InputStream inputStream = resource.getInputStream()) {
            Document baseDoc = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), baseUrl());
            Element el = baseDoc.selectFirst("component").child(0);            
            return el;
        }
    }

    @Override
    public String resPath(String path) {
        return ResourceLoader.CLASSPATH_URL_PREFIX + "components/" + path;
    }
}
