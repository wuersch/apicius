package dev.apicius.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.apicius.domain.AppUser;
import dev.apicius.repository.AppUserRepository;
import dev.apicius.service.UserProvisioningService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserResourceTest {

    @Inject
    AppUserRepository repository;

    @Inject
    UserProvisioningService provisioningService;

    @BeforeEach
    void cleanUsers() {
        QuarkusTransaction.requiringNew().run(repository::deleteAll);
    }

    // AC1: first authenticated request provisions an app_user from the token claims.
    @Test
    @TestSecurity(user = "sub-ada")
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "sub-ada"),
            @Claim(key = "name", value = "Ada Lovelace"),
            @Claim(key = "email", value = "ada@example.com")
    })
    void firstAuthenticatedRequestProvisionsAppUserFromClaims() {
        given()
                .when().get("/api/v1/users/me")
                .then()
                .statusCode(200)
                .body("displayName", equalTo("Ada Lovelace"))
                .body("email", equalTo("ada@example.com"));

        assertEquals(1, repository.count());
        AppUser user = repository.findByOidcSubject("sub-ada").orElseThrow();
        assertEquals("Ada Lovelace", user.displayName);
        assertEquals("ada@example.com", user.email);
    }

    // AC2: a returning sub reuses its row and refreshes display name / email — no duplicate.
    @Test
    @TestSecurity(user = "sub-ada")
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "sub-ada"),
            @Claim(key = "name", value = "Ada L. Lovelace"),
            @Claim(key = "email", value = "countess@example.com")
    })
    void returningSubjectReusesRowAndRefreshesClaims() {
        UUID existingId = QuarkusTransaction.requiringNew().call(
                () -> provisioningService.provision("sub-ada", "Ada Lovelace", "ada@example.com").id);

        given()
                .when().get("/api/v1/users/me")
                .then()
                .statusCode(200)
                .body("id", equalTo(existingId.toString()))
                .body("displayName", equalTo("Ada L. Lovelace"))
                .body("email", equalTo("countess@example.com"));

        assertEquals(1, repository.count());
    }

    // AC3: no valid token → 401, and no protected data is served nor any user provisioned.
    @Test
    void unauthenticatedRequestIsRejectedWithoutProvisioning() {
        given()
                .when().get("/api/v1/users/me")
                .then()
                .statusCode(401);

        assertEquals(0, repository.count());
    }
}
