package dev.apicius.service;

/**
 * A business-rule rejection: the request is well-formed but conflicts with the document's
 * current state (FEAT-005 AC6) — maps to 409 {@code problem+json}. The message is the
 * RFC 9457 {@code detail}, composed by the service, which knows both sides of the conflict.
 */
public class NameConflictException extends RuntimeException {

    public NameConflictException(String detail) {
        super(detail);
    }
}
