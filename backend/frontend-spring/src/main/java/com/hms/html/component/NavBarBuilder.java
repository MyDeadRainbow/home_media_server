package com.hms.html.component;

import org.jsoup.nodes.Element;

import com.hms.html.AbstractComponentBuilder;
import com.hms.html.ComponentField;
import com.hms.html.ComponentTag;

@ComponentTag(tagName = "navbar", path = "navbar.html")
public class NavBarBuilder extends AbstractComponentBuilder {    
    private final String currentPage;

    public NavBarBuilder(@ComponentField String currentPage) {
        super("navbar", "navbar.html");
        this.currentPage = currentPage;
    }

    @Override
    public Element build() throws Exception {
        Element navBarElement = super.build();
        navBarElement.selectFirst("[rid=nav-links]").children().forEach((li) -> {
            Element a = li.selectFirst("a");
            if (a != null) {
                String rid = a.attr("rid");
                if (currentPage.equals(rid)) {
                    a.addClass("active");
                } else {
                    a.removeClass("active");
                }
            }
        });
        return navBarElement;
    }

}
