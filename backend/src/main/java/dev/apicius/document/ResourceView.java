package dev.apicius.document;

import java.util.List;

/**
 * A resource as the concept projection sees it: the schema name, its description, and the
 * recognized capabilities (at least one — a capability-less schema is a datatype, not a
 * resource; see the glossary).
 */
public record ResourceView(String name, String description, List<CapabilityView> capabilities) {
}
