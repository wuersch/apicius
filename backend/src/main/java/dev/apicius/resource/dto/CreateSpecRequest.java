package dev.apicius.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * FEAT-003: the Create dialog's payload. Messages here are contract wording — the dialog renders
 * its own user-facing copy keyed on the violated {@code field}.
 */
public record CreateSpecRequest(
        @NotBlank(message = "must not be blank")
        @Schema(description = "Becomes info.title", example = "Storefront API")
        String title,

        @Schema(description = "Becomes info.description; omitted from the document when blank")
        String description,

        @Pattern(regexp = "3\\.[0-2]", message = "must be one of 3.0, 3.1, 3.2")
        @Schema(description = "OpenAPI minor version, fixed after creation; defaults to 3.1 and "
                + "is persisted as that minor's latest patch (FEAT-003)",
                enumeration = {"3.0", "3.1", "3.2"}, defaultValue = "3.1")
        String specVersion) {
}
