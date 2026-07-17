package dev.apicius.resource.dto;

import dev.apicius.document.RequestFacetView;
import java.util.List;

/** What clients send (FEAT-009 UC2/AC5) — derived from the shape, never authored. */
public record RequestFacetResponse(boolean mergePatch, List<FieldResponse> fields) {

    public static RequestFacetResponse from(RequestFacetView view) {
        return new RequestFacetResponse(view.mergePatch(),
                view.fields().stream().map(FieldResponse::from).toList());
    }
}
