package dev.apicius.resource.problem;

import dev.apicius.service.CapabilityNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Maps an unknown capability to <strong>404</strong> (FEAT-009). */
@Provider
public class CapabilityNotFoundExceptionMapper
        implements ExceptionMapper<CapabilityNotFoundException> {

    @Override
    public Response toResponse(CapabilityNotFoundException exception) {
        return Response.status(404)
                .type("application/problem+json")
                .entity(ProblemDetail.notFound(exception.getMessage()))
                .build();
    }
}
