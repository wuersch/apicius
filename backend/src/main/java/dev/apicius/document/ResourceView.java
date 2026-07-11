package dev.apicius.document;

import java.util.List;

/**
 * A resource as the concept projection sees it: the schema name, its description, the
 * recognized capabilities (at least one — a capability-less schema is a datatype, not a
 * resource; see the glossary), and its shape's fields (FEAT-006 AC11).
 */
public record ResourceView(String name, String description, List<CapabilityView> capabilities,
        List<FieldView> fields) {
}
