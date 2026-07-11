package dev.apicius.service;

/**
 * A field name that derives to an empty property name (FEAT-006 AC9, first half) — invalid
 * regardless of document state, so it maps to <strong>400</strong>, unlike the state-dependent
 * conflicts. A semantic rule bean validation cannot carry: it depends on ADR-0011's
 * derivation, not on the raw string's shape.
 */
public class UnderivableNameException extends RuntimeException {

    public UnderivableNameException(String detail) {
        super(detail);
    }
}
