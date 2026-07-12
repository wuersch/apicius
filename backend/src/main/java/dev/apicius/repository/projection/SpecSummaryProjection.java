package dev.apicius.repository.projection;

import java.time.Instant;
import java.util.UUID;

/**
 * The narrow read-model for the home's card list (ADR-0008): only the summary columns, selected
 * via a JPQL constructor expression so {@code Spec} (and its {@code body}) is never hydrated
 * (FEAT-002 AC5).
 */
public record SpecSummaryProjection(
        UUID id,
        String title,
        String description,
        String apiVersion,
        String specVersion,
        int resourceCount,
        int operationCount,
        Instant updatedAt) {
}
