package dev.apicius.resource.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * FEAT-007: the Edit details dialog's payload. Messages here are contract wording — the dialog
 * renders its own user-facing copy keyed on the violated {@code field}.
 */
public record UpdateSpecDetailsRequest(
        @NotBlank(message = "must not be blank")
        @Schema(description = "Rewrites info.title", example = "Payments API")
        String title,

        @Schema(description = "Rewrites info.description; removed from the document when blank")
        String description,

        @NotBlank(message = "must not be blank")
        @Schema(description = "Rewrites info.version — the API's own version, required by the "
                + "OpenAPI spec; not the openapi version, which is fixed after creation",
                example = "2.3.0")
        String version) {
}
