package dev.apicius.document.derivation;

import java.util.Objects;

/**
 * One declaration edit, pre-derived and pre-validated, as it crosses the engine seam
 * (FEAT-011) — {@link FieldEdit}'s counterpart for query parameters and headers. The engine
 * writes it mechanically; name derivation, uniqueness, reservations, and the "one of" rules
 * are the caller's (ADR-0009). {@code required} is meaningful on inputs only — a response
 * header's is always false, enforced upstream by the wire shape.
 */
public record DeclarationEdit(String name, ParameterKind kind, boolean required,
        String description) {

    public DeclarationEdit {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
    }
}
