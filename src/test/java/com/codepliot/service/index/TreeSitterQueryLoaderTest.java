package com.codepliot.service.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codepliot.service.index.LanguageType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TreeSitterQueryLoaderTest {

    private final TreeSitterQueryLoader queryLoader = new TreeSitterQueryLoader();

    @Test
    void shouldLoadExistingQueryFromClasspath() {
        Optional<String> query = queryLoader.load(LanguageType.JAVA, "symbols");

        assertTrue(query.isPresent());
        assertTrue(query.orElseThrow().contains("class_declaration"));
    }

    @Test
    void shouldReturnEmptyWhenQueryDoesNotExist() {
        Optional<String> query = queryLoader.load(LanguageType.GO, "symbols");

        assertFalse(query.isPresent());
    }

    @Test
    void shouldReturnEmptyForInvalidArguments() {
        assertEquals(Optional.empty(), queryLoader.load(null, "symbols"));
        assertEquals(Optional.empty(), queryLoader.load(LanguageType.JAVA, ""));
    }
}


