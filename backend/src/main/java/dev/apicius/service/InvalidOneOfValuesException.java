package dev.apicius.service;

/**
 * A "one of" value set violating FEAT-011 AC7 — after trimming: at least one value,
 * non-blank, distinct. Invalid regardless of document state, so <strong>400</strong>. The
 * rules live in {@code ParameterKind.OneOf}'s constructor; this is their service-layer
 * rejection type.
 */
public class InvalidOneOfValuesException extends RuntimeException {

    public InvalidOneOfValuesException(String detail) {
        super(detail);
    }
}
