package dev.apicius.service;

import java.util.UUID;

/** Thrown when a request names an API that doesn't exist; maps to 404 {@code problem+json}. */
public class SpecNotFoundException extends RuntimeException {

    public SpecNotFoundException(UUID specId) {
        super("No API with id " + specId + " exists.");
    }
}
