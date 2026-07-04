package dev.apicius.resource.dto;

import dev.apicius.repository.projection.LastEditedLocationProjection;
import java.time.Instant;
import java.util.UUID;

/** Self-contained jump-back-in pointer (FEAT-002 AC1); {@code capabilityName} null = API-level. */
public record LastEditedLocationResponse(
        UUID specId,
        String specTitle,
        String apiVersion,
        String capabilityName,
        Instant lastEditedAt) {

    public static LastEditedLocationResponse from(LastEditedLocationProjection location) {
        return new LastEditedLocationResponse(
                location.specId(),
                location.specTitle(),
                location.apiVersion(),
                location.capabilityName(),
                location.lastEditedAt());
    }
}
