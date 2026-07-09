package dev.apicius.resource.dto;

import dev.apicius.document.CapabilityView;
import dev.apicius.document.derivation.Capability;

/** One capability of a resource: the plain-language label first, derived detail after (PRIN-002). */
public record CapabilityResponse(Capability capability, String label, String method, String path) {

    public static CapabilityResponse from(CapabilityView view) {
        return new CapabilityResponse(view.capability(), view.label(), view.method(), view.path());
    }
}
