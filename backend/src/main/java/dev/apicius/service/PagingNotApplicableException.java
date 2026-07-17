package dev.apicius.service;

import dev.apicius.document.derivation.Capability;

/**
 * A paging change aimed at a capability paging doesn't apply to (FEAT-010): invalid input
 * regardless of document state — maps to 400 {@code problem+json}, the
 * {@link InvalidFieldKindException} precedent. The UI never offers the toggle there (the
 * facet is absent), so only a hand-crafted request lands here.
 */
public class PagingNotApplicableException extends RuntimeException {

    public PagingNotApplicableException(Capability capability) {
        super("Paging applies to list capabilities only — " + capability
                + " doesn't return a list.");
    }
}
