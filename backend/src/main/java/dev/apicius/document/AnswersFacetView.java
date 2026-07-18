package dev.apicius.document;

import java.util.List;

/**
 * What comes back (FEAT-009): the success answer as the document declares it — with the
 * response headers it ships (FEAT-011) — plus every applicable standard failure answer with
 * its structural present/absent state — absent ones are shown as available, and only the
 * adopt action writes (AC4).
 */
public record AnswersFacetView(
        String successStatus,
        String successDescription,
        List<DeclarationView> successHeaders,
        List<FailureAnswerView> failures) {
}
