package dev.apicius.document.derivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Plain unit test: ADR-0010's derivation table, beyond what the shared vectors cover (those
 * live in {@link DerivationVectorsTest}). The full name → naming/labels/operationIds matrix is
 * vector territory; this class pins behaviors the vectors can't express.
 */
class CanonicalDerivationTest {

    @Test
    void derivesOperationsInDialogRowOrderRegardlessOfInputOrder() {
        ResourceDerivation derivation = CanonicalDerivation.derive("Product",
                EnumSet.of(Capability.REMOVE, Capability.BROWSE, Capability.ADD));

        assertEquals(List.of(Capability.BROWSE, Capability.ADD, Capability.REMOVE),
                derivation.operations().stream().map(DerivedOperation::capability).toList());
    }

    @Test
    void aSubsetDerivesOnlyTheChosenOperations() {
        ResourceDerivation derivation =
                CanonicalDerivation.derive("Product", EnumSet.of(Capability.LOOK_UP));

        assertEquals(1, derivation.operations().size());
        DerivedOperation operation = derivation.operations().getFirst();
        assertEquals("GET", operation.method());
        assertEquals("/products/{id}", operation.path());
        assertEquals("getProduct", operation.operationId());
    }

    @Test
    void carriesTheCanonicalResponseDescriptions() {
        ResourceDerivation derivation =
                CanonicalDerivation.derive("Order item", EnumSet.allOf(Capability.class));

        assertEquals(List.of(
                "The list of order items.",
                "The order item.",
                "The created order item.",
                "The updated order item.",
                "The order item was removed."),
                derivation.operations().stream().map(DerivedOperation::successDescription).toList());
        assertEquals("No order item with this id exists.", derivation.notFoundDescription());
        assertEquals(List.of("200", "200", "201", "200", "204"),
                derivation.operations().stream().map(DerivedOperation::successStatus).toList());
    }

    // Recognition's most-likely inverse: schema name → words, for names this class produced.
    @ParameterizedTest
    @CsvSource({
            "Product, product",
            "OrderItem, order item",
            "APIKey, API key",
            "People, people",
            "Category5, category 5",
    })
    void wordsOfInvertsPascalCase(String schemaName, String expectedDisplay) {
        List<String> words = CanonicalDerivation.wordsOf(schemaName);
        ResourceDerivation reDerived =
                CanonicalDerivation.derive(String.join(" ", words), EnumSet.of(Capability.LOOK_UP));

        assertEquals(schemaName, reDerived.schemaName(), "wordsOf must round-trip the schema name");
        assertEquals("Look up one " + expectedDisplay, reDerived.operations().getFirst().label());
    }

    // PascalCase is lossy — recognition needs every plausible segmentation, checked against
    // the document's actual paths (the engine test proves the round-trip end-to-end).
    @Test
    void recognitionCandidatesCoverAmbiguousSegmentations() {
        assertEquals(List.of(List.of("Category", "5"), List.of("Category5")),
                CanonicalDerivation.recognitionCandidates("Category5"));
        assertTrue(CanonicalDerivation.recognitionCandidates("IPhone").contains(List.of("IPhone")),
                "the internal-capital noun (iPhone) needs the unsplit candidate");
        assertTrue(CanonicalDerivation.recognitionCandidates("AB").contains(List.of("A", "B")),
                "consecutive single-letter words need the fully split candidate");
    }
}
