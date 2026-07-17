package dev.apicius.resource.problem;

import dev.apicius.service.DeclarationNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Maps a change or removal addressing an absent declaration (FEAT-011) to <strong>404</strong>. */
@Provider
public class DeclarationNotFoundExceptionMapper
        implements ExceptionMapper<DeclarationNotFoundException> {

    @Override
    public Response toResponse(DeclarationNotFoundException exception) {
        return Response.status(404)
                .type("application/problem+json")
                .entity(ProblemDetail.notFound(exception.getMessage()))
                .build();
    }
}
