package dev.apicius.service;

import dev.apicius.document.derivation.CoreType;
import dev.apicius.document.derivation.FieldVisibility;
import dev.apicius.document.derivation.Refinement;

/**
 * One field edit as the designer expressed it (FEAT-006) — freeform name, plain-language
 * kind, attributes — before any derivation or validation. The service turns it into a
 * {@link dev.apicius.document.derivation.FieldEdit}; bundling the tuple keeps the same seven
 * values from being threaded positionally through every layer. {@code visibility} null means
 * "unspecified, apply the house-rule default" (AC5).
 */
public record FieldDraft(String name, CoreType coreType, Refinement refinement, boolean list,
        boolean required, FieldVisibility visibility, String description) {
}
