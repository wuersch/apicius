package dev.apicius.resource.dto;

import dev.apicius.repository.projection.SpecSummaryProjection;
import java.time.Instant;
import java.util.UUID;

public record SpecSummaryResponse(
        UUID id,
        String title,
        String description,
        String apiVersion,
        int resourceCount,
        int operationCount,
        Instant updatedAt) {

    public static SpecSummaryResponse from(SpecSummaryProjection summary) {
        return new SpecSummaryResponse(
                summary.id(),
                summary.title(),
                summary.description(),
                summary.apiVersion(),
                summary.resourceCount(),
                summary.operationCount(),
                summary.updatedAt());
    }
}
