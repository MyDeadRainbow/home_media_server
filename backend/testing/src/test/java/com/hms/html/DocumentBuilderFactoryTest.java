package com.hms.html;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

public class DocumentBuilderFactoryTest {
    
    @Test
    public void testDocumentBuilderFactory() {
        DocumentBuilder builder = assertDoesNotThrow(() -> DocumentBuilderFactory.getDocumentBuilder("index"));
        assertDoesNotThrow(() -> builder.buildDocument());
    }
}
