package dev.apicius.service;

import dev.apicius.document.derivation.Capability;

/** The addressed capability does not exist on this resource — 404 (FEAT-009). */
public class CapabilityNotFoundException extends RuntimeException {

    public CapabilityNotFoundException(String schemaName, Capability capability) {
        super("'" + schemaName + "' has no capability '" + capability + "'.");
    }
}
