package dev.apicius.security;

import dev.apicius.service.UserProvisioningService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Ensures an {@code app_user} row exists for every authenticated request (ADR-0005): the
 * first request for a new {@code sub} provisions it from the token claims, later requests
 * refresh display name / email (FEAT-001 AC1/AC2). OIDC authentication itself happens at
 * the HTTP layer before any JAX-RS filter, so the identity is already resolved here.
 */
@ApplicationScoped
public class UserProvisioningFilter {

    @Inject
    SecurityIdentity identity;

    @Inject
    UserProvisioningService provisioningService;

    @Inject
    CurrentUser currentUser;

    // Runs on the worker thread because every resource method is blocking (plain return
    // types); if a reactive endpoint is ever added, this JDBC call must not run for it
    // on the event loop.
    @ServerRequestFilter(priority = Priorities.AUTHENTICATION)
    public void provisionUser() {
        if (identity.isAnonymous()) {
            return; // @Authenticated on the resources produces the 401
        }
        // OIDC is the only authentication mechanism, so the principal is always a JWT;
        // the type check guards test identities constructed without one.
        if (identity.getPrincipal() instanceof JsonWebToken jwt) {
            String email = jwt.getClaim("email");
            currentUser.set(provisioningService.provision(jwt.getSubject(), resolveDisplayName(jwt), email));
        }
    }

    /**
     * IdP-agnostic display-name fallback chain (AC5):
     * {@code name} → {@code given_name} + {@code family_name} → {@code preferred_username}
     * → {@code email} → {@code sub}.
     */
    static String resolveDisplayName(JsonWebToken jwt) {
        String name = nonBlank(jwt.getClaim("name"));
        if (name != null) {
            return name;
        }
        String given = nonBlank(jwt.getClaim("given_name"));
        String family = nonBlank(jwt.getClaim("family_name"));
        if (given != null || family != null) {
            return Stream.of(given, family).filter(Objects::nonNull).collect(Collectors.joining(" "));
        }
        String preferred = nonBlank(jwt.getClaim("preferred_username"));
        if (preferred != null) {
            return preferred;
        }
        String email = nonBlank(jwt.getClaim("email"));
        return email != null ? email : jwt.getSubject();
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
