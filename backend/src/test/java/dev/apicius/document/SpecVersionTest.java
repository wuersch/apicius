package dev.apicius.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SpecVersionTest {

    // FEAT-003 AC1/AC3: the frontend sends a minor; the persisted `openapi` is its latest patch.
    @ParameterizedTest
    @CsvSource({"3.0, 3.0.4", "3.1, 3.1.1", "3.2, 3.2.0"})
    void resolvesEachMinorToItsLatestPatch(String minor, String latestPatch) {
        assertEquals(latestPatch, SpecVersion.fromMinor(minor).latestPatch());
    }

    // AC1: leaving the picker untouched means 3.1 — absent input resolves to the default.
    @Test
    void defaultsToThreeDotOneWhenAbsent() {
        assertEquals(SpecVersion.V3_1, SpecVersion.fromMinor(null));
        assertEquals(SpecVersion.V3_1, SpecVersion.fromMinor(" "));
    }

    // Defensive backstop — the request DTO's @Pattern already rejects these at the API edge.
    @Test
    void rejectsUnsupportedMinors() {
        assertThrows(IllegalArgumentException.class, () -> SpecVersion.fromMinor("2.0"));
        assertThrows(IllegalArgumentException.class, () -> SpecVersion.fromMinor("3.0.4"));
    }
}
