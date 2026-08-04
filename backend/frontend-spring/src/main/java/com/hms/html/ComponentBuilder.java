package com.hms.html;

import org.jsoup.nodes.Element;

public interface ComponentBuilder extends Builder {
    public Element build() throws Exception;
}
