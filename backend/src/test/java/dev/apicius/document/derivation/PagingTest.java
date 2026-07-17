package dev.apicius.document.derivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Pins the FEAT-010 paging table — the single source birth, toggle, and projection share. */
class PagingTest {

    // Paging belongs to list capabilities only: Browse, the standard five's only list.
    @Test
    void appliesToBrowse() {
        assertTrue(Paging.appliesTo(Capability.BROWSE));
    }

    @ParameterizedTest
    @EnumSource(value = Capability.class, names = "BROWSE", mode = EnumSource.Mode.EXCLUDE)
    void appliesToNoOtherCapability(Capability capability) {
        assertFalse(Paging.appliesTo(capability));
    }

    // The contract's numbers (FEAT-010): page ≥ 1 default 1; limit 1–100 default 20; the
    // pagination member's four whole-number fields.
    @Test
    void pinsTheContractsBoundsAndFields() {
        assertEquals(1, Paging.PAGE_MINIMUM);
        assertEquals(1, Paging.PAGE_DEFAULT);
        assertEquals(1, Paging.LIMIT_MINIMUM);
        assertEquals(100, Paging.LIMIT_MAXIMUM);
        assertEquals(20, Paging.LIMIT_DEFAULT);
        assertEquals(List.of("page", "limit", "totalItems", "totalPages"),
                Paging.PAGINATION_FIELDS);
    }
}
