package com.hms.html;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import com.hms.html.component.NavBarBuilder;

public class ComponentBuilderFactoryTest {
    
    @Test
    public void testComponentBuilderFactory() {
        // ComponentBuilder builder = ComponentBuilderFactory.getComponentBuilder("navbar");
    }

    @Test
    public void testNavBarBuilder() throws Exception {
        String currentPage = "upload";
        NavBarBuilder navBarBuilder = new NavBarBuilder(currentPage);
        assertDoesNotThrow(() -> navBarBuilder.build());
    }
}
