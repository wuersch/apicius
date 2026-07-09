package dev.apicius.document;

import java.util.List;

/**
 * The concept projection of a stored document (ADR-0009 traversal): every schema name and path
 * key (what uniqueness checks compare against — AC6 covers resources <em>and</em> datatypes),
 * plus the recognized resources the editor lists (FEAT-005 AC8).
 */
public record DocumentProjection(
        List<String> schemaNames,
        List<String> paths,
        List<ResourceView> resources) {
}
