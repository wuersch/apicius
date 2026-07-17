package dev.apicius.resource.dto;

import dev.apicius.document.derivation.CoreType;
import dev.apicius.document.derivation.Refinement;
import dev.apicius.service.DeclarationDraft;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * FEAT-011: one response-header declaration. {@link DeclarationRequest} minus optionality —
 * optionality is inputs-only, so a response header carrying it is unrepresentable, not
 * validated away.
 */
public record ResponseHeaderRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most 100 characters")
        @Schema(description = "Freeform name; the derived Hyphenated-Capitalized header name "
                + "is the declaration's identity", example = "Request id")
        String name,

        @Schema(description = "The plain-language core type; omit when sending oneOfValues")
        CoreType coreType,

        @Schema(description = "Optional refinement; must belong to the core type's row")
        Refinement refinement,

        @Schema(description = "The \"one of …\" kind's fixed value set — trimmed, at least "
                + "one, non-blank, distinct; omit when sending coreType")
        List<String> oneOfValues,

        @Schema(description = "Becomes the Header Object's description; omitted from the "
                + "document when blank")
        String description) {

    /** The wire shape as the service consumes it — never required (inputs-only optionality). */
    public DeclarationDraft draft() {
        return new DeclarationDraft(name, coreType, refinement, oneOfValues, false, description);
    }
}
