package com.hms.html;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public abstract class AbstractBuilder {
    protected final ResourceLoader resourceLoader;

    AbstractBuilder() {
        this.resourceLoader = new DefaultResourceLoader();
    }

    public String resPath(String path) {
        return ResourceLoader.CLASSPATH_URL_PREFIX + path;
    }

    public String baseUrl() {
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .toUriString();

        } catch (IllegalStateException e) {
            // Handle the case when there is no current request context
            return "";
        }
    }
}
