package dev.apicius.document.derivation;

import java.util.List;

/**
 * FEAT-010's paging contract as executable code — the single source shared by birth
 * derivation (list operations page from the start), the enable/disable mutations, and
 * structural detection (the contract projection's on/off state), so they can never disagree:
 * {@link StandardErrors}' pattern, applied to paging. Pure and deterministic; no engine types.
 */
public final class Paging {

    /** The page query parameter: whole number ≥ 1, default 1. */
    public static final String PAGE_PARAMETER = "page";

    /** The limit query parameter: whole number 1–100, default 20. */
    public static final String LIMIT_PARAMETER = "limit";

    /** The wrapper member carrying the paging answer. */
    public static final String PAGINATION_MEMBER = "pagination";

    public static final int PAGE_MINIMUM = 1;
    public static final int PAGE_DEFAULT = 1;
    public static final int LIMIT_MINIMUM = 1;
    public static final int LIMIT_MAXIMUM = 100;
    public static final int LIMIT_DEFAULT = 20;

    /** The pagination member's four required whole-number fields, in serialization order. */
    public static final List<String> PAGINATION_FIELDS =
            List.of("page", "limit", "totalItems", "totalPages");

    private Paging() {
    }

    /**
     * The applicability rule (FEAT-010): paging belongs to list-returning capabilities —
     * today exactly Browse, the standard five's only list.
     */
    public static boolean appliesTo(Capability capability) {
        return capability == Capability.BROWSE;
    }
}
