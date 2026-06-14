package com.hms.shared.scrape;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SearchParameter {
    String tag;
    String value;

    public SearchParameter(String tag, String value) {
        this.tag = tag;
        this.value = value;
    }

    public String toUrlParameter() {
        return "&" + URLEncoder.encode(tag, StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
