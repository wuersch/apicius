package dev.apicius.security;

import dev.apicius.domain.AppUser;
import jakarta.enterprise.context.RequestScoped;

/**
 * The authenticated request's provisioned {@code app_user}, resolved once per request by
 * {@link UserProvisioningFilter} (ADR-0005).
 */
@RequestScoped
public class CurrentUser {

    private AppUser user;

    void set(AppUser user) {
        this.user = user;
    }

    public AppUser require() {
        if (user == null) {
            throw new IllegalStateException("no authenticated user in request scope — the provisioning filter did not run");
        }
        return user;
    }
}
