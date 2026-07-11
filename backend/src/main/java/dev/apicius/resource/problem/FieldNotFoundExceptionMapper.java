package dev.apicius.resource.problem;

import dev.apicius.service.FieldNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Maps an unknown field (property name) to <strong>404</strong> (FEAT-006). */
@Provider
public class FieldNotFoundExceptionMapper implements ExceptionMapper<FieldNotFoundException> {

    @Override
    public Response toResponse(FieldNotFoundException exception) {
        return Response.status(404)
                .type("application/problem+json")
                .entity(ProblemDetail.notFound(exception.getMessage()))
                .build();
    }
}
