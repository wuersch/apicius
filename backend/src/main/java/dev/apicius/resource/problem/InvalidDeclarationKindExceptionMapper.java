package dev.apicius.resource.problem;

import dev.apicius.service.InvalidDeclarationKindException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;

/**
 * Maps an ambiguous or missing kind encoding (FEAT-011 — a core type or "one of" values,
 * exactly one) to <strong>400</strong> — invalid input regardless of document state.
 */
@Provider
public class InvalidDeclarationKindExceptionMapper
        implements ExceptionMapper<InvalidDeclarationKindException> {

    @Override
    public Response toResponse(InvalidDeclarationKindException exception) {
        return Response.status(400)
                .type("application/problem+json")
                .entity(ProblemDetail.validationFailed(
                        List.of(new FieldViolation("coreType", exception.getMessage()))))
                .build();
    }
}
