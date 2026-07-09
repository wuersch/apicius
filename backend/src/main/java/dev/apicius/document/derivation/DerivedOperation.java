package dev.apicius.document.derivation;

/**
 * One capability's derived HTTP realisation (ADR-0010) — everything noun- or
 * capability-dependent, pre-computed so the document writer and the uniqueness checker share
 * one source. {@code label} is the operation's {@code summary}, the round-trip carrier of the
 * plain-language name.
 */
public record DerivedOperation(
        Capability capability,
        String method,
        String path,
        String operationId,
        String label,
        String successStatus,
        String successDescription) {
}
