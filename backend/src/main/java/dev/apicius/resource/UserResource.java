package dev.apicius.resource;

import dev.apicius.resource.dto.UserResponse;
import dev.apicius.security.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/users")
@Tag(name = "users")
@Authenticated
public class UserResource {

    @Inject
    CurrentUser currentUser;

    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getCurrentUser", summary = "Get the authenticated user's own identity")
    public UserResponse me() {
        return UserResponse.from(currentUser.require());
    }
}
