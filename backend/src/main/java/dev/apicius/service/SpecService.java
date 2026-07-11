package dev.apicius.service;

import dev.apicius.document.CapabilityView;
import dev.apicius.document.DocumentEngine;
import dev.apicius.document.DocumentProjection;
import dev.apicius.document.ResourceView;
import dev.apicius.document.SpecVersion;
import dev.apicius.document.derivation.CanonicalDerivation;
import dev.apicius.document.derivation.Capability;
import dev.apicius.document.derivation.ResourceDerivation;
import dev.apicius.domain.AppUser;
import dev.apicius.domain.Spec;
import dev.apicius.repository.LastEditedLocationRepository;
import dev.apicius.repository.SpecRepository;
import dev.apicius.repository.projection.LastEditedLocationProjection;
import dev.apicius.repository.projection.SpecSummaryProjection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SpecService {

    @Inject
    SpecRepository specRepository;

    @Inject
    LastEditedLocationRepository lastEditedLocationRepository;

    @Inject
    DocumentEngine documentEngine;

    /**
     * All APIs as summary projections, alphabetical by title (FEAT-002 AC2). The list is
     * workspace-global by design — everyone sees every API; owner is provenance, not a filter.
     */
    @Transactional
    public List<SpecSummaryProjection> listSummaries() {
        return specRepository.listSummariesOrderedByTitle();
    }

    /** The caller's single jump-back-in pointer, if the (future) editor has recorded one (AC1). */
    @Transactional
    public Optional<LastEditedLocationProjection> lastEditedFor(AppUser user) {
        return lastEditedLocationRepository.findByUserId(user.id);
    }

    /**
     * FEAT-003: creates a new, empty API — the write chokepoint's first writer (ADR-0008). One
     * transaction covers the seeded document, its projection columns (counts are 0 by
     * construction — nothing is authored yet), and the creator's jump-back-in pointer (creating
     * is editing: the designer's next session should resume here).
     */
    @Transactional
    public Spec createEmpty(AppUser owner, String title, String description, String specVersionMinor) {
        SpecVersion version = SpecVersion.fromMinor(specVersionMinor);
        String normalizedDescription = normalize(description);

        Spec spec = new Spec();
        spec.owner = owner;
        spec.title = title;
        spec.description = normalizedDescription;
        spec.apiVersion = "1.0.0"; // seeded info.version (FEAT-003) — not the OpenAPI spec version
        spec.resourceCount = 0;
        spec.operationCount = 0;
        spec.body = documentEngine.createEmptyDocument(version, title, normalizedDescription);
        specRepository.persist(spec);
        // The upsert below is native SQL referencing the new row's FK — flush the insert first.
        specRepository.flush();

        // Creating is editing: the pointer moves to the new API, at API level (no capability yet).
        lastEditedLocationRepository.upsertForUser(owner.id, spec.id, null);
        return spec;
    }

    /**
     * FEAT-005: one API for the editor — the spec row plus its recognized resources (AC8).
     * This read path deliberately hydrates {@code body}: it is the editor's document read, not
     * a list projection (FEAT-002 AC5 scopes only the home list). Opening an API is not
     * editing, so the jump-back-in pointer does not move.
     */
    @Transactional
    public SpecDetail detail(UUID specId) {
        Spec spec = specRepository.findById(specId);
        if (spec == null) {
            throw new SpecNotFoundException(specId);
        }
        return new SpecDetail(spec, documentEngine.project(spec.body).resources());
    }

    /**
     * FEAT-005: the document's first content mutation — derives the resource's schema and
     * paths (ADR-0010) into {@code body}. One transaction covers the mutated document, the
     * ADR-0008 projection deltas, and the editor's jump-back-in pointer (AC2); any thrown
     * rejection rolls all of it back, so nothing is ever partially persisted (AC5–AC7).
     */
    @Transactional
    public ResourceView addResource(AppUser editor, UUID specId, String name, String description,
            List<Capability> capabilities) {
        // Document mutations serialize per spec (unlike creates, which never contend): under
        // the row lock the uniqueness check is race-free — a concurrent same-name add gets a
        // deterministic 409 instead of an optimistic-lock 500.
        Spec spec = specRepository.findById(specId, LockModeType.PESSIMISTIC_WRITE);
        if (spec == null) {
            throw new SpecNotFoundException(specId);
        }
        ResourceDerivation derivation =
                CanonicalDerivation.derive(name.trim(), EnumSet.copyOf(capabilities));
        rejectConflicts(spec, derivation, name.trim());

        String normalizedDescription = normalize(description);
        spec.body = documentEngine.addResource(spec.body, derivation, normalizedDescription);
        spec.resourceCount += 1;
        spec.operationCount += derivation.operations().size();

        lastEditedLocationRepository.upsertForUser(editor.id, spec.id, null);
        return view(derivation, normalizedDescription);
    }

    /**
     * AC6 — uniqueness on the <em>derived</em> footprint, not the raw string: a name whose
     * schema name clashes case-insensitively ("product" vs Product), or whose paths collide
     * (Person and People both derive /people), conflicts even though the strings differ.
     * Comparing against every schema covers datatypes too, once those exist.
     */
    private void rejectConflicts(Spec spec, ResourceDerivation derivation, String name) {
        DocumentProjection projection = documentEngine.project(spec.body);
        if (projection.schemaNames().stream()
                .anyMatch(existing -> existing.equalsIgnoreCase(derivation.schemaName()))) {
            throw new NameConflictException(
                    "'" + name + "' is already used by a resource or datatype in this API.");
        }
        if (projection.paths().contains(derivation.collectionPath())
                || projection.paths().contains(derivation.itemPath())) {
            throw new NameConflictException("'" + name + "' would use "
                    + derivation.collectionPath() + ", which this API already uses.");
        }
    }

    /** What the client sees is the derivation, same as a re-read would project (AC1). */
    private static ResourceView view(ResourceDerivation derivation, String description) {
        List<CapabilityView> capabilities = derivation.operations().stream()
                .map(operation -> new CapabilityView(operation.capability(), operation.label(),
                        operation.method(), operation.path()))
                .toList();
        return new ResourceView(derivation.schemaName(), description, capabilities);
    }

    /** Blank means "not provided": info.description is omitted, the projection column stays null (AC2). */
    private static String normalize(String description) {
        return description == null || description.isBlank() ? null : description;
    }
}
