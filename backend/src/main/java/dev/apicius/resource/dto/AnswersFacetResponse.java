package dev.apicius.resource.dto;

import dev.apicius.document.AnswersFacetView;
import java.util.List;

/** What comes back (FEAT-009): the success answer plus the standard failures' states. */
public record AnswersFacetResponse(
        String successStatus,
        String successDescription,
        List<FailureAnswerResponse> failures) {

    public static AnswersFacetResponse from(AnswersFacetView view) {
        return new AnswersFacetResponse(view.successStatus(), view.successDescription(),
                view.failures().stream().map(FailureAnswerResponse::from).toList());
    }
}
