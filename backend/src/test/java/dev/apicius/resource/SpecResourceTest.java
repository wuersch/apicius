package dev.apicius.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.apicius.domain.AppUser;
import dev.apicius.domain.LastEditedLocation;
import dev.apicius.domain.Spec;
import dev.apicius.service.UserProvisioningService;
import dev.apicius.test.AsAda;
import dev.apicius.test.CleanDatabaseTest;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SpecResourceTest extends CleanDatabaseTest {

    @Inject
    UserProvisioningService provisioningService;

    @Inject
    EntityManagerFactory entityManagerFactory;

    // AC2: every API appears as a card's worth of summary fields, alphabetical by title
    // (case-insensitive), regardless of owner — the list is workspace-global by design.
    @Test
    @AsAda
    void listSpecsReturnsAllApisAlphabeticallyByTitle() {
        seedSpec("sub-ada", "Ada Lovelace", "Storefront API", "Sell products online.", "1.0", 5, 21);
        seedSpec("sub-grace", "Grace Hopper", "billing API", "Invoices, payments & refunds.", "2.3", 4, 18);
        seedSpec("sub-ada", "Ada Lovelace", "Fleet API", "Vehicles, trips & drivers.", "0.4", 3, 12);

        given()
                .when().get("/api/v1/specs")
                .then()
                .statusCode(200)
                .body("total", equalTo(3))
                .body("items", hasSize(3))
                .body("items[0].title", equalTo("billing API"))
                .body("items[1].title", equalTo("Fleet API"))
                .body("items[2].title", equalTo("Storefront API"))
                .body("items[2].id", notNullValue())
                .body("items[2].description", equalTo("Sell products online."))
                .body("items[2].apiVersion", equalTo("1.0"))
                .body("items[2].resourceCount", equalTo(5))
                .body("items[2].operationCount", equalTo(21))
                .body("items[2].updatedAt", notNullValue());
    }

    // AC4: no APIs → an empty wrapped envelope, so the frontend can render the empty state.
    @Test
    @AsAda
    void listSpecsReturnsEmptyEnvelopeWhenNoApisExist() {
        given()
                .when().get("/api/v1/specs")
                .then()
                .statusCode(200)
                .body("total", equalTo(0))
                .body("items", hasSize(0));
    }

    // AC5: the list reads only the summary projection — the Spec entity (and its body JSONB)
    // is never hydrated. Hibernate statistics are the only black-box-proof of that: a buggy
    // fetch-then-map implementation would produce the same JSON but a non-zero load count.
    @Test
    @AsAda
    void listSpecsNeverHydratesSpecEntities() {
        UUID specId = seedSpec("sub-ada", "Ada Lovelace", "Storefront API", "Sell products online.",
                "1.0", 5, 21);
        setBody(specId, "{\"openapi\":\"3.1.0\",\"info\":{\"title\":\"Storefront API\"}}");
        Statistics statistics = statistics();
        statistics.clear();

        given()
                .when().get("/api/v1/specs")
                .then()
                .statusCode(200)
                .body("items", hasSize(1));

        assertEquals(0, statistics.getEntityStatistics(Spec.class.getName()).getLoadCount(),
                "the list must project columns, never load Spec entities");
    }

    // AC1: a recorded location with a capability names it, self-contained (no list lookup needed).
    @Test
    @AsAda
    void lastEditedNamesTheCapabilityWhenRecorded() {
        UUID specId = seedSpec("sub-ada", "Ada Lovelace", "Storefront API", "Sell products online.",
                "1.0", 5, 21);
        seedLocation("sub-ada", specId, "Add a product");

        given()
                .when().get("/api/v1/specs/last-edited")
                .then()
                .statusCode(200)
                .body("specId", equalTo(specId.toString()))
                .body("specTitle", equalTo("Storefront API"))
                .body("apiVersion", equalTo("1.0"))
                .body("capabilityName", equalTo("Add a product"))
                .body("lastEditedAt", notNullValue());
    }

    // AC1: no capability recorded → the card resolves to API-level (capabilityName null).
    @Test
    @AsAda
    void lastEditedResolvesToApiLevelWhenNoCapabilityRecorded() {
        UUID specId = seedSpec("sub-ada", "Ada Lovelace", "Storefront API", "Sell products online.",
                "1.0", 5, 21);
        seedLocation("sub-ada", specId, null);

        given()
                .when().get("/api/v1/specs/last-edited")
                .then()
                .statusCode(200)
                .body("specId", equalTo(specId.toString()))
                .body("capabilityName", nullValue());
    }

    // AC1: a designer who has never edited gets 204 — the home renders no jump-back-in card.
    @Test
    @AsAda
    void lastEditedReturns204WhenTheUserNeverEdited() {
        given()
                .when().get("/api/v1/specs/last-edited")
                .then()
                .statusCode(204);
    }

    // AC1: the pointer is per-user — another designer's location is not mine.
    @Test
    @AsAda
    void lastEditedIsScopedToTheCurrentUser() {
        UUID specId = seedSpec("sub-grace", "Grace Hopper", "Billing API", "Invoices.", "2.3", 4, 18);
        seedLocation("sub-grace", specId, "Refund a payment");

        given()
                .when().get("/api/v1/specs/last-edited")
                .then()
                .statusCode(204);
    }

    // AC5 (same guarantee on the second read path): resolving the pointer must not hydrate
    // Spec or LastEditedLocation entities either — it is a projection join.
    @Test
    @AsAda
    void lastEditedNeverHydratesEntities() {
        UUID specId = seedSpec("sub-ada", "Ada Lovelace", "Storefront API", "Sell products online.",
                "1.0", 5, 21);
        setBody(specId, "{\"openapi\":\"3.1.0\"}");
        seedLocation("sub-ada", specId, "Add a product");
        Statistics statistics = statistics();
        statistics.clear();

        given()
                .when().get("/api/v1/specs/last-edited")
                .then()
                .statusCode(200);

        assertEquals(0, statistics.getEntityStatistics(Spec.class.getName()).getLoadCount());
        assertEquals(0, statistics.getEntityStatistics(LastEditedLocation.class.getName()).getLoadCount());
    }

    // No valid token → 401 on both endpoints (matches the FEAT-001 posture: no anonymous surface).
    @Test
    void unauthenticatedRequestsAreRejected() {
        given().when().get("/api/v1/specs").then().statusCode(401);
        given().when().get("/api/v1/specs/last-edited").then().statusCode(401);
    }

    private UUID seedSpec(String oidcSubject, String displayName, String title, String description,
            String apiVersion, int resourceCount, int operationCount) {
        return QuarkusTransaction.requiringNew().call(() -> {
            AppUser owner = provisioningService.provision(oidcSubject, displayName, null);
            Spec spec = new Spec();
            spec.owner = owner;
            spec.title = title;
            spec.description = description;
            spec.apiVersion = apiVersion;
            spec.resourceCount = resourceCount;
            spec.operationCount = operationCount;
            specRepository.persist(spec);
            return spec.id;
        });
    }

    private void setBody(UUID specId, String body) {
        QuarkusTransaction.requiringNew().run(() -> {
            Spec spec = specRepository.findById(specId);
            spec.body = body;
        });
    }

    private void seedLocation(String oidcSubject, UUID specId, String capabilityName) {
        QuarkusTransaction.requiringNew().run(() -> {
            AppUser user = provisioningService.provision(oidcSubject, oidcSubject, null);
            LastEditedLocation location = new LastEditedLocation();
            location.user = user;
            location.spec = specRepository.findById(specId);
            location.capabilityName = capabilityName;
            lastEditedLocationRepository.persist(location);
        });
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }
}
