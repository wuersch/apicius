package dev.apicius.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;

/** Assertions on persisted/derived documents, shared across test classes. */
public final class JsonAssertions {

    private JsonAssertions() {
    }

    /** FEAT-005 AC3: nothing Apicius-specific in the document — no {@code x-} key anywhere. */
    public static void assertNoVendorExtensions(JsonNode node) {
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(name ->
                    assertFalse(name.startsWith("x-"), "unexpected vendor extension: " + name));
        }
        for (JsonNode child : node) {
            assertNoVendorExtensions(child);
        }
    }
}
