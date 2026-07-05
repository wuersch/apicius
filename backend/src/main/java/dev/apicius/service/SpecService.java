package dev.apicius.service;

import dev.apicius.document.DocumentEngine;
import dev.apicius.document.SpecVersion;
import dev.apicius.domain.AppUser;
import dev.apicius.domain.Spec;
import dev.apicius.repository.LastEditedLocationRepository;
import dev.apicius.repository.SpecRepository;
import dev.apicius.repository.projection.LastEditedLocationProjection;
import dev.apicius.repository.projection.SpecSummaryProjection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

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

    /** Blank means "not provided": info.description is omitted, the projection column stays null (AC2). */
    private static String normalize(String description) {
        return description == null || description.isBlank() ? null : description;
    }
}
