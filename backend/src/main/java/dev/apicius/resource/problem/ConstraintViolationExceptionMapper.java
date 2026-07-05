package dev.apicius.resource.problem;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;

/**
 * Maps bean-validation failures on request DTOs to RFC 9457 {@code application/problem+json}
 * (ADR-0002). Registering this replaces the Quarkus default violation report, so all write
 * endpoints share one error shape.
 */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(violation -> new FieldViolation(fieldName(violation), violation.getMessage()))
                .toList();
        return Response.status(400)
                .type("application/problem+json")
                .entity(ProblemDetail.validationFailed(violations))
                .build();
    }

    /**
     * The DTO field name is the path's leaf — the prefix ({@code create.request.title}) is
     * method/parameter plumbing the client never sees.
     */
    private static String fieldName(ConstraintViolation<?> violation) {
        String leaf = null;
        for (Path.Node node : violation.getPropertyPath()) {
            leaf = node.getName();
        }
        return leaf;
    }
}
