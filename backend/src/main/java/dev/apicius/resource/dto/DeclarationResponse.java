package dev.apicius.resource.dto;

import dev.apicius.document.DeclarationView;
import dev.apicius.document.derivation.CoreType;
import dev.apicius.document.derivation.ParameterKind;
import dev.apicius.document.derivation.Refinement;
import java.util.List;

/**
 * A declaration as projected (FEAT-011) — {@code name} is the derived identity. Exactly one
 * of {@code coreType} and {@code oneOfValues} is set, mirroring the request's kind encoding;
 * {@code required} is always false on a response header.
 */
public record DeclarationResponse(String name, CoreType coreType, Refinement refinement,
        List<String> oneOfValues, boolean required, String description) {

    public static DeclarationResponse from(DeclarationView view) {
        return switch (view.kind()) {
            case ParameterKind.Scalar scalar -> new DeclarationResponse(view.name(),
                    scalar.kind().core(), scalar.kind().refinement(), null, view.required(),
                    view.description());
            case ParameterKind.OneOf oneOf -> new DeclarationResponse(view.name(), null, null,
                    oneOf.values(), view.required(), view.description());
        };
    }
}
