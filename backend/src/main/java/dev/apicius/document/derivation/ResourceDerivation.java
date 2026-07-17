package dev.apicius.document.derivation;

import java.util.List;

/**
 * The full ADR-0010 derivation for one resource: naming plus the chosen operations, in dialog
 * row order. {@code singularNoun} is the noun as labels display it ("order item"), for copy
 * phrased from the noun.
 */
public record ResourceDerivation(
        String schemaName,
        String collectionPath,
        String itemPath,
        String singularNoun,
        List<DerivedOperation> operations) {
}
