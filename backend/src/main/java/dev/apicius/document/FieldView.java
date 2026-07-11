package dev.apicius.document;

import dev.apicius.document.derivation.FieldKind;
import dev.apicius.document.derivation.FieldVisibility;

/**
 * A field as the concept projection sees it (FEAT-006 AC11): the property name — the field's
 * identity and display (ADR-0011) — its plain-language kind, and its attributes. {@code id}
 * projects as an ordinary field (Text, auto, required); it is special only where mutation
 * rules and the UI lock it, not in the model.
 */
public record FieldView(String name, FieldKind kind, boolean required,
        FieldVisibility visibility, String description) {
}
