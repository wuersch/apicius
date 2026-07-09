package dev.apicius.resource.dto;

import dev.apicius.document.ResourceView;
import java.util.List;

public record ResourceResponse(String name, String description, List<CapabilityResponse> capabilities) {

    public static ResourceResponse from(ResourceView view) {
        return new ResourceResponse(view.name(), view.description(),
                view.capabilities().stream().map(CapabilityResponse::from).toList());
    }
}
