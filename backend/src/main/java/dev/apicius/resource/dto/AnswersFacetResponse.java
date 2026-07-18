package dev.apicius.resource.dto;

import dev.apicius.document.AnswersFacetView;
import java.util.List;

/**
 * What comes back (FEAT-009): the success answer — with the response headers it ships
 * (FEAT-011) — plus the standard failures' states.
 */
public record AnswersFacetResponse(
        String successStatus,
        String successDescription,
        List<DeclarationResponse> successHeaders,
        List<FailureAnswerResponse> failures) {

    public static AnswersFacetResponse from(AnswersFacetView view) {
        return new AnswersFacetResponse(view.successStatus(), view.successDescription(),
                view.successHeaders().stream().map(DeclarationResponse::from).toList(),
                view.failures().stream().map(FailureAnswerResponse::from).toList());
    }
}
