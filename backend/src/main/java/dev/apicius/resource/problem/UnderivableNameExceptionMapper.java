package dev.apicius.resource.problem;

import dev.apicius.service.UnderivableNameException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;

/**
 * Maps a name that derives to nothing (FEAT-006 AC9) to <strong>400</strong> — invalid input,
 * not a state conflict — keyed to its field like any validation failure.
 */
@Provider
public class UnderivableNameExceptionMapper implements ExceptionMapper<UnderivableNameException> {

    @Override
    public Response toResponse(UnderivableNameException exception) {
        return Response.status(400)
                .type("application/problem+json")
                .entity(ProblemDetail.validationFailed(
                        List.of(new FieldViolation("name", exception.getMessage()))))
                .build();
    }
}
