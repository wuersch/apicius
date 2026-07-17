package dev.apicius.document;

import java.util.List;

/**
 * What clients send (FEAT-009 UC2), derived from the resource's shape — never authored. Add
 * carries the shape's fields (identity included, its auto visibility stating "server-assigned");
 * Update states merge-patch semantics ({@code mergePatch}) and carries no field list (AC5).
 */
public record RequestFacetView(boolean mergePatch, List<FieldView> fields) {
}
