package dev.apicius.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * FEAT-008: transcodes the stored document body (the ADR-0009 engine's JSON serialization,
 * ADR-0004) into YAML. Deliberately beside — not behind — the {@link DocumentEngine} seam: the
 * engine has no YAML writer, and this is pure format transcoding of its output, not document
 * semantics; no engine implementation should have to carry it.
 */
@ApplicationScoped
public class DocumentTranscoder {

    /**
     * Reads with exact literals: node objects keep authored key order (LinkedHashMap-backed),
     * and big-decimal/big-integer reading keeps numbers as written — a double round-trip
     * would rewrite {@code 1.10} or 64-bit integers.
     */
    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
            // ...and node-building must not undo that exactness (1.10 → 1.1 by default).
            .disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
            .build();

    /**
     * {@code MINIMIZE_QUOTES} stays off: quoting every string keeps {@code "1.0"},
     * {@code "on"}, {@code "no"}, {@code "null"} strings under YAML 1.1 and 1.2 parsers
     * alike. No document start marker — the export is a standalone file, not a stream.
     */
    private static final ObjectMapper YAML = YAMLMapper.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .build();

    /** The body as YAML — every node, authored property order, exact scalars (FEAT-008 AC1). */
    public String toYaml(String jsonBody) {
        try {
            return YAML.writeValueAsString(JSON.readTree(jsonBody));
        } catch (JsonProcessingException e) {
            // Unreachable for persisted documents: every body is the engine's serialization.
            throw new IllegalStateException("stored document is not valid JSON", e);
        }
    }
}
