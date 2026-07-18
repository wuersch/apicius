package dev.apicius.service;

import dev.apicius.document.derivation.DeclarationLocation;

/** A change or removal addressing a declaration this capability doesn't carry — 404. */
public class DeclarationNotFoundException extends RuntimeException {

    public DeclarationNotFoundException(DeclarationLocation location, String name) {
        super("No " + describe(location) + " named '" + name + "' exists on this capability.");
    }

    private static String describe(DeclarationLocation location) {
        return switch (location) {
            case QUERY_PARAMETER -> "query parameter";
            case REQUEST_HEADER -> "request header";
            case RESPONSE_HEADER -> "response header";
        };
    }
}
