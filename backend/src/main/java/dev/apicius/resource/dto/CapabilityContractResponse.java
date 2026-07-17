package dev.apicius.resource.dto;

import dev.apicius.document.CapabilityContractView;
import java.util.List;

/**
 * One capability's full contract (FEAT-009 AC1) in the stable facet order: identity,
 * description, Request, Headers, Answers. A facet that doesn't apply is {@code null}, never
 * empty (AC2). {@code singularNoun} lets the client phrase copy from the noun (the 404's
 * plain-language name).
 */
public record CapabilityContractResponse(
        CapabilityResponse capability,
        String description,
        String singularNoun,
        RequestFacetResponse request,
        List<HeaderLineResponse> headers,
        AnswersFacetResponse answers) {

    public static CapabilityContractResponse from(CapabilityContractView view) {
        return new CapabilityContractResponse(
                CapabilityResponse.from(view.identity()),
                view.description(),
                view.singularNoun(),
                view.request() == null ? null : RequestFacetResponse.from(view.request()),
                view.headers().stream().map(HeaderLineResponse::from).toList(),
                AnswersFacetResponse.from(view.answers()));
    }
}
