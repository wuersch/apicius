package dev.apicius.document;

import java.util.List;

/**
 * One capability's full contract, as the contract view presents it (FEAT-009): the identity
 * facet (the {@link CapabilityView} the resource list already shows), the description, and
 * the Request / query-parameter / Paging / Headers / Answers facets — each projected from
 * the document, in the stable facet order (Paging joined with FEAT-010, query parameters
 * between Request and Paging with FEAT-011). A facet that doesn't apply is {@code null},
 * never empty (AC2) — query parameters and headers apply to every capability, so those are
 * lists that may be empty (not-yet-authored, not not-applicable). {@code singularNoun}
 * carries the noun for copy the client phrases from it (the 404's plain-language name).
 */
public record CapabilityContractView(
        CapabilityView identity,
        String description,
        String singularNoun,
        RequestFacetView request,
        List<DeclarationView> queryParameters,
        PagingFacetView paging,
        HeadersFacetView headers,
        AnswersFacetView answers) {
}
