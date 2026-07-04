package dev.apicius.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

// AC5's testable half: the display-name fallback chain handles any OIDC-compliant
// claim shape (Keycloak-rich, Entra-thin, bare) without IdP-specific branches.
class UserProvisioningFilterTest {

    @Test
    void prefersTheNameClaim() {
        JsonWebToken jwt = token(Map.of("sub", "s", "name", "Ada Lovelace", "given_name", "Ada"));
        assertEquals("Ada Lovelace", UserProvisioningFilter.resolveDisplayName(jwt));
    }

    @Test
    void assemblesGivenAndFamilyNameWhenNameIsMissing() {
        JsonWebToken jwt = token(Map.of("sub", "s", "given_name", "Grace", "family_name", "Hopper"));
        assertEquals("Grace Hopper", UserProvisioningFilter.resolveDisplayName(jwt));
    }

    @Test
    void usesASingleNamePartWhenOnlyOneIsPresent() {
        JsonWebToken jwt = token(Map.of("sub", "s", "family_name", "Hopper"));
        assertEquals("Hopper", UserProvisioningFilter.resolveDisplayName(jwt));
    }

    @Test
    void fallsBackToPreferredUsernameThenEmail() {
        assertEquals("gracehopper",
                UserProvisioningFilter.resolveDisplayName(token(Map.of("sub", "s", "preferred_username", "gracehopper"))));
        assertEquals("grace@example.com",
                UserProvisioningFilter.resolveDisplayName(token(Map.of("sub", "s", "email", "grace@example.com"))));
    }

    @Test
    void fallsBackToTheSubjectWhenNoNameClaimsExist() {
        JsonWebToken jwt = token(Map.of("sub", "sub-123"));
        assertEquals("sub-123", UserProvisioningFilter.resolveDisplayName(jwt));
    }

    @Test
    void treatsBlankClaimsAsAbsent() {
        JsonWebToken jwt = token(Map.of("sub", "s", "name", "  ", "preferred_username", "ada"));
        assertEquals("ada", UserProvisioningFilter.resolveDisplayName(jwt));
    }

    private static JsonWebToken token(Map<String, Object> claims) {
        return new JsonWebToken() {
            @Override
            public String getName() {
                return (String) claims.get("sub");
            }

            @Override
            public Set<String> getClaimNames() {
                return claims.keySet();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T getClaim(String claimName) {
                return (T) claims.get(claimName);
            }
        };
    }
}
