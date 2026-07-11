package dev.apicius.service;

import dev.apicius.document.CapabilityView;
import dev.apicius.document.DocumentEngine;
import dev.apicius.document.DocumentProjection;
import dev.apicius.document.FieldView;
import dev.apicius.document.ResourceView;
import dev.apicius.document.SpecVersion;
import dev.apicius.document.derivation.CanonicalDerivation;
import dev.apicius.document.derivation.Capability;
import dev.apicius.document.derivation.CoreType;
import dev.apicius.document.derivation.FieldEdit;
import dev.apicius.document.derivation.FieldKind;
import dev.apicius.document.derivation.FieldNameDerivation;
import dev.apicius.document.derivation.FieldVisibility;
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
        Spec spec = lockedSpec(specId);
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
     * FEAT-006: adds a field to a resource's shape — one atomic document mutation through the
     * engine seam. One transaction covers the mutated document and the editor's jump-back-in
     * pointer (AC2); the ADR-0008 counts are untouched — a shape edit changes the schema and
     * nothing else. Any thrown rejection rolls everything back (AC9).
     */
    @Transactional
    public FieldView addField(AppUser editor, UUID specId, String schemaName, FieldDraft draft) {
        Spec spec = lockedSpec(specId);
        ResourceView resource = resourceOf(spec, schemaName);
        FieldEdit edit = derivedEdit(draft);
        rejectFieldConflicts(resource, null, edit.propertyName(), draft.name());

        spec.body = documentEngine.addField(spec.body, schemaName, edit);
        lastEditedLocationRepository.upsertForUser(editor.id, spec.id, null);
        return fieldView(edit);
    }

    /**
     * FEAT-006 UC3: rewrites a field in place — rename, retype, attributes, description as
     * one atomic save; {@code required} membership follows the field (AC6). The identity
     * field is exempt (AC7): {@code id} short-circuits before the engine is ever called.
     */
    @Transactional
    public FieldView updateField(AppUser editor, UUID specId, String schemaName,
            String propertyName, FieldDraft draft) {
        Spec spec = lockedSpec(specId);
        ResourceView resource = resourceOf(spec, schemaName);
        rejectIdentityField(propertyName);
        rejectUnknownField(resource, propertyName);
        FieldEdit edit = derivedEdit(draft);
        rejectFieldConflicts(resource, propertyName, edit.propertyName(), draft.name());

        spec.body = documentEngine.updateField(spec.body, schemaName, propertyName, edit);
        lastEditedLocationRepository.upsertForUser(editor.id, spec.id, null);
        return fieldView(edit);
    }

    /**
     * FEAT-006 UC4: removes a field — the property and its {@code required} entry, no other
     * trace (AC8); removing the last field beyond {@code id} is fine. {@code id} is exempt
     * (AC7).
     */
    @Transactional
    public void removeField(AppUser editor, UUID specId, String schemaName, String propertyName) {
        Spec spec = lockedSpec(specId);
        ResourceView resource = resourceOf(spec, schemaName);
        rejectIdentityField(propertyName);
        rejectUnknownField(resource, propertyName);

        spec.body = documentEngine.removeField(spec.body, schemaName, propertyName);
        lastEditedLocationRepository.upsertForUser(editor.id, spec.id, null);
    }

    /**
     * Document mutations serialize per spec (unlike creates, which never contend): under the
     * row lock the uniqueness checks are race-free — a concurrent same-name add gets a
     * deterministic 409 instead of an optimistic-lock 500.
     */
    private Spec lockedSpec(UUID specId) {
        Spec spec = specRepository.findById(specId, LockModeType.PESSIMISTIC_WRITE);
        if (spec == null) {
            throw new SpecNotFoundException(specId);
        }
        return spec;
    }

    /** The addressed resource, from a fresh projection — its fields back the conflict check. */
    private ResourceView resourceOf(Spec spec, String schemaName) {
        return documentEngine.project(spec.body).resources().stream()
                .filter(resource -> resource.name().equals(schemaName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(schemaName));
    }

    /**
     * The pre-derived, pre-validated edit the engine trusts: kind compatibility, the AC9
     * empty-derivation rule, and the AC5 visibility default all resolve here, before any
     * document is touched.
     */
    private static FieldEdit derivedEdit(FieldDraft draft) {
        if (draft.refinement() != null && draft.refinement().core() != draft.coreType()) {
            throw new InvalidFieldKindException(draft.coreType(), draft.refinement());
        }
        String propertyName = FieldNameDerivation.derive(draft.name());
        if (propertyName.isEmpty()) {
            throw new UnderivableNameException("'" + draft.name()
                    + "' derives to an empty property name — use letters or digits.");
        }
        return new FieldEdit(propertyName,
                new FieldKind(draft.coreType(), draft.refinement(), draft.list()),
                draft.required(), FieldVisibility.resolve(draft.visibility(), draft.refinement()),
                normalize(draft.description()));
    }

    private static void rejectIdentityField(String propertyName) {
        if (propertyName.equals("id")) {
            throw new FieldNotEditableException();
        }
    }

    private static void rejectUnknownField(ResourceView resource, String propertyName) {
        if (resource.fields().stream().noneMatch(field -> field.name().equals(propertyName))) {
            throw new FieldNotFoundException(resource.name(), propertyName);
        }
    }

    /**
     * AC9 — uniqueness case-insensitively on the derived property name, against every field
     * of this shape including {@code id}; on an update the field itself is exempt, so a
     * case-only rename stays legal.
     */
    private static void rejectFieldConflicts(ResourceView resource, String currentName,
            String propertyName, String rawName) {
        boolean collides = resource.fields().stream()
                .map(FieldView::name)
                .filter(existing -> !existing.equals(currentName))
                .anyMatch(existing -> existing.equalsIgnoreCase(propertyName));
        if (collides) {
            throw new NameConflictException("'" + rawName + "' derives to '" + propertyName
                    + "', which this shape already uses — id counts too.");
        }
    }

    /** What the client sees is the derivation, same as a re-read would project (AC1). */
    private static FieldView fieldView(FieldEdit edit) {
        return new FieldView(edit.propertyName(), edit.kind(), edit.required(),
                edit.visibility(), edit.description());
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
        // The one field creation writes: the identity house rule, as a re-read projects it.
        List<FieldView> fields = List.of(new FieldView("id",
                new FieldKind(CoreType.TEXT, null, false), true, FieldVisibility.AUTO, null));
        return new ResourceView(derivation.schemaName(), description, capabilities, fields);
    }

    /** Blank means "not provided": info.description is omitted, the projection column stays null (AC2). */
    private static String normalize(String description) {
        return description == null || description.isBlank() ? null : description;
    }
}
