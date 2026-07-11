package dev.apicius.document.derivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * The Java half of the shared-vector contract for FEAT-006 (ADR-0011): every vector in
 * {@code field-vectors.json} asserted against {@link FieldNameDerivation}, {@link FieldKind},
 * and {@link FieldVisibility}. The TS half is {@code frontend/src/lib/fieldDerivation.test.ts},
 * reading the same file.
 */
class FieldVectorsTest {

    private static final JsonNode VECTORS = load();

    @TestFactory
    Stream<DynamicTest> namesDeriveExactlyTheVector() {
        return stream(VECTORS.path("propertyNames").path("valid"))
                .map(vector -> DynamicTest.dynamicTest("\"" + vector.path("name").asText() + "\"",
                        () -> assertEquals(vector.path("propertyName").asText(),
                                FieldNameDerivation.derive(vector.path("name").asText()))));
    }

    @TestFactory
    Stream<DynamicTest> underivableNamesDeriveToEmpty() {
        return stream(VECTORS.path("propertyNames").path("deriveToEmpty"))
                .map(name -> DynamicTest.dynamicTest("\"" + name.asText() + "\"",
                        () -> assertEquals("", FieldNameDerivation.derive(name.asText()))));
    }

    @TestFactory
    Stream<DynamicTest> kindsSerializePerTheTableAndRecognizeBack() {
        return stream(VECTORS.path("kinds")).map(vector -> DynamicTest.dynamicTest(
                kindLabel(vector), () -> {
                    FieldKind kind = kindOf(vector);
                    assertEquals(vector.path("type").asText(), kind.serializedType());
                    assertEquals(textOrNull(vector.path("format")), kind.serializedFormat());
                    // Recognition is derivation inverted: the scalar half round-trips exactly.
                    assertEquals(Optional.of(new FieldKind(kind.core(), kind.refinement(), false)),
                            FieldKind.recognizeScalar(kind.serializedType(), kind.serializedFormat()));
                }));
    }

    @TestFactory
    Stream<DynamicTest> incompatibleRefinementsAreUnconstructible() {
        return stream(VECTORS.path("incompatible")).map(vector -> DynamicTest.dynamicTest(
                kindLabel(vector),
                () -> assertThrows(IllegalArgumentException.class, () -> kindOf(vector))));
    }

    @TestFactory
    Stream<DynamicTest> unspecifiedVisibilityResolvesToTheHouseRuleDefault() {
        return stream(VECTORS.path("visibilityDefaults")).map(vector -> DynamicTest.dynamicTest(
                textOrNull(vector.path("refinement")) + " → " + vector.path("default").asText(),
                () -> assertEquals(FieldVisibility.valueOf(vector.path("default").asText()),
                        FieldVisibility.resolve(null, refinementOf(vector)))));
    }

    private static FieldKind kindOf(JsonNode vector) {
        return new FieldKind(CoreType.valueOf(vector.path("coreType").asText()),
                refinementOf(vector), vector.path("list").asBoolean());
    }

    private static Refinement refinementOf(JsonNode vector) {
        String refinement = textOrNull(vector.path("refinement"));
        return refinement == null ? null : Refinement.valueOf(refinement);
    }

    private static String kindLabel(JsonNode vector) {
        return vector.path("coreType").asText() + " as " + textOrNull(vector.path("refinement"))
                + (vector.path("list").asBoolean() ? ", list" : "");
    }

    private static String textOrNull(JsonNode node) {
        return node.isNull() || node.isMissingNode() ? null : node.asText();
    }

    private static Stream<JsonNode> stream(JsonNode array) {
        return Stream.iterate(0, i -> i + 1).limit(array.size()).map(array::get);
    }

    private static JsonNode load() {
        try (var in = FieldVectorsTest.class
                .getResourceAsStream("/derivation/field-vectors.json")) {
            return new ObjectMapper().readTree(in);
        } catch (Exception e) {
            throw new IllegalStateException("field-vectors.json must be on the classpath", e);
        }
    }
}
