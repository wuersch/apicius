package dev.apicius.resource.problem;

import dev.apicius.service.PagingNotApplicableException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;

/**
 * Maps a paging change on a non-list capability to <strong>400</strong> — invalid input
 * regardless of document state (FEAT-010), like {@link InvalidFieldKindExceptionMapper}.
 */
@Provider
public class PagingNotApplicableExceptionMapper
        implements ExceptionMapper<PagingNotApplicableException> {

    @Override
    public Response toResponse(PagingNotApplicableException exception) {
        return Response.status(400)
                .type("application/problem+json")
                .entity(ProblemDetail.validationFailed(
                        List.of(new FieldViolation("capability", exception.getMessage()))))
                .build();
    }
}
