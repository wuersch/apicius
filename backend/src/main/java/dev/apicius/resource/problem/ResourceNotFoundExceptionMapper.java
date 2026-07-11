package dev.apicius.resource.problem;

import dev.apicius.service.ResourceNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Maps an unknown resource (schema name) to <strong>404</strong> (FEAT-006). */
@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {

    @Override
    public Response toResponse(ResourceNotFoundException exception) {
        return Response.status(404)
                .type("application/problem+json")
                .entity(ProblemDetail.notFound(exception.getMessage()))
                .build();
    }
}
