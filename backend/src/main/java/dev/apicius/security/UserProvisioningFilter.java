package dev.apicius.security;

import dev.apicius.service.UserProvisioningService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
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
    public void provisionUser(ContainerRequestContext requestContext) {
        if (identity.isAnonymous()) {
            return; // @Authenticated on the resources produces the 401
        }
        // OIDC bearer tokens are the only accepted credential, so the principal is a JWT.
        // Anything else (e.g. an opaque token) is unsupported — reject rather than 500 later.
        if (!(identity.getPrincipal() instanceof JsonWebToken jwt)) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }
        currentUser.set(provisioningService.provision(jwt.getSubject(), resolveDisplayName(jwt), verifiedEmail(jwt)));
    }

    /**
     * IdP-agnostic display-name fallback chain (AC5):
     * {@code name} → {@code given_name} + {@code family_name} → {@code preferred_username}
     * → {@code email} → {@code sub}.
     *
     * <p>Order = most-to-least human: {@code name} is the IdP's canonical display rendering;
     * given + family reconstruct it when absent; {@code preferred_username} is a login handle
     * (less human, so lower); {@code email} is a last-resort label; {@code sub} is opaque but
     * the only claim guaranteed present, so it anchors the chain and keeps the result non-null.
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

    /** The {@code email} claim, but only when the IdP asserts it verified — an unverified
     *  address is attacker-controllable and must not be stored as the user's email. */
    static String verifiedEmail(JsonWebToken jwt) {
        return isTrue(jwt.getClaim("email_verified")) ? nonBlank(jwt.getClaim("email")) : null;
    }

    // Accept a real JSON boolean or a stringified "true" (test-security stringifies claims).
    private static boolean isTrue(Object claim) {
        return Boolean.TRUE.equals(claim) || "true".equals(String.valueOf(claim));
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
