package dev.apicius.resource.dto;

import dev.apicius.service.SpecDetail;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** One API for the editor (FEAT-005 AC8): the summary facts plus the recognized resources. */
public record SpecDetailResponse(
        UUID id,
        String title,
        String description,
        String apiVersion,
        int resourceCount,
        int operationCount,
        Instant updatedAt,
        List<ResourceResponse> resources) {

    public static SpecDetailResponse from(SpecDetail detail) {
        return new SpecDetailResponse(
                detail.spec().id,
                detail.spec().title,
                detail.spec().description,
                detail.spec().apiVersion,
                detail.spec().resourceCount,
                detail.spec().operationCount,
                detail.spec().updatedAt,
                detail.resources().stream().map(ResourceResponse::from).toList());
    }
}
