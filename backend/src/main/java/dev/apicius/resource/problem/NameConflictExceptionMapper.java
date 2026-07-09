package dev.apicius.resource.problem;

import dev.apicius.service.NameConflictException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps a name conflict (FEAT-005 AC6) to <strong>409</strong> {@code application/problem+json}
 * — the precedent for business-rule rejections: distinct from 400 ("you sent nonsense"),
 * because the request is fine and the document's current state is what refuses it.
 */
@Provider
public class NameConflictExceptionMapper implements ExceptionMapper<NameConflictException> {

    @Override
    public Response toResponse(NameConflictException exception) {
        return Response.status(409)
                .type("application/problem+json")
                .entity(ProblemDetail.nameConflict(exception.getMessage()))
                .build();
    }
}
