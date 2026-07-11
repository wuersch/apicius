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

    public static ProblemDetail notFound(String detail) {
        return new ProblemDetail(
                "https://apicius.dev/problems/not-found",
                "Not found",
                404,
                detail,
                null);
    }

    /**
     * A name conflict (FEAT-005 AC6): well-formed request, colliding document state. The
     * {@code violations} entry keys the conflict to its field so dialogs can render it inline,
     * like a validation failure.
     */
    public static ProblemDetail nameConflict(String detail) {
        return new ProblemDetail(
                "https://apicius.dev/problems/name-conflict",
                "Name conflict",
                409,
                detail,
                List.of(new FieldViolation("name", detail)));
    }

    /**
     * A mutation aimed at a field the rules lock (FEAT-006 AC7 — the identity field):
     * well-formed request, refused by the document's rules. No {@code violations} — the
     * problem is the target, not an input field.
     */
    public static ProblemDetail fieldNotEditable(String detail) {
        return new ProblemDetail(
                "https://apicius.dev/problems/field-not-editable",
                "Field not editable",
                409,
                detail,
                null);
    }
}
