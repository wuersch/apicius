package dev.apicius.resource.problem;

import dev.apicius.service.SpecNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Maps a missing API to 404 {@code application/problem+json} (RFC 9457, ADR-0002). */
@Provider
public class SpecNotFoundExceptionMapper implements ExceptionMapper<SpecNotFoundException> {

    @Override
    public Response toResponse(SpecNotFoundException exception) {
        return Response.status(404)
                .type("application/problem+json")
                .entity(ProblemDetail.notFound(exception.getMessage()))
                .build();
    }
}
