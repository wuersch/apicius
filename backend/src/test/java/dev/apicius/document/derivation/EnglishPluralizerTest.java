package dev.apicius.document.derivation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Plain unit test: the pluralizer is pure policy (ADR-0010). */
class EnglishPluralizerTest {

    @ParameterizedTest
    @CsvSource({
            // default +s
            "product, products",
            "order, orders",
            "review, reviews",
            // s/x/z/ch/sh → es
            "status, statuses",
            "box, boxes",
            "quiz, quizes",
            "branch, branches",
            "dish, dishes",
            "address, addresses",
            // consonant+y → ies, vowel+y → +s
            "category, categories",
            "key, keys",
            "day, days",
            // irregulars
            "person, people",
            "child, children",
            "goose, geese",
            "tooth, teeth",
            // no f→ves rule, deliberately (wrong-but-consistent beats chef→cheves)
            "chef, chefs",
            "leaf, leafs",
    })
    void appliesTheRulesInOrder(String singular, String plural) {
        assertEquals(plural, EnglishPluralizer.pluralize(singular));
    }

    // An already-irregular-plural word returns itself: this is what makes Person and People
    // derive the same /people path, so the collision is caught instead of written.
    @ParameterizedTest
    @CsvSource({"people, people", "children, children", "geese, geese"})
    void irregularPluralsAreFixedPoints(String word, String expected) {
        assertEquals(expected, EnglishPluralizer.pluralize(word));
    }

    @Test
    void preservesTheFirstLetterCaseOfIrregulars() {
        assertEquals("People", EnglishPluralizer.pluralize("Person"));
    }

    @Test
    void preservesCasingOnSuffixRules() {
        assertEquals("Categories", EnglishPluralizer.pluralize("Category"));
        assertEquals("Statuses", EnglishPluralizer.pluralize("Status"));
    }
}
