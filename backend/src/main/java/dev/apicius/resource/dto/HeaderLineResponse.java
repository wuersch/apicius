package dev.apicius.resource.dto;

import dev.apicius.document.HeaderLineView;

/** One Headers-facet line (FEAT-009 AC3); {@code derived} lines are Apicius-supplied, read-only. */
public record HeaderLineResponse(String name, String value, boolean derived) {

    public static HeaderLineResponse from(HeaderLineView view) {
        return new HeaderLineResponse(view.name(), view.value(), view.derived());
    }
}
