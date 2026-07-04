package dev.apicius.repository.projection;

import java.time.Instant;
import java.util.UUID;

/**
 * The jump-back-in read-model (ADR-0008, FEAT-002 AC1): self-contained — it carries the spec
 * facts the card renders, so resolving it needs neither the list nor the spec entity.
 * {@code capabilityName} is {@code null} when the location resolves to API-level.
 */
public record LastEditedLocationProjection(
        UUID specId,
        String specTitle,
        String apiVersion,
        String capabilityName,
        Instant lastEditedAt) {
}
