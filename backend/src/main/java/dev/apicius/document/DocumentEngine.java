package dev.apicius.document;

import dev.apicius.document.derivation.Capability;
import dev.apicius.document.derivation.FieldEdit;
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
     * FEAT-006: appends one property to the named resource's schema, serialized per ADR-0011
     * (with {@code required} membership per the edit), and returns the serialized result. A
     * pure transformation, like {@link #addResource}: the edit arrives pre-derived and
     * pre-validated — name uniqueness and the {@code id} guard are the caller's rules.
     * Touches nothing else in the document (AC1, AC3).
     */
    String addField(String body, String schemaName, FieldEdit field);

    /**
     * FEAT-006: rewrites the property currently named {@code propertyName} in place — same
     * position in the schema's {@code properties}, {@code required} membership following the
     * field (a rename included) — and touches nothing else (AC6).
     */
    String updateField(String body, String schemaName, String propertyName, FieldEdit field);

    /**
     * FEAT-006: removes the property and its {@code required} entry with no other change; a
     * shape reduced to {@code id} alone remains valid (AC8).
     */
    String removeField(String body, String schemaName, String propertyName);

    /**
     * FEAT-007: rewrites {@code info.title}, {@code info.version}, and {@code info.description}
     * ({@code null} removes the member) — the details save. Unmodeled {@code info} members
     * (contact, license, extensions) and every other document node are untouched (AC1).
     */
    String updateInfo(String body, String title, String description, String version);

    /**
     * FEAT-007: rewrites {@code info.title} only — the duplicate's single delta from the
     * original; {@code info.version} and everything else are preserved by construction (AC3).
     */
    String retitle(String body, String title);

    /**
     * FEAT-009: one capability's full contract, projected from the document (AC1–AC5) — the
     * identity (label preferring the operation's {@code summary}), the description, the
     * Request facet (Add: the shape's fields; Update: merge-patch semantics; absent
     * otherwise), the derived content-negotiation header line, and the Answers facet: the
     * success answer as the document declares it plus each applicable standard failure
     * answer's structural present/absent state. A pure read — never mutates (AC4). That the
     * resource and capability exist is the caller's verified rule.
     */
    CapabilityContractView capabilityContract(String body, String schemaName, Capability capability);

    /**
     * FEAT-009 UC3: rewrites the capability's operation so it answers its full standard set —
     * idempotently creating the shared {@code Error} schema and reusable responses on first
     * need (AC7) and referencing them at every applicable status, replacing whatever sat
     * there (FEAT-005's inline 404 included). Touches nothing else in the document (AC6).
     */
    String adoptStandardErrors(String body, String schemaName, Capability capability);

    /**
     * The concept projection of a stored document (FEAT-005 AC8): schema names, path keys, and
     * the recognized resources with their capabilities — "recognition is derivation inverted"
     * (ADR-0010), matching candidate segmentations of each schema name against the document's
     * actual paths. Heuristic, refuse-don't-mangle recognition of imports (PRIN-003) is a
     * future feature.
     */
    DocumentProjection project(String body);
}
