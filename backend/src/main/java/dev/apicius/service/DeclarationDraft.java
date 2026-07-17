package dev.apicius.service;

import dev.apicius.document.derivation.CoreType;
import dev.apicius.document.derivation.Refinement;
import java.util.List;

/**
 * One declaration edit as the designer expressed it (FEAT-011) — freeform name,
 * plain-language kind, attributes — before any derivation or validation, the
 * {@link FieldDraft} counterpart. The kind is either {@code coreType} (+ optional
 * {@code refinement}) or {@code oneOfValues}, exactly one; {@code required} is always false
 * for a response header (its wire shape has no such field).
 */
public record DeclarationDraft(String name, CoreType coreType, Refinement refinement,
        List<String> oneOfValues, boolean required, String description) {
}
