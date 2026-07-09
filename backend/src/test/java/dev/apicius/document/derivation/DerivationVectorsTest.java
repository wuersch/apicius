package dev.apicius.document.derivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * The Java half of the shared-vector contract: every vector in
 * {@code derivation-vectors.json} asserted against {@link CanonicalDerivation}. The TS half is
 * {@code frontend/src/lib/derivation.test.ts}, reading the same file — together they keep the
 * backend writer and the frontend presentation mirror from drifting.
 */
class DerivationVectorsTest {

    private static final JsonNode VECTORS = load();

    @TestFactory
    Stream<DynamicTest> validNamesDeriveExactlyTheVector() {
        return stream(VECTORS.path("valid")).map(vector -> DynamicTest.dynamicTest(
                vector.path("name").asText(), () -> assertVector(vector)));
    }

    @TestFactory
    Stream<DynamicTest> invalidNamesFailThePattern() {
        return stream(VECTORS.path("invalid")).map(name -> DynamicTest.dynamicTest(
                "\"" + name.asText() + "\"",
                () -> assertFalse(name.asText().matches(CanonicalDerivation.NAME_PATTERN),
                        "must be rejected by NAME_PATTERN")));
    }

    private static void assertVector(JsonNode vector) {
        String name = vector.path("name").asText();
        assertTrue(name.matches(CanonicalDerivation.NAME_PATTERN), "vector names must be valid");

        ResourceDerivation derivation =
                CanonicalDerivation.derive(name, EnumSet.allOf(Capability.class));
        assertEquals(vector.path("schemaName").asText(), derivation.schemaName());
        assertEquals(vector.path("collectionPath").asText(), derivation.collectionPath());
        assertEquals(vector.path("itemPath").asText(), derivation.itemPath());
        for (DerivedOperation operation : derivation.operations()) {
            String capability = operation.capability().name();
            assertEquals(vector.path("labels").path(capability).asText(), operation.label());
            assertEquals(vector.path("operationIds").path(capability).asText(),
                    operation.operationId());
        }
    }

    private static Stream<JsonNode> stream(JsonNode array) {
        return Stream.iterate(0, i -> i + 1).limit(array.size()).map(array::get);
    }

    private static JsonNode load() {
        try (var in = DerivationVectorsTest.class
                .getResourceAsStream("/derivation/derivation-vectors.json")) {
            return new ObjectMapper().readTree(in);
        } catch (Exception e) {
            throw new IllegalStateException("derivation-vectors.json must be on the classpath", e);
        }
    }
}
