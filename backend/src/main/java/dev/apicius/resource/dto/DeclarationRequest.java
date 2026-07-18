package dev.apicius.resource.dto;

import dev.apicius.document.derivation.CoreType;
import dev.apicius.document.derivation.Refinement;
import dev.apicius.service.DeclarationDraft;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * FEAT-011: one declaration — query parameter, request header, or response header — the same
 * complete, editable state for add and change (UC4 is one atomic save). Every serialized
 * construct is derived server-side; the kind is {@code coreType} (+ optional
 * {@code refinement}) or {@code oneOfValues}, exactly one. Messages are contract wording —
 * the editor renders its own copy keyed on the violated {@code field}.
 */
public record DeclarationRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most 100 characters")
        @Schema(description = "Freeform name; the derived name (property-style for query "
                + "parameters, Hyphenated-Capitalized for headers) is the declaration's "
                + "identity", example = "Price max")
        String name,

        @Schema(description = "The plain-language core type; omit when sending oneOfValues")
        CoreType coreType,

        @Schema(description = "Optional refinement; must belong to the core type's row")
        Refinement refinement,

        @Schema(description = "The \"one of …\" kind's fixed value set — trimmed, at least "
                + "one, non-blank, distinct; omit when sending coreType")
        List<String> oneOfValues,

        @Schema(description = "On an input, whether callers must send it; on a response "
                + "header, whether the answer always carries it (\"always sent\"). Default "
                + "optional either way")
        boolean required,

        @Schema(description = "Becomes the construct's description; omitted from the "
                + "document when blank")
        String description) {

    /** The wire shape as the service consumes it — one bundled value (the FieldRequest idiom). */
    public DeclarationDraft draft() {
        return new DeclarationDraft(name, coreType, refinement, oneOfValues, required,
                description);
    }
}
