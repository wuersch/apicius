package dev.apicius.document.derivation;

import java.util.Objects;
import java.util.Optional;

/**
 * ADR-0011's three-slot kind: a core type, an optional refinement, optionally wrapped as a
 * list. The slots are the durable contract — complex kinds later occupy the same core slot
 * without reshaping anything. Serialization and recognition both read the enum-carried table,
 * so "recognition is derivation inverted" is a structural fact, not a convention.
 */
public record FieldKind(CoreType core, Refinement refinement, boolean list) {

    public FieldKind {
        Objects.requireNonNull(core, "core");
        if (refinement != null && refinement.core() != core) {
            throw new IllegalArgumentException(refinement + " does not refine " + core);
        }
    }

    /** The serialized {@code type} of the scalar (the {@code items} type when {@code list}). */
    public String serializedType() {
        return core.type();
    }

    /**
     * The serialized {@code format} of the scalar, or null for none — the refinement's when
     * present, otherwise the core type's intrinsic one (Date, Date &amp; time).
     */
    public String serializedFormat() {
        return refinement != null ? refinement.format() : core.intrinsicFormat();
    }

    /**
     * The table read backwards: the kind a scalar {@code (type, format)} pair spells, or
     * empty when the pair is outside ADR-0011's table (unreachable for Apicius-authored
     * documents; import — FEAT-004 — owns displaying foreign pairs as-is).
     */
    public static Optional<FieldKind> recognizeScalar(String type, String format) {
        for (CoreType core : CoreType.values()) {
            if (core.type().equals(type) && Objects.equals(core.intrinsicFormat(), format)) {
                return Optional.of(new FieldKind(core, null, false));
            }
        }
        for (Refinement refinement : Refinement.values()) {
            if (refinement.core().type().equals(type) && refinement.format().equals(format)) {
                return Optional.of(new FieldKind(refinement.core(), refinement, false));
            }
        }
        return Optional.empty();
    }
}
