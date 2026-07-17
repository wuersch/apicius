package dev.apicius.document;

import java.util.List;

/**
 * One capability's full contract, as the contract view presents it (FEAT-009): the identity
 * facet (the {@link CapabilityView} the resource list already shows), the description, and the
 * Request / Paging / Headers / Answers facets — each projected from the document, in the
 * stable facet order (Paging joined with FEAT-010). A facet that doesn't apply is {@code null},
 * never empty (AC2). {@code singularNoun} carries the noun for copy the client phrases from it
 * (the 404's plain-language name).
 */
public record CapabilityContractView(
        CapabilityView identity,
        String description,
        String singularNoun,
        RequestFacetView request,
        PagingFacetView paging,
        List<HeaderLineView> headers,
        AnswersFacetView answers) {
}
