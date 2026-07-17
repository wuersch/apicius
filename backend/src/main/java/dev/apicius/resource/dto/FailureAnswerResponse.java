package dev.apicius.resource.dto;

import dev.apicius.document.FailureAnswerView;

/** One standard failure answer's structural state (FEAT-009 AC4): present, or available. */
public record FailureAnswerResponse(String status, boolean present) {

    public static FailureAnswerResponse from(FailureAnswerView view) {
        return new FailureAnswerResponse(view.status(), view.present());
    }
}
