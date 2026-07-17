package dev.apicius.document;

import java.util.List;

/**
 * The Headers facet — request headers only (FEAT-011; response headers surface with the
 * success answer they ship with). {@code derived} lines are Apicius-supplied display, not
 * document content (the content-negotiation line, FEAT-009 AC3); {@code authored} rows are
 * the capability's request-header declarations, projected from the operation.
 */
public record HeadersFacetView(List<HeaderLineView> derived, List<DeclarationView> authored) {
}
