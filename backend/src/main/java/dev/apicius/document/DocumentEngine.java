package dev.apicius.document;

/**
 * Apicius-owned seam over the OpenAPI document model/edit/validation engine (ADR-0009). The
 * concrete implementation ({@code apitomy-data-models}, isolated in {@code document.apitomy}) is
 * swappable; no library type crosses this boundary. FEAT-003 needs only creation — import
 * (FEAT-004: parse, validate) and the editor (commands, traversal-derived counts) extend this
 * interface when they land.
 */
public interface DocumentEngine {

    /**
     * A new, empty OpenAPI document — {@code openapi} = {@code version.latestPatch()},
     * {@code info.title}, {@code info.version} = {@code "1.0.0"}, and {@code info.description}
     * only when {@code description} is non-null (FEAT-003 AC1–AC3) — serialized, ready to persist
     * as {@code Spec.body}.
     */
    String createEmptyDocument(SpecVersion version, String title, String description);
}
