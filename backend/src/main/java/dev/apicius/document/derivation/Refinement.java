package dev.apicius.document.derivation;

/**
 * ADR-0011's refinements — secondary intent narrowing a core type (PRIN-006: optional, never
 * required to proceed). Each constant is bound to the one core type it refines and the
 * {@code format} it serializes to; the binding is what makes an incompatible pair
 * ("Whole number as email") unconstructible in {@link FieldKind}.
 */
public enum Refinement {
    EMAIL(CoreType.TEXT, "email"),
    UUID(CoreType.TEXT, "uuid"),
    URL(CoreType.TEXT, "uri"),
    PASSWORD(CoreType.TEXT, "password"),
    INT32(CoreType.WHOLE_NUMBER, "int32"),
    INT64(CoreType.WHOLE_NUMBER, "int64"),
    FLOAT(CoreType.DECIMAL_NUMBER, "float"),
    DOUBLE(CoreType.DECIMAL_NUMBER, "double");

    private final CoreType core;
    private final String format;

    Refinement(CoreType core, String format) {
        this.core = core;
        this.format = format;
    }

    /** The one core type this refinement narrows. */
    public CoreType core() {
        return core;
    }

    /** The serialized {@code format} construct. */
    public String format() {
        return format;
    }
}
