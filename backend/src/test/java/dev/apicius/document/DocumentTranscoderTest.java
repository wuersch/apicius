package dev.apicius.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * FEAT-008 AC1, transcoder half: the YAML carries every node in authored property order, with
 * scalars exactly as stored — no re-typing, no re-formatting of literals.
 */
class DocumentTranscoderTest {

    private final DocumentTranscoder transcoder = new DocumentTranscoder();

    /** Reads YAML with the same exactness the transcoder must write with. */
    private final ObjectMapper yaml = YAMLMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
            .build();

    // AC1: authored property order survives — including integer-like keys ("404" before
    // "200"), which a naive parse through ordinary JS/Java maps would reorder.
    @Test
    void preservesAuthoredPropertyOrder() throws Exception {
        String body = "{\"paths\":{\"/b\":{},\"/a\":{}},"
                + "\"responses\":{\"404\":{\"z\":1,\"a\":2},\"200\":{}}}";

        JsonNode roundTripped = yaml.readTree(transcoder.toYaml(body));

        assertEquals(List.of("/b", "/a"), fieldNames(roundTripped.path("paths")));
        assertEquals(List.of("404", "200"), fieldNames(roundTripped.path("responses")));
        assertEquals(List.of("z", "a"), fieldNames(roundTripped.path("responses").path("404")));
    }

    // AC1: numeric literals survive exactly — trailing zeros, 64-bit integers, and precision
    // beyond double must not be rewritten by a lossy double round-trip.
    @Test
    void preservesNumericLiteralsExactly() {
        String body = "{\"price\":1.10,\"big\":9007199254740993,"
                + "\"precise\":0.12345678901234567890123456789}";

        String yamlText = transcoder.toYaml(body);

        assertTrue(yamlText.contains("1.10"), "trailing zero must survive: " + yamlText);
        assertTrue(yamlText.contains("9007199254740993"), "64-bit int must survive: " + yamlText);
        assertTrue(yamlText.contains("0.12345678901234567890123456789"),
                "precision beyond double must survive: " + yamlText);
    }

    // AC1: strings a YAML 1.1 or 1.2 parser would re-type when unquoted ("1.0" → float,
    // "on"/"yes"/"no" → bool, "null" → null) must stay strings — quoting is the guard.
    @Test
    void keepsAmbiguousStringsAsStrings() throws Exception {
        String body = "{\"version\":\"1.0\",\"a\":\"on\",\"b\":\"yes\",\"c\":\"no\",\"d\":\"null\"}";

        JsonNode roundTripped = yaml.readTree(transcoder.toYaml(body));

        for (String field : List.of("version", "a", "b", "c", "d")) {
            assertTrue(roundTripped.path(field).isTextual(),
                    "'" + field + "' must still be a string");
        }
        assertEquals("1.0", roundTripped.path("version").asText());
    }

    @Test
    void carriesUnicodeThrough() throws Exception {
        String body = "{\"description\":\"Café orders ✨ — für alle\"}";

        JsonNode roundTripped = yaml.readTree(transcoder.toYaml(body));

        assertEquals("Café orders ✨ — für alle", roundTripped.path("description").asText());
    }

    // A downloaded document is a standalone file — no stream framing ("---") in front.
    @Test
    void emitsNoDocumentStartMarker() throws Exception {
        String yamlText = transcoder.toYaml("{\"openapi\":\"3.1.1\"}");

        assertFalse(yamlText.startsWith("---"), "no document start marker: " + yamlText);
        assertTrue(yaml.readTree(yamlText).isObject());
    }

    @Test
    void transcodesTheEmptyDocument() throws Exception {
        JsonNode roundTripped = yaml.readTree(transcoder.toYaml("{}"));

        assertTrue(roundTripped.isObject() && roundTripped.isEmpty());
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
