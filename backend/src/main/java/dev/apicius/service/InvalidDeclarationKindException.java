package dev.apicius.service;

/**
 * A declaration whose kind encoding is ambiguous or missing (FEAT-011): a kind is FEAT-006's
 * vocabulary <em>or</em> "one of …" — exactly one of {@code coreType} and {@code oneOfValues}
 * must be sent. Invalid regardless of document state, so <strong>400</strong>. Our UI cannot
 * construct this (the kind dropdown is one choice); the check exists because the contract
 * must hold for any client.
 */
public class InvalidDeclarationKindException extends RuntimeException {

    public InvalidDeclarationKindException(String detail) {
        super(detail);
    }
}
