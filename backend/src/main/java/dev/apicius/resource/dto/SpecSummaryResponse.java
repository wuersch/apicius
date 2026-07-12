package dev.apicius.resource.dto;

import dev.apicius.domain.Spec;
import dev.apicius.repository.projection.SpecSummaryProjection;
import java.time.Instant;
import java.util.UUID;

public record SpecSummaryResponse(
        UUID id,
        String title,
        String description,
        String apiVersion,
        String specVersion,
        int resourceCount,
        int operationCount,
        Instant updatedAt) {

    public static SpecSummaryResponse from(SpecSummaryProjection summary) {
        return new SpecSummaryResponse(
                summary.id(),
                summary.title(),
                summary.description(),
                summary.apiVersion(),
                summary.specVersion(),
                summary.resourceCount(),
                summary.operationCount(),
                summary.updatedAt());
    }

    /** A just-created API is a spec summary — the create path (FEAT-003) returns the same shape. */
    public static SpecSummaryResponse from(Spec spec) {
        return new SpecSummaryResponse(
                spec.id,
                spec.title,
                spec.description,
                spec.apiVersion,
                spec.specVersion,
                spec.resourceCount,
                spec.operationCount,
                spec.updatedAt);
    }
}
