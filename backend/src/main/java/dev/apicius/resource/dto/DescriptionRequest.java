package dev.apicius.resource.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * FEAT-012: a quiet description's payload — free prose, deliberately unvalidated (no length or
 * content rules), because blank is the clear gesture, not an error.
 */
public record DescriptionRequest(
        @Schema(description = "The element's note for readers; blank or absent clears it — "
                + "removed from the document (capability, resource) or reset to the derived "
                + "default (answer)")
        String description) {
}
