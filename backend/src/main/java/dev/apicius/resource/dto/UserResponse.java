package dev.apicius.resource.dto;

import dev.apicius.domain.AppUser;
import java.util.UUID;

public record UserResponse(UUID id, String displayName, String email) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.id, user.displayName, user.email);
    }
}
