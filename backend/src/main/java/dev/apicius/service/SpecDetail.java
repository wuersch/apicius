package dev.apicius.service;

import dev.apicius.document.ResourceView;
import dev.apicius.domain.Spec;
import java.util.List;

/** One API for the editor: the spec row plus the concept projection of its document (AC8). */
public record SpecDetail(Spec spec, List<ResourceView> resources) {
}
