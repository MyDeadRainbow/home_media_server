package com.hms.html;

import org.jsoup.nodes.Document;

public interface DocumentBuilder extends Builder{
    public Document buildDocument() throws Exception;
}
