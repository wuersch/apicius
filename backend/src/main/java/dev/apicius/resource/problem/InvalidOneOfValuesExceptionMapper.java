package dev.apicius.resource.problem;

import dev.apicius.service.InvalidOneOfValuesException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;

/**
 * Maps a "one of" value set violating FEAT-011 AC7 to <strong>400</strong> — invalid input
 * regardless of document state, keyed to its field.
 */
@Provider
public class InvalidOneOfValuesExceptionMapper
        implements ExceptionMapper<InvalidOneOfValuesException> {

    @Override
    public Response toResponse(InvalidOneOfValuesException exception) {
        return Response.status(400)
                .type("application/problem+json")
                .entity(ProblemDetail.validationFailed(
                        List.of(new FieldViolation("oneOfValues", exception.getMessage()))))
                .build();
    }
}
