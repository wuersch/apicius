package dev.apicius.resource.problem;

import dev.apicius.service.ReservedNameException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;

/**
 * Maps a reserved header name (FEAT-011 UC5) to <strong>400</strong> — reserved regardless
 * of document state, unlike the 409 conflicts — keyed to its field like any validation
 * failure.
 */
@Provider
public class ReservedNameExceptionMapper implements ExceptionMapper<ReservedNameException> {

    @Override
    public Response toResponse(ReservedNameException exception) {
        return Response.status(400)
                .type("application/problem+json")
                .entity(ProblemDetail.validationFailed(
                        List.of(new FieldViolation("name", exception.getMessage()))))
                .build();
    }
}
