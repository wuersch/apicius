package dev.apicius.resource.dto;

import dev.apicius.document.HeadersFacetView;
import java.util.List;

/**
 * The Headers facet — request headers only (FEAT-011): the Apicius-supplied derived lines
 * (FEAT-009 AC3, read-only) plus the capability's authored request headers.
 */
public record HeadersFacetResponse(List<HeaderLineResponse> derived,
        List<DeclarationResponse> authored) {

    public static HeadersFacetResponse from(HeadersFacetView view) {
        return new HeadersFacetResponse(
                view.derived().stream().map(HeaderLineResponse::from).toList(),
                view.authored().stream().map(DeclarationResponse::from).toList());
    }
}
