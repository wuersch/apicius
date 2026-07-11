package dev.apicius.document.derivation;

/**
 * A field's visibility as one value (ADR-0011): auto and write-only at once would describe a
 * field nobody can ever see, so the invisible state is unrepresentable rather than validated
 * away (FEAT-006 AC10).
 */
public enum FieldVisibility {
    /** Serializes nothing. */
    NORMAL,
    /** "The server sets it; you never send it" — {@code readOnly: true}. */
    AUTO,
    /** "You send it; the server never returns it" — {@code writeOnly: true}. */
    WRITE_ONLY;

    /**
     * The house rule (ADR-0011, FEAT-006 AC5): <em>Text as password</em> defaults to
     * write-only — {@code format: password} alone is only a display hint. An explicit choice
     * (the override, or any deliberate pick) is honored as sent; null means "unspecified,
     * apply the default", so the rule holds for any client, not just our UI.
     */
    public static FieldVisibility resolve(FieldVisibility explicit, Refinement refinement) {
        if (explicit != null) {
            return explicit;
        }
        return refinement == Refinement.PASSWORD ? WRITE_ONLY : NORMAL;
    }
}
