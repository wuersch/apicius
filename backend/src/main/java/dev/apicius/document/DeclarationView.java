package dev.apicius.document;

import dev.apicius.document.derivation.ParameterKind;

/**
 * One designer-authored declaration as projected (FEAT-011) — {@code name} is the derived
 * identity (property-style for query parameters, Hyphenated-Capitalized for headers).
 * {@code required} is always false on a response header (optionality is inputs-only).
 */
public record DeclarationView(String name, ParameterKind kind, boolean required,
        String description) {
}
