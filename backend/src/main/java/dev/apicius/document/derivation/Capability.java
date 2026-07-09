package dev.apicius.document.derivation;

/**
 * The five standard capabilities (ADR-0010), in dialog row order. Enum names are the management
 * API's wire values; the plain-language label each one carries is derived per resource by
 * {@link CanonicalDerivation}.
 */
public enum Capability {
    BROWSE, LOOK_UP, ADD, UPDATE, REMOVE
}
