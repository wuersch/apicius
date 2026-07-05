package dev.apicius.document;

import java.util.Arrays;

/**
 * The OpenAPI spec versions Apicius targets, and the minor→latest-patch policy (FEAT-003): clients
 * choose a minor; the persisted {@code openapi} string is its latest patch. This enum is the one
 * place that knows the mapping — creation uses it today; import (FEAT-004) and the editor resolve
 * minors through it later.
 */
public enum SpecVersion {
    V3_0("3.0", "3.0.4"),
    V3_1("3.1", "3.1.1"),
    V3_2("3.2", "3.2.0");

    /** The safe modern default (FEAT-003 AC1) — what an untouched picker means. */
    public static final SpecVersion DEFAULT = V3_1;

    private final String minor;
    private final String latestPatch;

    SpecVersion(String minor, String latestPatch) {
        this.minor = minor;
        this.latestPatch = latestPatch;
    }

    /** Resolves a wire-format minor ({@code "3.1"}); absent means {@link #DEFAULT}. */
    public static SpecVersion fromMinor(String minor) {
        if (minor == null || minor.isBlank()) {
            return DEFAULT;
        }
        return Arrays.stream(values())
                .filter(version -> version.minor.equals(minor))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported spec version: " + minor));
    }

    public String minor() {
        return minor;
    }

    public String latestPatch() {
        return latestPatch;
    }
}
