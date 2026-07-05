package dev.apicius.resource.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * RFC 9457 problem details — the management API's error contract (ADR-0002), shared by every
 * endpoint that can reject a request. {@code violations} is the Apicius extension member carrying
 * field-level validation failures; it is omitted for problems that aren't about fields.
 *
 * <p>{@code type} identifies the problem class; per RFC 9457 it is a stable identifier, not
 * necessarily a resolvable URL.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(
        String type,
        String title,
        int status,
        String detail,
        List<FieldViolation> violations) {

    public static ProblemDetail validationFailed(List<FieldViolation> violations) {
        return new ProblemDetail(
                "https://apicius.dev/problems/validation-failed",
                "Validation failed",
                400,
                "The request contains one or more invalid fields.",
                violations);
    }
}
