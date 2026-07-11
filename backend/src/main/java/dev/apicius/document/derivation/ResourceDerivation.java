package dev.apicius.document.derivation;

import java.util.List;

/**
 * The full ADR-0010 derivation for one resource: naming plus the chosen operations, in dialog
 * row order. {@code notFoundDescription} serves the 404 every item-path operation declares.
 */
public record ResourceDerivation(
        String schemaName,
        String collectionPath,
        String itemPath,
        String notFoundDescription,
        List<DerivedOperation> operations) {
}
