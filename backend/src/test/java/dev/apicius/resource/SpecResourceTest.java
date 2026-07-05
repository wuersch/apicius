package dev.apicius.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.apicius.domain.AppUser;
import dev.apicius.domain.LastEditedLocation;
import dev.apicius.domain.Spec;
import dev.apicius.service.SpecService;
import dev.apicius.service.UserProvisioningService;
import dev.apicius.test.AsAda;
import dev.apicius.test.CleanDatabaseTest;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
class SpecResourceTest extends CleanDatabaseTest {

    @Inject
    UserProvisioningService provisioningService;

    @Inject
    SpecService specService;

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
        given().contentType("application/json").body("{\"title\":\"Storefront API\"}")
                .when().post("/api/v1/specs").then().statusCode(401);
    }

    // ---- FEAT-003: create a new empty API ----

    // AC1: default version → persisted spec carries the title, seeded info.version 1.0.0, the
    // latest 3.1 patch as `openapi`, zeroed counts — and the response is the created summary.
    @Test
    @AsAda
    void createSpecWithDefaultVersionSeedsIdentityAndDocument() {
        String id = given()
                .contentType("application/json")
                .body("{\"title\":\"Storefront API\"}")
                .when().post("/api/v1/specs")
                .then()
                .statusCode(201)
                .header("Location", containsString("/api/v1/specs/"))
                .body("title", equalTo("Storefront API"))
                .body("description", nullValue())
                .body("apiVersion", equalTo("1.0.0"))
                .body("resourceCount", equalTo(0))
                .body("operationCount", equalTo(0))
                .body("updatedAt", notNullValue())
                .extract().path("id");

        var body = readBody(UUID.fromString(id));
        assertEquals("3.1.1", body.path("openapi").asText());
        assertEquals("Storefront API", body.path("info").path("title").asText());
        assertEquals("1.0.0", body.path("info").path("version").asText());
    }

    // AC2: a provided description lands in both the projection column and info.description.
    @Test
    @AsAda
    void createSpecWithDescriptionSetsDescription() {
        String id = given()
                .contentType("application/json")
                .body("{\"title\":\"Storefront API\",\"description\":\"Sell products online.\"}")
                .when().post("/api/v1/specs")
                .then()
                .statusCode(201)
                .body("description", equalTo("Sell products online."))
                .extract().path("id");

        assertEquals("Sell products online.",
                readBody(UUID.fromString(id)).path("info").path("description").asText());
    }

    // AC2: omitted (or blank) description → info.description is absent, not null/empty.
    @Test
    @AsAda
    void createSpecWithoutDescriptionOmitsDescriptionKey() {
        String id = given()
                .contentType("application/json")
                .body("{\"title\":\"Storefront API\",\"description\":\"  \"}")
                .when().post("/api/v1/specs")
                .then()
                .statusCode(201)
                .body("description", nullValue())
                .extract().path("id");

        assertFalse(readBody(UUID.fromString(id)).path("info").has("description"),
                "info.description must be absent when not provided (AC2)");
    }

    // AC3: a chosen minor pins the latest patch of that minor; immutability is by omission —
    // no endpoint mutates `openapi` (v1 recourse: recreate).
    @ParameterizedTest
    @CsvSource({"3.0, 3.0.4", "3.2, 3.2.0"})
    @AsAda
    void createSpecHonorsTheChosenSpecVersion(String minor, String expectedPatch) {
        String id = given()
                .contentType("application/json")
                .body("{\"title\":\"Fleet API\",\"specVersion\":\"" + minor + "\"}")
                .when().post("/api/v1/specs")
                .then()
                .statusCode(201)
                .extract().path("id");

        assertEquals(expectedPatch, readBody(UUID.fromString(id)).path("openapi").asText());
    }

    // AC4: titles need not be unique — APIs are keyed by UUID.
    @Test
    @AsAda
    void createSpecAllowsDuplicateTitles() {
        String first = given().contentType("application/json").body("{\"title\":\"Storefront API\"}")
                .when().post("/api/v1/specs").then().statusCode(201).extract().path("id");
        String second = given().contentType("application/json").body("{\"title\":\"Storefront API\"}")
                .when().post("/api/v1/specs").then().statusCode(201).extract().path("id");

        assertNotEquals(first, second);
        assertEquals(2, specRepository.count());
    }

    // AC5: blank title → RFC 9457 problem+json naming the field; nothing persisted.
    @Test
    @AsAda
    void createSpecRejectsBlankTitleWithProblemDetail() {
        given()
                .contentType("application/json")
                .body("{\"title\":\"   \"}")
                .when().post("/api/v1/specs")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("title", equalTo("Validation failed"))
                .body("status", equalTo(400))
                .body("violations[0].field", equalTo("title"))
                .body("violations[0].message", notNullValue());

        assertEquals(0, specRepository.count(), "nothing may be persisted on a rejected create (AC5)");
    }

    // AC5 (same contract): the title field missing entirely.
    @Test
    @AsAda
    void createSpecRejectsMissingTitle() {
        given()
                .contentType("application/json")
                .body("{}")
                .when().post("/api/v1/specs")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("violations[0].field", equalTo("title"));

        assertEquals(0, specRepository.count());
    }

    // Defensive edge of AC3: an unsupported version is rejected through the same problem contract.
    @Test
    @AsAda
    void createSpecRejectsUnsupportedSpecVersion() {
        given()
                .contentType("application/json")
                .body("{\"title\":\"Fleet API\",\"specVersion\":\"2.0\"}")
                .when().post("/api/v1/specs")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("violations[0].field", equalTo("specVersion"));

        assertEquals(0, specRepository.count());
    }

    // Creating counts as editing: the creator's jump-back-in pointer lands on the new API,
    // at API level (no capability yet).
    @Test
    @AsAda
    void createSpecRecordsTheCreatorsLastEditedLocation() {
        String id = given().contentType("application/json").body("{\"title\":\"Storefront API\"}")
                .when().post("/api/v1/specs").then().statusCode(201).extract().path("id");

        given()
                .when().get("/api/v1/specs/last-edited")
                .then()
                .statusCode(200)
                .body("specId", equalTo(id))
                .body("specTitle", equalTo("Storefront API"))
                .body("capabilityName", nullValue());
    }

    // The pointer upsert must be atomic: concurrent creates by the same user race the
    // uq_last_edited_location_user_id constraint — a read-then-write upsert makes the loser's
    // whole transaction (including its new Spec) roll back.
    @Test
    void concurrentCreatesByTheSameUserAllSucceed() throws Exception {
        AppUser ada = QuarkusTransaction.requiringNew()
                .call(() -> provisioningService.provision("sub-ada", "Ada Lovelace", null));
        int creates = 4;
        ExecutorService executor = Executors.newFixedThreadPool(creates);
        CyclicBarrier barrier = new CyclicBarrier(creates);
        try {
            List<Future<Spec>> futures = IntStream.range(0, creates)
                    .mapToObj(i -> executor.submit(() -> {
                        barrier.await();
                        return specService.createEmpty(ada, "Race API", null, null);
                    }))
                    .toList();
            for (Future<Spec> future : futures) {
                assertNotNull(future.get().id, "every concurrent create must commit");
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(creates, specRepository.count());
        assertEquals(1, lastEditedLocationRepository.count(), "still one pointer row per user");
    }

    // The pointer is an upsert against the single per-user row, not an accumulating history.
    @Test
    @AsAda
    void creatingASecondApiMovesTheLastEditedLocation() {
        given().contentType("application/json").body("{\"title\":\"Storefront API\"}")
                .when().post("/api/v1/specs").then().statusCode(201);
        String second = given().contentType("application/json").body("{\"title\":\"Fleet API\"}")
                .when().post("/api/v1/specs").then().statusCode(201).extract().path("id");

        given()
                .when().get("/api/v1/specs/last-edited")
                .then()
                .statusCode(200)
                .body("specId", equalTo(second));
        assertEquals(1, lastEditedLocationRepository.count(), "one row per user (upsert, not insert)");
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

    /** The persisted document, parsed — create-path assertions read what actually hit the column. */
    private JsonNode readBody(UUID specId) {
        String body = QuarkusTransaction.requiringNew()
                .call(() -> specRepository.findById(specId).body);
        try {
            return new ObjectMapper().readTree(body);
        } catch (Exception e) {
            throw new AssertionError("spec.body is not valid JSON", e);
        }
    }
}
