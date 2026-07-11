package dev.apicius.resource.dto;

import dev.apicius.document.derivation.CoreType;
import dev.apicius.document.derivation.FieldVisibility;
import dev.apicius.document.derivation.Refinement;
import dev.apicius.service.FieldDraft;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * FEAT-006: one field edit — the same complete, editable state for add and change (UC3 is one
 * atomic save, not per-attribute patching). Every serialized construct (property name,
 * type/format, array wrapping) is derived server-side per ADR-0011. Messages are contract
 * wording — the editor renders its own copy keyed on the violated {@code field}.
 */
public record FieldRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most 100 characters")
        @Schema(description = "Freeform field name; the JSON property name is derived "
                + "(ADR-0011) and is the field's identity", example = "First name")
        String name,

        @NotNull(message = "a core type is required")
        @Schema(description = "The plain-language core type (ADR-0011)")
        CoreType coreType,

        @Schema(description = "Optional refinement; must belong to the core type's row")
        Refinement refinement,

        @Schema(description = "Wraps the kind as a list (array of the serialized scalar)")
        boolean list,

        @Schema(description = "Membership in the schema's required array")
        boolean required,

        @Schema(description = "Omit to apply the default — write-only for Text as password "
                + "(the house rule), normal otherwise; an explicit value is honored as sent")
        FieldVisibility visibility,

        @Schema(description = "Becomes the property description; omitted from the document "
                + "when blank")
        String description) {

    /** The wire shape as the service consumes it — one bundled value, not seven parameters. */
    public FieldDraft draft() {
        return new FieldDraft(name, coreType, refinement, list, required, visibility, description);
    }
}
