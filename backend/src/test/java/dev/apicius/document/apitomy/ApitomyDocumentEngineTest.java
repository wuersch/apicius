package dev.apicius.document.apitomy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.apicius.document.SpecVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Plain unit test (no Quarkus): the adapter is deliberately CDI-trivial. */
class ApitomyDocumentEngineTest {

    private final ApitomyDocumentEngine engine = new ApitomyDocumentEngine();
    private final ObjectMapper mapper = new ObjectMapper();

    // AC1: the seeded document carries the identity the dialog collected, and nothing else.
    @Test
    void seedsTitleAndAutoVersion() throws Exception {
        JsonNode document = parse(engine.createEmptyDocument(SpecVersion.V3_1, "Storefront API", null));

        assertEquals("3.1.1", document.path("openapi").asText());
        assertEquals("Storefront API", document.path("info").path("title").asText());
        assertEquals("1.0.0", document.path("info").path("version").asText());
    }

    // AC2: description present when provided…
    @Test
    void includesDescriptionWhenProvided() throws Exception {
        JsonNode document = parse(
                engine.createEmptyDocument(SpecVersion.V3_1, "Storefront API", "Sell products online."));

        assertEquals("Sell products online.", document.path("info").path("description").asText());
    }

    // …and absent (not null, not empty-string) when omitted.
    @Test
    void omitsDescriptionKeyWhenNull() throws Exception {
        JsonNode document = parse(engine.createEmptyDocument(SpecVersion.V3_1, "Storefront API", null));

        assertFalse(document.path("info").has("description"),
                "info.description must be absent, not null/empty (AC2)");
    }

    // AC3: the chosen minor pins the exact latest patch in the document.
    @ParameterizedTest
    @CsvSource({"V3_0, 3.0.4", "V3_1, 3.1.1", "V3_2, 3.2.0"})
    void pinsTheLatestPatchOfTheChosenMinor(SpecVersion version, String expected) throws Exception {
        JsonNode document = parse(engine.createEmptyDocument(version, "Fleet API", null));

        assertEquals(expected, document.path("openapi").asText());
    }

    // The container is empty: creation precedes all resource/capability authoring (FEAT-003).
    @Test
    void createsNoPathsOrComponents() throws Exception {
        JsonNode document = parse(engine.createEmptyDocument(SpecVersion.V3_1, "Storefront API", null));

        assertTrue(document.path("paths").isMissingNode() || document.path("paths").isEmpty());
        assertTrue(document.path("components").isMissingNode() || document.path("components").isEmpty());
    }

    private JsonNode parse(String body) throws Exception {
        return mapper.readTree(body);
    }
}
