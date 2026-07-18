package dev.apicius.document.derivation;

import java.util.List;

/**
 * FEAT-011's reserved header names as executable code — the single source shared by the
 * service's reservation guard and the frontend's inline pre-check (via the shared vectors),
 * so they can never disagree: {@link Paging}'s pattern, applied to declarations. Reserved in
 * both header locations: {@code Accept} and {@code Content-Type} are owned by content
 * negotiation (the derived line, FEAT-009), {@code Authorization} by the future security
 * feature — auth is OpenAPI {@code security}, never a parameter. The {@code page}/{@code
 * limit} reservation is not here: it is document-state-dependent (a paged capability only)
 * and lives with {@link Paging}, its owner.
 */
public final class Declarations {

    /** The reserved header names, in display casing. */
    public static final List<String> RESERVED_HEADER_NAMES =
            List.of("Accept", "Content-Type", "Authorization");

    private Declarations() {
    }

    /** The reservation, case-insensitive like every other name rule. */
    public static boolean isReservedHeaderName(String headerName) {
        return RESERVED_HEADER_NAMES.stream().anyMatch(name -> name.equalsIgnoreCase(headerName));
    }
}
