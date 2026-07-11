package dev.apicius.service;

import dev.apicius.document.derivation.CoreType;
import dev.apicius.document.derivation.Refinement;

/**
 * A refinement outside its core type's row (ADR-0011) — invalid regardless of document state,
 * so <strong>400</strong>. Our UI cannot construct this (refinement options are filtered by
 * core type); the check exists because the contract must hold for any client.
 */
public class InvalidFieldKindException extends RuntimeException {

    public InvalidFieldKindException(CoreType coreType, Refinement refinement) {
        super("'" + refinement + "' does not refine '" + coreType + "'.");
    }
}
