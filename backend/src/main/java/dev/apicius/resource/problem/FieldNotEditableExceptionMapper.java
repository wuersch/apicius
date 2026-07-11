package dev.apicius.resource.problem;

import dev.apicius.service.FieldNotEditableException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps a mutation aimed at the identity field (FEAT-006 AC7) to <strong>409</strong> — a
 * well-formed request the document's rules refuse, same family as a name conflict.
 */
@Provider
public class FieldNotEditableExceptionMapper implements ExceptionMapper<FieldNotEditableException> {

    @Override
    public Response toResponse(FieldNotEditableException exception) {
        return Response.status(409)
                .type("application/problem+json")
                .entity(ProblemDetail.fieldNotEditable(exception.getMessage()))
                .build();
    }
}
