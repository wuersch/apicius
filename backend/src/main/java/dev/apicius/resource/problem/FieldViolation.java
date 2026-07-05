package dev.apicius.resource.problem;

/** One field-level failure inside a {@link ProblemDetail} — which field, and why. */
public record FieldViolation(String field, String message) {
}
