package dev.apicius.document.derivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * The Java half of the shared-vector contract for FEAT-011: every vector in
 * {@code declaration-vectors.json} asserted against {@link HeaderNameDerivation},
 * {@link Declarations}, and {@link ParameterKind.OneOf}. The TS half is
 * {@code frontend/src/lib/headerDerivation.test.ts}, reading the same file.
 */
class DeclarationVectorsTest {

    private static final JsonNode VECTORS = load();

    @TestFactory
    Stream<DynamicTest> headerNamesDeriveExactlyTheVector() {
        return stream(VECTORS.path("headerNames").path("valid"))
                .map(vector -> DynamicTest.dynamicTest("\"" + vector.path("name").asText() + "\"",
                        () -> assertEquals(vector.path("headerName").asText(),
                                HeaderNameDerivation.derive(vector.path("name").asText()))));
    }

    @TestFactory
    Stream<DynamicTest> underivableHeaderNamesDeriveToEmpty() {
        return stream(VECTORS.path("headerNames").path("deriveToEmpty"))
                .map(name -> DynamicTest.dynamicTest("\"" + name.asText() + "\"",
                        () -> assertEquals("", HeaderNameDerivation.derive(name.asText()))));
    }

    @TestFactory
    Stream<DynamicTest> reservedHeaderNamesAreReservedCaseInsensitively() {
        return Stream.concat(
                stream(VECTORS.path("reservedHeaderNames").path("reserved"))
                        .map(name -> DynamicTest.dynamicTest("reserved \"" + name.asText() + "\"",
                                () -> assertEquals(true,
                                        Declarations.isReservedHeaderName(name.asText())))),
                stream(VECTORS.path("reservedHeaderNames").path("free"))
                        .map(name -> DynamicTest.dynamicTest("free \"" + name.asText() + "\"",
                                () -> assertEquals(false,
                                        Declarations.isReservedHeaderName(name.asText())))));
    }

    // The serialized schema itself is the engine's to write (asserted in its test); here the
    // vector pins normalization: the constructed value list is the schema's enum, in order.
    @TestFactory
    Stream<DynamicTest> oneOfValuesNormalizeToTheSchemaEnum() {
        return stream(VECTORS.path("oneOf").path("serialization"))
                .map(vector -> DynamicTest.dynamicTest(vector.path("values").toString(), () -> {
                    ParameterKind.OneOf kind =
                            new ParameterKind.OneOf(textsOf(vector.path("values")));
                    assertEquals(textsOf(vector.path("schema").path("enum")), kind.values());
                    assertEquals("string", vector.path("schema").path("type").asText());
                }));
    }

    @TestFactory
    Stream<DynamicTest> invalidOneOfValuesAreUnconstructible() {
        return stream(VECTORS.path("oneOf").path("invalid"))
                .map(vector -> DynamicTest.dynamicTest(vector.path("why").asText(),
                        () -> assertThrows(IllegalArgumentException.class,
                                () -> new ParameterKind.OneOf(textsOf(vector.path("values"))))));
    }

    private static List<String> textsOf(JsonNode array) {
        List<String> texts = new ArrayList<>();
        array.forEach(node -> texts.add(node.asText()));
        return texts;
    }

    private static Stream<JsonNode> stream(JsonNode array) {
        return Stream.iterate(0, i -> i + 1).limit(array.size()).map(array::get);
    }

    private static JsonNode load() {
        try (var in = DeclarationVectorsTest.class
                .getResourceAsStream("/derivation/declaration-vectors.json")) {
            return new ObjectMapper().readTree(in);
        } catch (Exception e) {
            throw new IllegalStateException("declaration-vectors.json must be on the classpath", e);
        }
    }
}
