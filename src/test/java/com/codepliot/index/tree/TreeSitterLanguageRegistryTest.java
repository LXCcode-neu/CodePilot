package com.codepliot.index.tree;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codepliot.index.detect.LanguageType;
import org.junit.jupiter.api.Test;

class TreeSitterLanguageRegistryTest {

    @Test
    void shouldAlwaysMarkUnknownLanguageAsUnavailable() {
        TreeSitterLanguageRegistry registry = new TreeSitterLanguageRegistry();

        TreeSitterLanguageRegistry.TreeSitterLanguageHandle handle = registry.get(LanguageType.UNKNOWN);

        assertFalse(handle.available());
        assertNotNull(handle.unavailableReason());
    }

    @Test
    void shouldExposeSupportedLanguageAvailabilityWithoutThrowing() {
        TreeSitterLanguageRegistry registry = new TreeSitterLanguageRegistry();

        TreeSitterLanguageRegistry.TreeSitterLanguageHandle handle = registry.get(LanguageType.JAVA);

        if (registry.isNativeLibraryLoaded()) {
            assertTrue(handle.available());
        } else {
            assertFalse(handle.available());
            assertTrue(registry.getNativeLibraryErrorMessage().isPresent());
            assertNotNull(handle.unavailableReason());
        }
    }
}
