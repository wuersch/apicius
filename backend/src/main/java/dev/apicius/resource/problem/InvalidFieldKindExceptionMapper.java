package dev.apicius.resource.problem;

import dev.apicius.service.InvalidFieldKindException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;

/**
 * Maps a refinement outside its core type's ADR-0011 row to <strong>400</strong> — invalid
 * input regardless of document state (FEAT-006).
 */
@Provider
public class InvalidFieldKindExceptionMapper implements ExceptionMapper<InvalidFieldKindException> {

    @Override
    public Response toResponse(InvalidFieldKindException exception) {
        return Response.status(400)
                .type("application/problem+json")
                .entity(ProblemDetail.validationFailed(
                        List.of(new FieldViolation("refinement", exception.getMessage()))))
                .build();
    }
}
