package dev.apicius.resource.dto;

import dev.apicius.document.FieldView;
import dev.apicius.document.derivation.CoreType;
import dev.apicius.document.derivation.FieldVisibility;
import dev.apicius.document.derivation.Refinement;

/** A field as projected (FEAT-006 AC11) — {@code name} is the derived JSON property name. */
public record FieldResponse(String name, CoreType coreType, Refinement refinement, boolean list,
        boolean required, FieldVisibility visibility, String description) {

    public static FieldResponse from(FieldView view) {
        return new FieldResponse(view.name(), view.kind().core(), view.kind().refinement(),
                view.kind().list(), view.required(), view.visibility(), view.description());
    }
}
