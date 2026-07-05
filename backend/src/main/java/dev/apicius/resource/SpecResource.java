package dev.apicius.resource;

import dev.apicius.domain.Spec;
import dev.apicius.resource.dto.CreateSpecRequest;
import dev.apicius.resource.dto.LastEditedLocationResponse;
import dev.apicius.resource.dto.SpecListResponse;
import dev.apicius.resource.dto.SpecSummaryResponse;
import dev.apicius.resource.problem.ProblemDetail;
import dev.apicius.security.CurrentUser;
import dev.apicius.service.SpecService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/specs")
@Tag(name = "specs")
@Authenticated
public class SpecResource {

    @Inject
    SpecService specService;

    @Inject
    CurrentUser currentUser;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "listSpecs", summary = "List all APIs as summary projections (ADR-0008)")
    public SpecListResponse list() {
        return SpecListResponse.of(
                specService.listSummaries().stream().map(SpecSummaryResponse::from).toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "createSpec", summary = "Create a new, empty API (FEAT-003)")
    @APIResponse(responseCode = "201", description = "Created",
            content = @Content(schema = @Schema(implementation = SpecSummaryResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation failed",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public Response create(@Valid CreateSpecRequest request, @Context UriInfo uriInfo) {
        Spec spec = specService.createEmpty(currentUser.require(), request.title(),
                request.description(), request.specVersion());
        URI location = uriInfo.getAbsolutePathBuilder().path(spec.id.toString()).build();
        return Response.created(location).entity(SpecSummaryResponse.from(spec)).build();
    }

    @GET
    @Path("/last-edited")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getLastEditedLocation",
            summary = "The current user's last-edited location, powering jump-back-in")
    @APIResponse(responseCode = "200", description = "The location",
            content = @Content(schema = @Schema(implementation = LastEditedLocationResponse.class)))
    @APIResponse(responseCode = "204", description = "The user has not edited anything yet")
    public Response lastEdited() {
        return specService.lastEditedFor(currentUser.require())
                .map(location -> Response.ok(LastEditedLocationResponse.from(location)).build())
                .orElseGet(() -> Response.noContent().build());
    }
}
