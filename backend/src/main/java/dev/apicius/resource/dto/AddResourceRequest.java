package dev.apicius.resource.dto;

import dev.apicius.document.derivation.CanonicalDerivation;
import dev.apicius.document.derivation.Capability;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * FEAT-005: the New resource dialog's payload — the noun and the chosen capabilities; every
 * derived construct (schema, paths, operations) is computed server-side (ADR-0010). Messages
 * are contract wording — the dialog renders its own copy keyed on the violated {@code field}.
 */
public record AddResourceRequest(
        @NotBlank(message = "must not be blank")
        @Pattern(regexp = CanonicalDerivation.NAME_PATTERN,
                message = "must start with a letter and contain only letters, digits and single spaces")
        @Size(max = 100, message = "must be at most 100 characters")
        @Schema(description = "The noun; becomes the schema name (PascalCase) and, pluralized, "
                + "the collection path", example = "Product")
        String name,

        @Schema(description = "Becomes the schema description; omitted from the document when blank")
        String description,

        @NotEmpty(message = "at least one capability is required")
        @Schema(description = "The chosen standard capabilities (ADR-0010); duplicates are ignored")
        List<Capability> capabilities) {
}
