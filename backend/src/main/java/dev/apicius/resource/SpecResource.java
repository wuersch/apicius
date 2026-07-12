package dev.apicius.resource;

import dev.apicius.document.FieldView;
import dev.apicius.document.ResourceView;
import dev.apicius.domain.Spec;
import dev.apicius.resource.dto.AddResourceRequest;
import dev.apicius.resource.dto.CreateSpecRequest;
import dev.apicius.resource.dto.FieldRequest;
import dev.apicius.resource.dto.FieldResponse;
import dev.apicius.resource.dto.LastEditedLocationResponse;
import dev.apicius.resource.dto.ResourceResponse;
import dev.apicius.resource.dto.SpecDetailResponse;
import dev.apicius.resource.dto.SpecListResponse;
import dev.apicius.resource.dto.SpecSummaryResponse;
import dev.apicius.resource.dto.UpdateSpecDetailsRequest;
import dev.apicius.resource.problem.ProblemDetail;
import dev.apicius.security.CurrentUser;
import dev.apicius.service.SpecService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.UUID;
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
    @Path("/{specId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getSpec",
            summary = "One API with its resources and their capabilities (FEAT-005 AC8)")
    @APIResponse(responseCode = "200", description = "The API",
            content = @Content(schema = @Schema(implementation = SpecDetailResponse.class)))
    @APIResponse(responseCode = "404", description = "No such API",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public SpecDetailResponse get(@PathParam("specId") UUID specId) {
        return SpecDetailResponse.from(specService.detail(specId));
    }

    @PATCH
    @Path("/{specId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "updateSpecDetails",
            summary = "Rewrite an API's details — info.title, info.version, info.description — "
                    + "never its contents (FEAT-007)")
    @APIResponse(responseCode = "200", description = "The updated summary",
            content = @Content(schema = @Schema(implementation = SpecSummaryResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation failed",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "404", description = "No such API",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public SpecSummaryResponse updateDetails(@PathParam("specId") UUID specId,
            @Valid UpdateSpecDetailsRequest request) {
        return SpecSummaryResponse.from(specService.updateDetails(currentUser.require(), specId,
                request.title(), request.description(), request.version()));
    }

    @POST
    @Path("/{specId}/duplicate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "duplicateSpec",
            summary = "Duplicate an API — a fork: the same document under a new identity, "
                    + "titled \"<title> (copy)\" and owned by the duplicator (FEAT-007)")
    @APIResponse(responseCode = "201", description = "Created",
            content = @Content(schema = @Schema(implementation = SpecSummaryResponse.class)))
    @APIResponse(responseCode = "404", description = "No such API",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public Response duplicate(@PathParam("specId") UUID specId, @Context UriInfo uriInfo) {
        Spec copy = specService.duplicate(currentUser.require(), specId);
        // Location addresses the new API itself — the absolute path would nest it under
        // .../duplicate/{id}, which 404s.
        URI location = uriInfo.getBaseUriBuilder().path(SpecResource.class)
                .path(copy.id.toString()).build();
        return Response.created(location).entity(SpecSummaryResponse.from(copy)).build();
    }

    @DELETE
    @Path("/{specId}")
    @Operation(operationId = "deleteSpec",
            summary = "Delete an API permanently — no archive, no undo; every user's "
                    + "jump-back-in pointer at it is cleared (FEAT-007)")
    @APIResponse(responseCode = "204", description = "Deleted")
    @APIResponse(responseCode = "404", description = "No such API",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public Response delete(@PathParam("specId") UUID specId) {
        specService.delete(specId);
        return Response.noContent().build();
    }

    @POST
    @Path("/{specId}/resources")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "addResource",
            summary = "Add a resource; its operations derive from the chosen capabilities "
                    + "per ADR-0010 (FEAT-005)")
    @APIResponse(responseCode = "201", description = "Created",
            content = @Content(schema = @Schema(implementation = ResourceResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation failed",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "404", description = "No such API",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "The name is already used in this API",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public Response addResource(@PathParam("specId") UUID specId,
            @Valid AddResourceRequest request) {
        ResourceView resource = specService.addResource(currentUser.require(), specId,
                request.name(), request.description(), request.capabilities());
        // 201 without a Location header, deliberately (RFC 9110 SHOULD): resources have no
        // addressable sub-endpoint yet, and a Location that 404s is worse than none.
        return Response.status(Response.Status.CREATED)
                .entity(ResourceResponse.from(resource)).build();
    }

    @POST
    @Path("/{specId}/resources/{schemaName}/fields")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "addField",
            summary = "Add a field to a resource's shape; the property name and constructs "
                    + "derive per ADR-0011 (FEAT-006)")
    @APIResponse(responseCode = "201", description = "Created",
            content = @Content(schema = @Schema(implementation = FieldResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation failed",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "404", description = "No such API or resource",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "The name is already used by a field of this shape",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public Response addField(@PathParam("specId") UUID specId,
            @PathParam("schemaName") String schemaName, @Valid FieldRequest request,
            @Context UriInfo uriInfo) {
        FieldView field = specService.addField(currentUser.require(), specId, schemaName,
                request.draft());
        // Unlike addResource's deliberate omission, fields are addressable — Location points
        // at the PATCH/DELETE endpoint below.
        URI location = uriInfo.getAbsolutePathBuilder().path(field.name()).build();
        return Response.created(location).entity(FieldResponse.from(field)).build();
    }

    @PATCH
    @Path("/{specId}/resources/{schemaName}/fields/{propertyName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "updateField",
            summary = "Rewrite a field in place — rename, retype, attributes, description as "
                    + "one atomic save (FEAT-006). A rename changes the field's identity: it "
                    + "is addressed by the new property name afterwards.")
    @APIResponse(responseCode = "200", description = "The updated field",
            content = @Content(schema = @Schema(implementation = FieldResponse.class)))
    @APIResponse(responseCode = "400", description = "Validation failed",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "404", description = "No such API, resource, or field",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409",
            description = "The name is already used by a field of this shape, or the field is id",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public FieldResponse updateField(@PathParam("specId") UUID specId,
            @PathParam("schemaName") String schemaName,
            @PathParam("propertyName") String propertyName, @Valid FieldRequest request) {
        return FieldResponse.from(specService.updateField(currentUser.require(), specId,
                schemaName, propertyName, request.draft()));
    }

    @DELETE
    @Path("/{specId}/resources/{schemaName}/fields/{propertyName}")
    @Operation(operationId = "removeField",
            summary = "Remove a field — the property and its required entry, nothing else "
                    + "(FEAT-006)")
    @APIResponse(responseCode = "204", description = "Removed")
    @APIResponse(responseCode = "404", description = "No such API, resource, or field",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "The field is id",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public Response removeField(@PathParam("specId") UUID specId,
            @PathParam("schemaName") String schemaName,
            @PathParam("propertyName") String propertyName) {
        specService.removeField(currentUser.require(), specId, schemaName, propertyName);
        return Response.noContent().build();
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
