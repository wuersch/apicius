package dev.apicius.service;

/**
 * A mutation aimed at the identity field (FEAT-006 AC7): {@code id} cannot be renamed,
 * retyped, changed, or removed. Well-formed request, refused by the document's rules — the
 * 409 family, like a name conflict.
 */
public class FieldNotEditableException extends RuntimeException {

    public FieldNotEditableException() {
        super("'id' is the identity field — the server sets it; it cannot be changed or removed.");
    }
}
