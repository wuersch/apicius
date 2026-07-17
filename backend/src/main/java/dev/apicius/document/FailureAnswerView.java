package dev.apicius.document;

/**
 * One applicable standard failure answer (FEAT-009). {@code present} is derived structurally:
 * the operation answers this status with the canonical shared reference — no marker, no
 * extension. The plain-language name is client vocabulary, keyed on the status.
 */
public record FailureAnswerView(String status, boolean present) {
}
