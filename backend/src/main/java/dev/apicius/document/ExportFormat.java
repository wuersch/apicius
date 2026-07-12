package dev.apicius.document;

/**
 * The two export serializations FEAT-008 offers. Constants are lowercase deliberately: JAX-RS
 * binds the query parameter via {@code valueOf} (so URLs read {@code ?format=yaml}), and
 * smallrye-openapi emits the constant names as the schema enum, which the generated client
 * turns into the {@code 'yaml' | 'json'} union.
 */
public enum ExportFormat {

    yaml("application/yaml"),
    json("application/json");

    private final String mediaType;

    ExportFormat(String mediaType) {
        this.mediaType = mediaType;
    }

    public String mediaType() {
        return mediaType;
    }

    /** The file extension — by construction the constant's own name. */
    public String extension() {
        return name();
    }
}
