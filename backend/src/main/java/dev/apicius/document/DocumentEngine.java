package dev.apicius.document;

import dev.apicius.document.derivation.ResourceDerivation;

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

    /**
     * FEAT-005: derives a resource into the document per ADR-0010 — the schema (required,
     * read-only {@code id: string}, {@code description} only when non-null) plus the
     * collection/item paths carrying exactly the chosen operations — and returns the serialized
     * result. A pure transformation: the derivation arrives pre-computed
     * ({@code CanonicalDerivation}) so the writer and the uniqueness check share one source,
     * and uniqueness itself is the caller's rule. Touches nothing else in the document (AC3).
     */
    String addResource(String body, ResourceDerivation derivation, String description);

    /**
     * The concept projection of a stored document (FEAT-005 AC8): schema names, path keys, and
     * the recognized resources with their capabilities — "recognition is derivation inverted"
     * (ADR-0010), matching candidate segmentations of each schema name against the document's
     * actual paths. Heuristic, refuse-don't-mangle recognition of imports (PRIN-003) is a
     * future feature.
     */
    DocumentProjection project(String body);
}
