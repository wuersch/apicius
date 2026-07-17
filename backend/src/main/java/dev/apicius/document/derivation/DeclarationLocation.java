package dev.apicius.document.derivation;

/**
 * Where a declaration travels (FEAT-011) — a declaration belongs to exactly one capability
 * and one location, and the location decides its name derivation: query parameters share
 * FEAT-006's property-name rules, headers derive Hyphenated-Capitalized. Each constant
 * carries its Parameter Object {@code in} value where one exists, like {@link CoreType}
 * carries its serialization — response headers serialize as Header Objects, not parameters.
 */
public enum DeclarationLocation {
    QUERY_PARAMETER("query"),
    REQUEST_HEADER("header"),
    RESPONSE_HEADER(null);

    private final String parameterIn;

    DeclarationLocation(String parameterIn) {
        this.parameterIn = parameterIn;
    }

    /** The Parameter Object's {@code in} construct, or null — response headers aren't parameters. */
    public String parameterIn() {
        return parameterIn;
    }

    /** The location's derived name — the declaration's identity, same stance as fields. */
    public String deriveName(String rawName) {
        return this == QUERY_PARAMETER
                ? FieldNameDerivation.derive(rawName) : HeaderNameDerivation.derive(rawName);
    }
}
