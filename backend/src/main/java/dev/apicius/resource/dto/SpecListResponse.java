package dev.apicius.resource.dto;

import java.util.List;

/**
 * Wrapped collection envelope (ADR-0002). Deliberately carries no paging fields yet: the
 * pagination mechanism (pages vs. load-on-scroll) is undecided, and either extends this shape
 * additively (FEAT-002 non-goal).
 */
public record SpecListResponse(List<SpecSummaryResponse> items, long total) {

    public static SpecListResponse of(List<SpecSummaryResponse> items) {
        return new SpecListResponse(items, items.size());
    }
}
