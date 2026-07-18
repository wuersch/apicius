package dev.apicius.resource.dto;

import dev.apicius.document.CapabilityContractView;
import java.util.List;

/**
 * One capability's full contract (FEAT-009 AC1) in the stable facet order: identity,
 * description, Request, query parameters (FEAT-011), Paging (FEAT-010), Headers, Answers. A
 * facet that doesn't apply is {@code null}, never empty (AC2) — query parameters and headers
 * apply everywhere, so those are lists that may be empty. {@code singularNoun} lets the
 * client phrase copy from the noun (the 404's plain-language name).
 */
public record CapabilityContractResponse(
        CapabilityResponse capability,
        String description,
        String singularNoun,
        RequestFacetResponse request,
        List<DeclarationResponse> queryParameters,
        PagingFacetResponse paging,
        HeadersFacetResponse headers,
        AnswersFacetResponse answers) {

    public static CapabilityContractResponse from(CapabilityContractView view) {
        return new CapabilityContractResponse(
                CapabilityResponse.from(view.identity()),
                view.description(),
                view.singularNoun(),
                view.request() == null ? null : RequestFacetResponse.from(view.request()),
                view.queryParameters().stream().map(DeclarationResponse::from).toList(),
                view.paging() == null ? null : PagingFacetResponse.from(view.paging()),
                HeadersFacetResponse.from(view.headers()),
                AnswersFacetResponse.from(view.answers()));
    }
}
