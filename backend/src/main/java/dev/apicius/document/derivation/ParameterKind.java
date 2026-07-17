package dev.apicius.document.derivation;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A declaration's kind (FEAT-011): FEAT-006's plain-language vocabulary, or "one of …" — a
 * fixed set of distinct text values. A sibling sum type, not a fork of the kind enums:
 * "one of" needs a value list no {@code (type, format)} pair can carry, so it stands beside
 * {@link FieldKind} rather than bending it. Serialization is derived, never typed: a scalar
 * kind reads ADR-0011's table, "one of" serializes as {@code type: string} plus an inline
 * {@code enum} (allowed here — a parameter's value set is not a shape, FEAT-011 § Why).
 */
public sealed interface ParameterKind {

    /** FEAT-006's vocabulary, reused whole — minus the list wrapper (a FEAT-011 Non-Goal). */
    record Scalar(FieldKind kind) implements ParameterKind {

        public Scalar {
            Objects.requireNonNull(kind, "kind");
            if (kind.list()) {
                throw new IllegalArgumentException("a parameter is never a list");
            }
        }
    }

    /**
     * A fixed value set: text-only, at least one, non-blank, distinct (case-sensitively —
     * the values are payload, not names). The constructor is the one normalizer and
     * validator (AC7): values are trimmed, and a violation throws — the service translates
     * to its rejection type, so the rules live here once.
     */
    record OneOf(List<String> values) implements ParameterKind {

        public OneOf {
            values = values.stream().map(String::trim).toList();
            if (values.isEmpty()) {
                throw new IllegalArgumentException("\"one of\" needs at least one value");
            }
            if (values.stream().anyMatch(String::isEmpty)) {
                throw new IllegalArgumentException("\"one of\" values must be non-blank");
            }
            if (Set.copyOf(values).size() != values.size()) {
                throw new IllegalArgumentException("\"one of\" values must be distinct");
            }
        }
    }
}
