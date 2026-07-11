package dev.apicius.document.derivation;

import java.util.Objects;

/**
 * One field edit, pre-derived and pre-validated, as it crosses the engine seam (FEAT-006) —
 * the counterpart of {@link ResourceDerivation} for shapes. The engine writes it mechanically;
 * name derivation, uniqueness, and the id guard are the caller's rules (ADR-0009).
 */
public record FieldEdit(String propertyName, FieldKind kind, boolean required,
        FieldVisibility visibility, String description) {

    public FieldEdit {
        Objects.requireNonNull(propertyName, "propertyName");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(visibility, "visibility");
    }
}
