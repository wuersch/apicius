package dev.apicius.document.derivation;

/**
 * ADR-0011's core types — the working vocabulary of a field's kind. Each constant carries its
 * serialization, so the table's left column (designer language) and right column (derived
 * constructs) live in one place; {@link FieldKind} reads it forwards (derivation) and
 * backwards (recognition). Dates are formatted strings by house rule — the intrinsic format
 * belongs to the core type, not a refinement.
 */
public enum CoreType {
    TEXT("string", null),
    WHOLE_NUMBER("integer", null),
    DECIMAL_NUMBER("number", null),
    YES_NO("boolean", null),
    DATE("string", "date"),
    DATE_TIME("string", "date-time");

    private final String type;
    private final String intrinsicFormat;

    CoreType(String type, String intrinsicFormat) {
        this.type = type;
        this.intrinsicFormat = intrinsicFormat;
    }

    /** The serialized {@code type} construct. */
    public String type() {
        return type;
    }

    /** The serialized {@code format} the unrefined core type carries, or null for none. */
    public String intrinsicFormat() {
        return intrinsicFormat;
    }
}
