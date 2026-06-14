package com.hms.shared.scrape;

public class SearchUrl {
    private final String url;

    public SearchUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getSearchUrl(SearchParameter... parameters) {
        StringBuilder searchUrl = new StringBuilder(url);
        if (!url.endsWith("?")) {
            searchUrl.append("?");
        }

        for (SearchParameter param : parameters) {
            searchUrl.append(param.toUrlParameter());
        }        
        return searchUrl.toString();

    }


    
}
