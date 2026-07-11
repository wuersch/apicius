package dev.apicius.service;

/** The addressed field (property name) does not exist on this shape — 404 (FEAT-006). */
public class FieldNotFoundException extends RuntimeException {

    public FieldNotFoundException(String schemaName, String propertyName) {
        super("'" + schemaName + "' has no field named '" + propertyName + "'.");
    }
}
