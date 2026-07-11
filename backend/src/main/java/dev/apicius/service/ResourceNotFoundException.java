package dev.apicius.service;

/** The addressed resource (schema name) does not exist in this API — 404 (FEAT-006). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String schemaName) {
        super("No resource named '" + schemaName + "' exists in this API.");
    }
}
