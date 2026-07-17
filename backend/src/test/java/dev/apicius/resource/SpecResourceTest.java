package dev.apicius.resource;

import static dev.apicius.test.JsonAssertions.assertNoVendorExtensions;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
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
import java.util.ArrayList;
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

    /** Exactness-preserving readers for the FEAT-008 AC1 equivalence checks. */
    private static final ObjectMapper EXACT_JSON = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
            .build();

    private static final ObjectMapper EXACT_YAML = YAMLMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
            .build();

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

    // ---- FEAT-005: create a resource ----

    private static final String ALL_CAPABILITIES =
            "[\"BROWSE\",\"LOOK_UP\",\"ADD\",\"UPDATE\",\"REMOVE\"]";

    // AC1: all five capabilities → the response names them in plain language, and the persisted
    // document carries the ADR-0010 constructs (the engine test owns the exhaustive assertions).
    @Test
    @AsAda
    void addResourceWithAllCapabilitiesDerivesSchemaAndPaths() {
        UUID specId = createApi("Storefront API");

        postResource(specId, "{\"name\":\"Product\",\"description\":\"Something you sell.\","
                + "\"capabilities\":" + ALL_CAPABILITIES + "}")
                .statusCode(201)
                .body("name", equalTo("Product"))
                .body("description", equalTo("Something you sell."))
                .body("capabilities", hasSize(5))
                .body("capabilities[0].capability", equalTo("BROWSE"))
                .body("capabilities[0].label", equalTo("Browse all products"))
                .body("capabilities[0].method", equalTo("GET"))
                .body("capabilities[0].path", equalTo("/products"))
                .body("capabilities[3].label", equalTo("Update a product"))
                .body("capabilities[3].method", equalTo("PATCH"))
                .body("capabilities[3].path", equalTo("/products/{id}"));

        JsonNode body = readBody(specId);
        JsonNode schema = body.path("components").path("schemas").path("Product");
        assertEquals("Something you sell.", schema.path("description").asText());
        assertEquals("string", schema.path("properties").path("id").path("type").asText());
        assertTrue(schema.path("properties").path("id").path("readOnly").asBoolean());
        assertEquals("Browse all products",
                body.path("paths").path("/products").path("get").path("summary").asText());
        assertTrue(body.path("paths").path("/products/{id}").path("patch").path("requestBody")
                .path("content").has("application/merge-patch+json"));
        assertEquals("#/components/schemas/Product",
                body.path("paths").path("/products").path("get").path("responses").path("200")
                        .path("content").path("application/json").path("schema")
                        .path("properties").path("data").path("items").path("$ref").asText());
    }

    // AC2: the projection columns and the jump-back-in pointer move in the same transaction
    // as the document mutation.
    @Test
    @AsAda
    void addResourceUpdatesProjectionCountsAndPointer() {
        UUID specId = createApi("Storefront API");

        postResource(specId, "{\"name\":\"Product\",\"capabilities\":" + ALL_CAPABILITIES + "}")
                .statusCode(201);

        given()
                .when().get("/api/v1/specs/{specId}", specId)
                .then()
                .statusCode(200)
                .body("resourceCount", equalTo(1))
                .body("operationCount", equalTo(5));
        given()
                .when().get("/api/v1/specs/last-edited")
                .then()
                .statusCode(200)
                .body("specId", equalTo(specId.toString()))
                .body("capabilityName", nullValue());
    }

    // AC3: the persisted document contains nothing Apicius-specific — no x- keys anywhere.
    @Test
    @AsAda
    void addResourcePersistsNothingApiciusSpecific() {
        UUID specId = createApi("Storefront API");

        postResource(specId, "{\"name\":\"Product\",\"capabilities\":" + ALL_CAPABILITIES + "}")
                .statusCode(201);

        assertNoVendorExtensions(readBody(specId));
    }

    // AC4: a subset derives exactly the chosen operations; the unused path item is absent.
    @Test
    @AsAda
    void addResourceWithSubsetOmitsUnchosenOperationsAndPaths() {
        UUID specId = createApi("Storefront API");

        postResource(specId, "{\"name\":\"Product\",\"capabilities\":[\"BROWSE\",\"ADD\"]}")
                .statusCode(201)
                .body("capabilities", hasSize(2));

        JsonNode body = readBody(specId);
        assertTrue(body.path("paths").has("/products"));
        assertFalse(body.path("paths").has("/products/{id}"),
                "a path item with no chosen operation must not exist at all (AC4)");
        given()
                .when().get("/api/v1/specs/{specId}", specId)
                .then()
                .body("operationCount", equalTo(2));
    }

    // AC5: blank / missing name → problem+json naming the field; nothing persisted.
    @Test
    @AsAda
    void addResourceRejectsBlankName() {
        UUID specId = createApi("Storefront API");

        postResource(specId, "{\"name\":\"   \",\"capabilities\":" + ALL_CAPABILITIES + "}")
                .statusCode(400)
                .contentType("application/problem+json")
                .body("violations.field", org.hamcrest.Matchers.hasItem("name"));
        postResource(specId, "{\"capabilities\":" + ALL_CAPABILITIES + "}")
                .statusCode(400)
                .contentType("application/problem+json")
                .body("violations[0].field", equalTo("name"));

        assertUntouched(specId);
    }

    // AC5's edge: a name that can't derive cleanly (pattern) is rejected the same way.
    @Test
    @AsAda
    void addResourceRejectsUnderivableName() {
        UUID specId = createApi("Storefront API");

        postResource(specId, "{\"name\":\"1product\",\"capabilities\":" + ALL_CAPABILITIES + "}")
                .statusCode(400)
                .contentType("application/problem+json")
                .body("violations[0].field", equalTo("name"));

        assertUntouched(specId);
    }

    // AC6: a name already used in this API — case-insensitively, both derive the same schema —
    // is a 409 conflict naming the problem; nothing persisted.
    @Test
    @AsAda
    void addResourceRejectsDuplicateNameWithConflict() {
        UUID specId = createApi("Storefront API");
        postResource(specId, "{\"name\":\"Product\",\"capabilities\":[\"ADD\"]}").statusCode(201);

        postResource(specId, "{\"name\":\"product\",\"capabilities\":" + ALL_CAPABILITIES + "}")
                .statusCode(409)
                .contentType("application/problem+json")
                .body("title", equalTo("Name conflict"))
                .body("status", equalTo(409))
                .body("detail", containsString("product"))
                .body("violations[0].field", equalTo("name"));

        given()
                .when().get("/api/v1/specs/{specId}", specId)
                .then()
                .body("resourceCount", equalTo(1))
                .body("operationCount", equalTo(1));
    }

    // AC6's structural twin: different names colliding on the derived path (Person and People
    // both pluralize to /people) conflict too — one path can't serve two resources.
    @Test
    @AsAda
    void addResourceRejectsCollidingDerivedPaths() {
        UUID specId = createApi("Storefront API");
        postResource(specId, "{\"name\":\"Person\",\"capabilities\":[\"ADD\"]}").statusCode(201);

        postResource(specId, "{\"name\":\"People\",\"capabilities\":[\"ADD\"]}")
                .statusCode(409)
                .contentType("application/problem+json")
                .body("detail", containsString("/people"));
    }

    // AC7: no capability selected → rejected with the at-least-one rule; nothing persisted.
    @Test
    @AsAda
    void addResourceRejectsEmptyCapabilities() {
        UUID specId = createApi("Storefront API");

        postResource(specId, "{\"name\":\"Product\",\"capabilities\":[]}")
                .statusCode(400)
                .contentType("application/problem+json")
                .body("violations[0].field", equalTo("capabilities"));

        assertUntouched(specId);
    }

    // Duplicate capability values are deselection-proofing, not extra operations.
    @Test
    @AsAda
    void addResourceIgnoresDuplicateCapabilities() {
        UUID specId = createApi("Storefront API");

        postResource(specId, "{\"name\":\"Product\",\"capabilities\":[\"ADD\",\"ADD\"]}")
                .statusCode(201)
                .body("capabilities", hasSize(1));

        given()
                .when().get("/api/v1/specs/{specId}", specId)
                .then()
                .body("operationCount", equalTo(1));
    }

    // Unknown API → 404 problem+json on both new endpoints.
    @Test
    @AsAda
    void resourceEndpointsReturn404ForUnknownApi() {
        UUID unknown = UUID.randomUUID();

        given()
                .when().get("/api/v1/specs/{specId}", unknown)
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("title", equalTo("Not found"));
        given()
                .contentType("application/json")
                .body("{\"name\":\"Product\",\"capabilities\":[\"ADD\"]}")
                .when().post("/api/v1/specs/{specId}/resources", unknown)
                .then()
                .statusCode(404)
                .contentType("application/problem+json");
    }

    // Same 401 posture as every other endpoint (FEAT-001).
    @Test
    void unauthenticatedResourceEndpointsAreRejected() {
        UUID any = UUID.randomUUID();
        given().when().get("/api/v1/specs/" + any).then().statusCode(401);
        given().contentType("application/json")
                .body("{\"name\":\"Product\",\"capabilities\":[\"ADD\"]}")
                .when().post("/api/v1/specs/" + any + "/resources").then().statusCode(401);
    }

    // AC8, empty half: an API with no resources says so through an empty projection.
    @Test
    @AsAda
    void getSpecReturnsTheEmptyApiPlainly() {
        UUID specId = createApi("Storefront API");

        given()
                .when().get("/api/v1/specs/{specId}", specId)
                .then()
                .statusCode(200)
                .body("id", equalTo(specId.toString()))
                .body("title", equalTo("Storefront API"))
                .body("apiVersion", equalTo("1.0.0"))
                .body("resourceCount", equalTo(0))
                .body("resources", hasSize(0));
    }

    // AC8, populated half: created resources come back with their capabilities in plain
    // language — projected from the document, not echoed from the request.
    @Test
    @AsAda
    void getSpecProjectsCreatedResourcesWithCapabilities() {
        UUID specId = createApi("Storefront API");
        postResource(specId, "{\"name\":\"Order item\",\"description\":\"A line of an order.\","
                + "\"capabilities\":[\"BROWSE\",\"REMOVE\"]}").statusCode(201);

        given()
                .when().get("/api/v1/specs/{specId}", specId)
                .then()
                .statusCode(200)
                .body("resources", hasSize(1))
                .body("resources[0].name", equalTo("OrderItem"))
                .body("resources[0].description", equalTo("A line of an order."))
                .body("resources[0].capabilities", hasSize(2))
                .body("resources[0].capabilities[0].label", equalTo("Browse all order items"))
                .body("resources[0].capabilities[0].method", equalTo("GET"))
                .body("resources[0].capabilities[0].path", equalTo("/order-items"))
                .body("resources[0].capabilities[1].label", equalTo("Remove an order item"))
                .body("resources[0].capabilities[1].path", equalTo("/order-items/{id}"));
    }

    // Document mutations serialize on the spec row (pessimistic lock): concurrent adds of
    // different resources to the same API must both commit, not 500 on optimistic conflicts.
    @Test
    void concurrentResourceAddsToTheSameApiAllSucceed() throws Exception {
        AppUser ada = QuarkusTransaction.requiringNew()
                .call(() -> provisioningService.provision("sub-ada", "Ada Lovelace", null));
        UUID specId = QuarkusTransaction.requiringNew()
                .call(() -> specService.createEmpty(ada, "Race API", null, null).id);
        List<String> names = List.of("Product", "Review");
        ExecutorService executor = Executors.newFixedThreadPool(names.size());
        CyclicBarrier barrier = new CyclicBarrier(names.size());
        try {
            List<Future<?>> futures = names.stream()
                    .<Future<?>>map(name -> executor.submit(() -> {
                        barrier.await();
                        return QuarkusTransaction.requiringNew().call(() ->
                                specService.addResource(ada, specId, name, null,
                                        List.of(dev.apicius.document.derivation.Capability.values())));
                    }))
                    .toList();
            for (Future<?> future : futures) {
                assertNotNull(future.get(), "every concurrent add must commit");
            }
        } finally {
            executor.shutdownNow();
        }

        JsonNode body = readBody(specId);
        assertEquals(3, body.path("components").path("schemas").size(),
                "the two resources plus the FEAT-009 Error furniture");
        QuarkusTransaction.requiringNew().run(() -> {
            Spec spec = specRepository.findById(specId);
            assertEquals(2, spec.resourceCount);
            assertEquals(10, spec.operationCount);
        });
    }

    // ---- FEAT-006: edit a resource's shape ----

    // AC1: the response is the derivation, the persisted schema carries exactly the ADR-0011
    // constructs (the engine test owns the exhaustive table), and fields are addressable —
    // Location points at the new property.
    @Test
    @AsAda
    void addFieldDerivesThePropertyAndPersistsIt() {
        UUID specId = productApi();

        postField(specId, "Product", "{\"name\":\"First name\",\"coreType\":\"TEXT\","
                + "\"required\":true,\"description\":\"The customer's given name.\"}")
                .statusCode(201)
                .header("Location", endsWith("/specs/" + specId + "/resources/Product/fields/firstName"))
                .body("name", equalTo("firstName"))
                .body("coreType", equalTo("TEXT"))
                .body("refinement", nullValue())
                .body("list", equalTo(false))
                .body("required", equalTo(true))
                .body("visibility", equalTo("NORMAL"))
                .body("description", equalTo("The customer's given name."));

        JsonNode schema = readBody(specId).path("components").path("schemas").path("Product");
        assertEquals("string", schema.path("properties").path("firstName").path("type").asText());
        assertEquals("The customer's given name.",
                schema.path("properties").path("firstName").path("description").asText());
        assertTrue(streamOf(schema.path("required")).contains("firstName"));
    }

    // AC2: a field edit changes the schema and nothing else — the ADR-0008 counts stay put;
    // the jump-back-in pointer moves in the same transaction.
    @Test
    @AsAda
    void addFieldLeavesCountsUntouchedAndMovesThePointer() {
        UUID specId = productApi();

        postField(specId, "Product", "{\"name\":\"Price\",\"coreType\":\"DECIMAL_NUMBER\"}")
                .statusCode(201);

        given()
                .when().get("/api/v1/specs/{specId}", specId)
                .then()
                .body("resourceCount", equalTo(1))
                .body("operationCount", equalTo(5));
        given()
                .when().get("/api/v1/specs/last-edited")
                .then()
                .statusCode(200)
                .body("specId", equalTo(specId.toString()))
                .body("capabilityName", nullValue());
    }

    // AC3: nothing Apicius-specific in the document after any field edit.
    @Test
    @AsAda
    void fieldEditsPersistNothingApiciusSpecific() {
        UUID specId = productApi();

        postField(specId, "Product", "{\"name\":\"Tags\",\"coreType\":\"TEXT\",\"list\":true}")
                .statusCode(201);
        patchField(specId, "Product", "tags", "{\"name\":\"Labels\",\"coreType\":\"TEXT\","
                + "\"list\":true}").statusCode(200);

        assertNoVendorExtensions(readBody(specId));
    }

    // AC4: refinement and list serialize per the table — format on the type, array + items.
    @Test
    @AsAda
    void addFieldSerializesRefinementAndListPerTheTable() {
        UUID specId = productApi();

        postField(specId, "Product", "{\"name\":\"Related products\",\"coreType\":\"TEXT\","
                + "\"refinement\":\"UUID\",\"list\":true}")
                .statusCode(201)
                .body("name", equalTo("relatedProducts"))
                .body("list", equalTo(true));

        JsonNode property = readBody(specId).path("components").path("schemas").path("Product")
                .path("properties").path("relatedProducts");
        assertEquals("array", property.path("type").asText());
        assertEquals("string", property.path("items").path("type").asText());
        assertEquals("uuid", property.path("items").path("format").asText());
    }

    // AC5: Text as password defaults to write-only (the house rule, applied server-side so it
    // holds for any client); an explicit override persists format: password without writeOnly.
    @Test
    @AsAda
    void passwordDefaultsToWriteOnlyAndHonorsTheOverride() {
        UUID specId = productApi();

        postField(specId, "Product", "{\"name\":\"Password\",\"coreType\":\"TEXT\","
                + "\"refinement\":\"PASSWORD\"}")
                .statusCode(201)
                .body("visibility", equalTo("WRITE_ONLY"));
        JsonNode password = readBody(specId).path("components").path("schemas").path("Product")
                .path("properties").path("password");
        assertEquals("password", password.path("format").asText());
        assertTrue(password.path("writeOnly").asBoolean());

        patchField(specId, "Product", "password", "{\"name\":\"Password\",\"coreType\":\"TEXT\","
                + "\"refinement\":\"PASSWORD\",\"visibility\":\"NORMAL\"}")
                .statusCode(200)
                .body("visibility", equalTo("NORMAL"));
        JsonNode overridden = readBody(specId).path("components").path("schemas").path("Product")
                .path("properties").path("password");
        assertEquals("password", overridden.path("format").asText());
        assertFalse(overridden.has("writeOnly"));
    }

    // AC6: one atomic save rewrites the property in place — position kept, required following,
    // nothing else changed. A rename changes the field's address (the derived name is the
    // identity); the old property name is gone.
    @Test
    @AsAda
    void updateFieldRewritesThePropertyInPlace() {
        UUID specId = productApi();
        postField(specId, "Product", "{\"name\":\"Price\",\"coreType\":\"WHOLE_NUMBER\","
                + "\"required\":true}").statusCode(201);
        postField(specId, "Product", "{\"name\":\"In stock\",\"coreType\":\"YES_NO\"}")
                .statusCode(201);

        patchField(specId, "Product", "price", "{\"name\":\"Unit price\","
                + "\"coreType\":\"DECIMAL_NUMBER\",\"required\":true}")
                .statusCode(200)
                .body("name", equalTo("unitPrice"))
                .body("coreType", equalTo("DECIMAL_NUMBER"));

        JsonNode schema = readBody(specId).path("components").path("schemas").path("Product");
        assertEquals(List.of("id", "unitPrice", "inStock"),
                fieldNamesOf(schema.path("properties")));
        assertEquals("number", schema.path("properties").path("unitPrice").path("type").asText());
        assertEquals(List.of("id", "unitPrice"), streamOf(schema.path("required")));
    }

    // AC6's identity edge: a rename that only re-cases the same field is legal — the field is
    // exempt from colliding with itself.
    @Test
    @AsAda
    void updateFieldAllowsRenamingAFieldToItself() {
        UUID specId = productApi();
        postField(specId, "Product", "{\"name\":\"First name\",\"coreType\":\"TEXT\"}")
                .statusCode(201);

        patchField(specId, "Product", "firstName", "{\"name\":\"First name\","
                + "\"coreType\":\"TEXT\",\"required\":true}")
                .statusCode(200)
                .body("name", equalTo("firstName"))
                .body("required", equalTo(true));
    }

    // AC7: no mutation can target id — 409, document untouched.
    @Test
    @AsAda
    void identityFieldCannotBeChangedOrRemoved() {
        UUID specId = productApi();

        patchField(specId, "Product", "id", "{\"name\":\"Identifier\",\"coreType\":\"TEXT\"}")
                .statusCode(409)
                .contentType("application/problem+json")
                .body("title", equalTo("Field not editable"));
        deleteField(specId, "Product", "id")
                .statusCode(409)
                .contentType("application/problem+json")
                .body("title", equalTo("Field not editable"));

        JsonNode schema = readBody(specId).path("components").path("schemas").path("Product");
        assertTrue(schema.path("properties").has("id"));
        assertTrue(schema.path("properties").path("id").path("readOnly").asBoolean());
    }

    // AC8: removal drops the property and its required entry, nothing else; the shape falls
    // back to id alone and stays valid.
    @Test
    @AsAda
    void removeFieldDropsThePropertyAndItsRequiredEntry() {
        UUID specId = productApi();
        postField(specId, "Product", "{\"name\":\"Name\",\"coreType\":\"TEXT\","
                + "\"required\":true}").statusCode(201);

        deleteField(specId, "Product", "name").statusCode(204);

        JsonNode schema = readBody(specId).path("components").path("schemas").path("Product");
        assertEquals(List.of("id"), fieldNamesOf(schema.path("properties")));
        assertEquals(List.of("id"), streamOf(schema.path("required")));
        given()
                .when().get("/api/v1/specs/{specId}", specId)
                .then()
                .body("resources[0].fields", hasSize(1));
    }

    // AC9, first half: a name that derives to nothing is a 400 naming the field; nothing
    // persisted.
    @Test
    @AsAda
    void addFieldRejectsANameThatDerivesToNothing() {
        UUID specId = productApi();

        postField(specId, "Product", "{\"name\":\"!!!\",\"coreType\":\"TEXT\"}")
                .statusCode(400)
                .contentType("application/problem+json")
                .body("violations[0].field", equalTo("name"));

        assertProductFieldsUntouched(specId);
    }

    // AC9, second half: case-insensitive collision with any field of this shape — id included —
    // is a 409; nothing persisted.
    @Test
    @AsAda
    void addFieldRejectsACollidingName() {
        UUID specId = productApi();
        postField(specId, "Product", "{\"name\":\"First name\",\"coreType\":\"TEXT\"}")
                .statusCode(201);

        postField(specId, "Product", "{\"name\":\"FIRST NAME\",\"coreType\":\"TEXT\"}")
                .statusCode(409)
                .contentType("application/problem+json")
                .body("title", equalTo("Name conflict"))
                .body("violations[0].field", equalTo("name"));
        postField(specId, "Product", "{\"name\":\"Id\",\"coreType\":\"TEXT\"}")
                .statusCode(409)
                .contentType("application/problem+json")
                .body("detail", containsString("id"));

        JsonNode schema = readBody(specId).path("components").path("schemas").path("Product");
        assertEquals(List.of("id", "firstName"), fieldNamesOf(schema.path("properties")));
    }

    // The kind contract must hold for any client: a refinement outside its core type's row is
    // a 400 (our UI cannot even construct one).
    @Test
    @AsAda
    void addFieldRejectsAnIncompatibleRefinement() {
        UUID specId = productApi();

        postField(specId, "Product", "{\"name\":\"Count\",\"coreType\":\"WHOLE_NUMBER\","
                + "\"refinement\":\"EMAIL\"}")
                .statusCode(400)
                .contentType("application/problem+json")
                .body("violations[0].field", equalTo("refinement"));

        assertProductFieldsUntouched(specId);
    }

    // Bean validation guards the request shape itself.
    @Test
    @AsAda
    void addFieldRejectsBlankNameAndMissingCoreType() {
        UUID specId = productApi();

        postField(specId, "Product", "{\"name\":\"   \",\"coreType\":\"TEXT\"}")
                .statusCode(400)
                .contentType("application/problem+json")
                .body("violations[0].field", equalTo("name"));
        postField(specId, "Product", "{\"name\":\"Price\"}")
                .statusCode(400)
                .contentType("application/problem+json")
                .body("violations[0].field", equalTo("coreType"));

        assertProductFieldsUntouched(specId);
    }

    // Unknown spec, resource, or field → 404 problem+json on every field endpoint.
    @Test
    @AsAda
    void fieldEndpointsReturn404ForUnknownTargets() {
        UUID specId = productApi();

        postField(UUID.randomUUID(), "Product", "{\"name\":\"Price\",\"coreType\":\"TEXT\"}")
                .statusCode(404)
                .contentType("application/problem+json");
        postField(specId, "Order", "{\"name\":\"Price\",\"coreType\":\"TEXT\"}")
                .statusCode(404)
                .contentType("application/problem+json")
                .body("detail", containsString("Order"));
        patchField(specId, "Product", "nope", "{\"name\":\"Price\",\"coreType\":\"TEXT\"}")
                .statusCode(404)
                .contentType("application/problem+json")
                .body("detail", containsString("nope"));
        deleteField(specId, "Product", "nope")
                .statusCode(404)
                .contentType("application/problem+json");
    }

    // Same 401 posture as every other endpoint (FEAT-001).
    @Test
    void unauthenticatedFieldEndpointsAreRejected() {
        String base = "/api/v1/specs/" + UUID.randomUUID() + "/resources/Product/fields";
        given().contentType("application/json").body("{\"name\":\"Price\",\"coreType\":\"TEXT\"}")
                .when().post(base).then().statusCode(401);
        given().contentType("application/json").body("{\"name\":\"Price\",\"coreType\":\"TEXT\"}")
                .when().patch(base + "/price").then().statusCode(401);
        given().when().delete(base + "/price").then().statusCode(401);
    }

    // AC11, backend half: the projection lists every field with its plain-language kind and
    // attributes, id first as an ordinary auto-visibility field.
    @Test
    @AsAda
    void getSpecProjectsFieldsInDocumentOrder() {
        UUID specId = productApi();
        postField(specId, "Product", "{\"name\":\"Name\",\"coreType\":\"TEXT\","
                + "\"required\":true}").statusCode(201);
        postField(specId, "Product", "{\"name\":\"Contact\",\"coreType\":\"TEXT\","
                + "\"refinement\":\"EMAIL\",\"description\":\"How to reach the seller.\"}")
                .statusCode(201);

        given()
                .when().get("/api/v1/specs/{specId}", specId)
                .then()
                .statusCode(200)
                .body("resources[0].fields", hasSize(3))
                .body("resources[0].fields[0].name", equalTo("id"))
                .body("resources[0].fields[0].coreType", equalTo("TEXT"))
                .body("resources[0].fields[0].required", equalTo(true))
                .body("resources[0].fields[0].visibility", equalTo("AUTO"))
                .body("resources[0].fields[1].name", equalTo("name"))
                .body("resources[0].fields[1].required", equalTo(true))
                .body("resources[0].fields[2].name", equalTo("contact"))
                .body("resources[0].fields[2].refinement", equalTo("EMAIL"))
                .body("resources[0].fields[2].description", equalTo("How to reach the seller."));
    }

    // ---- FEAT-007: manage an API ----

    // AC1: one save rewrites info.title / info.description / info.version and the ADR-0008
    // projection; every other document node is untouched.
    @Test
    @AsAda
    void updateDetailsRewritesInfoAndProjection() {
        UUID specId = productApi();
        JsonNode before = readBody(specId);

        patchDetails(specId, "{\"title\":\"Storefront API v2\","
                + "\"description\":\"Everything for the shop.\",\"version\":\"2.0.0\"}")
                .statusCode(200)
                .body("id", equalTo(specId.toString()))
                .body("title", equalTo("Storefront API v2"))
                .body("description", equalTo("Everything for the shop."))
                .body("apiVersion", equalTo("2.0.0"))
                .body("resourceCount", equalTo(1))
                .body("operationCount", equalTo(5));

        JsonNode after = readBody(specId);
        assertEquals("Storefront API v2", after.path("info").path("title").asText());
        assertEquals("Everything for the shop.", after.path("info").path("description").asText());
        assertEquals("2.0.0", after.path("info").path("version").asText());
        assertEquals(before.path("openapi"), after.path("openapi"));
        assertEquals(before.path("paths"), after.path("paths"));
        assertEquals(before.path("components"), after.path("components"));
        given()
                .when().get("/api/v1/specs")
                .then()
                .body("items[0].title", equalTo("Storefront API v2"))
                .body("items[0].apiVersion", equalTo("2.0.0"));
    }

    // AC1: description is removable — blank means "not provided": the member leaves the
    // document, the projection column goes null.
    @Test
    @AsAda
    void updateDetailsWithBlankDescriptionRemovesIt() {
        String id = given().contentType("application/json")
                .body("{\"title\":\"Storefront API\",\"description\":\"Sell products online.\"}")
                .when().post("/api/v1/specs").then().statusCode(201).extract().path("id");
        UUID specId = UUID.fromString(id);

        patchDetails(specId,
                "{\"title\":\"Storefront API\",\"description\":\"   \",\"version\":\"1.0.0\"}")
                .statusCode(200)
                .body("description", nullValue());

        assertFalse(readBody(specId).path("info").has("description"),
                "info.description must be removed when blanked (AC1)");
    }

    // AC1 × PRIN-003: unmodeled info members (contact, extensions) and unknown root nodes
    // survive a details save untouched — the save rewrites info's three fields, not info.
    @Test
    @AsAda
    void updateDetailsPreservesUnmodeledContent() {
        UUID specId = seedSpec("sub-ada", "Ada Lovelace", "Payments API", "Charges.", "2.3.0", 0, 0);
        setBody(specId, "{\"openapi\":\"3.1.1\",\"x-internal\":true,"
                + "\"info\":{\"title\":\"Payments API\",\"version\":\"2.3.0\","
                + "\"contact\":{\"name\":\"Platform team\"},\"x-audience\":\"partners\"},"
                + "\"paths\":{}}");

        patchDetails(specId, "{\"title\":\"Payments API\",\"version\":\"2.4.0\"}")
                .statusCode(200);

        JsonNode body = readBody(specId);
        assertEquals("2.4.0", body.path("info").path("version").asText());
        assertEquals("Platform team", body.path("info").path("contact").path("name").asText());
        assertEquals("partners", body.path("info").path("x-audience").asText());
        assertTrue(body.path("x-internal").asBoolean());
    }

    // Editing details is editing: the pointer moves to the edited API (chokepoint convention).
    @Test
    @AsAda
    void updateDetailsMovesTheEditorsPointer() {
        UUID first = createApi("Storefront API");
        createApi("Fleet API"); // the pointer now sits on the second create

        patchDetails(first, "{\"title\":\"Storefront API\",\"version\":\"1.1.0\"}")
                .statusCode(200);

        given()
                .when().get("/api/v1/specs/last-edited")
                .then()
                .statusCode(200)
                .body("specId", equalTo(first.toString()))
                .body("capabilityName", nullValue());
    }

    // AC2: the openapi spec version is visible on the summary (the details surface shows it
    // locked) and no details save can change it — immutability by omission (FEAT-003 AC3).
    @Test
    @AsAda
    void specVersionIsProjectedAndImmutable() {
        UUID specId = createApi("Storefront API");

        given()
                .when().get("/api/v1/specs")
                .then()
                .body("items[0].specVersion", equalTo("3.1.1"));
        patchDetails(specId, "{\"title\":\"Storefront API\",\"version\":\"9.9.9\"}")
                .statusCode(200)
                .body("specVersion", equalTo("3.1.1"));
        assertEquals("3.1.1", readBody(specId).path("openapi").asText());
    }

    // AC3: duplicate is a fork — new UUID, owner = the duplicator, fresh timestamps, derived
    // "(copy)" title, and a document functionally equivalent in every other node, unmodeled
    // content and info.version included.
    @Test
    @AsAda
    void duplicateForksTheDocumentUnderANewIdentity() {
        UUID specId = seedSpec("sub-grace", "Grace Hopper", "Payments API", "Charges.", "2.3.0", 9, 31);
        setBody(specId, "{\"openapi\":\"3.1.1\",\"x-internal\":true,"
                + "\"info\":{\"title\":\"Payments API\",\"version\":\"2.3.0\","
                + "\"contact\":{\"name\":\"Platform team\"}},"
                + "\"paths\":{\"/charges\":{\"get\":{\"summary\":\"Browse all charges\"}}}}");

        var response = duplicateApi(specId)
                .statusCode(201)
                .body("title", equalTo("Payments API (copy)"))
                .body("description", equalTo("Charges."))
                .body("apiVersion", equalTo("2.3.0"))
                .body("specVersion", equalTo("3.1.1"))
                .body("resourceCount", equalTo(9))
                .body("operationCount", equalTo(31))
                .extract();
        String copyId = response.path("id");
        assertNotEquals(specId.toString(), copyId);
        assertTrue(response.header("Location").endsWith("/api/v1/specs/" + copyId),
                "Location must address the new API itself");

        JsonNode original = readBody(specId);
        JsonNode copy = readBody(UUID.fromString(copyId));
        assertEquals("Payments API (copy)", copy.path("info").path("title").asText());
        ((ObjectNode) copy.path("info")).put("title", "Payments API");
        assertEquals(original, copy, "beyond info.title, the fork must be equivalent (AC3)");

        QuarkusTransaction.requiringNew().run(() -> {
            Spec originalSpec = specRepository.findById(specId);
            Spec copySpec = specRepository.findById(UUID.fromString(copyId));
            assertEquals("sub-ada", copySpec.owner.oidcSubject, "owner is the duplicator (AC3)");
            assertTrue(copySpec.createdAt.isAfter(originalSpec.createdAt), "fresh timestamps (AC3)");
        });
    }

    // The pointer is neither copied nor moved by a duplicate — managing is not editing.
    @Test
    @AsAda
    void duplicateDoesNotMoveThePointer() {
        UUID first = createApi("Storefront API");
        UUID second = createApi("Fleet API"); // the pointer sits on Fleet API

        duplicateApi(first).statusCode(201);

        given()
                .when().get("/api/v1/specs/last-edited")
                .then()
                .statusCode(200)
                .body("specId", equalTo(second.toString()));
        assertEquals(1, lastEditedLocationRepository.count(), "no pointer row is copied");
    }

    // AC4: the fork is independent — editing the copy leaves the original untouched, and both
    // appear on the home like any APIs.
    @Test
    @AsAda
    void duplicateAndOriginalDivergeIndependently() {
        UUID specId = productApi();
        String copyId = duplicateApi(specId).statusCode(201).extract().path("id");
        JsonNode originalBefore = readBody(specId);

        postResource(UUID.fromString(copyId), "{\"name\":\"Review\",\"capabilities\":[\"ADD\"]}")
                .statusCode(201);

        assertEquals(originalBefore, readBody(specId), "the original never sees the copy's edits");
        given()
                .when().get("/api/v1/specs")
                .then()
                .body("total", equalTo(2))
                .body("items[0].title", equalTo("Storefront API"))
                .body("items[0].resourceCount", equalTo(1))
                .body("items[1].title", equalTo("Storefront API (copy)"))
                .body("items[1].resourceCount", equalTo(2));
    }

    // AC5: confirmed deletion is terminal — gone from the list, gone by ID.
    @Test
    @AsAda
    void deleteRemovesTheApiFromListAndById() {
        UUID specId = productApi();

        deleteApi(specId).statusCode(204);

        given().when().get("/api/v1/specs").then().body("total", equalTo(0));
        given()
                .when().get("/api/v1/specs/{specId}", specId)
                .then()
                .statusCode(404)
                .contentType("application/problem+json");
        assertEquals(0, specRepository.count());
    }

    // AC5 × FEAT-002: every user's jump-back-in pointer at the deleted API is cleared — the
    // workspace is global, so other designers may point here too; unrelated pointers survive.
    @Test
    @AsAda
    void deleteClearsEveryUsersPointerAtTheSpec() {
        UUID doomed = seedSpec("sub-ada", "Ada Lovelace", "Payments API", null, "1.0", 0, 0);
        UUID kept = seedSpec("sub-ada", "Ada Lovelace", "Fleet API", null, "1.0", 0, 0);
        seedLocation("sub-ada", doomed, null);
        seedLocation("sub-grace", doomed, "Refund a payment");
        seedLocation("sub-bob", kept, null);

        deleteApi(doomed).statusCode(204);

        given().when().get("/api/v1/specs/last-edited").then().statusCode(204);
        assertEquals(1, lastEditedLocationRepository.count(), "only the unrelated pointer survives");
    }

    // AC6: a blank title is rejected through the problem contract; nothing is persisted.
    @Test
    @AsAda
    void updateDetailsRejectsABlankTitle() {
        UUID specId = createApi("Storefront API");

        patchDetails(specId, "{\"title\":\"   \",\"version\":\"1.0.0\"}")
                .statusCode(400)
                .contentType("application/problem+json")
                .body("violations[0].field", equalTo("title"));

        given()
                .when().get("/api/v1/specs/{specId}", specId)
                .then()
                .body("title", equalTo("Storefront API"));
        assertEquals("Storefront API", readBody(specId).path("info").path("title").asText());
    }

    // AC6's sibling: info.version is REQUIRED by the OpenAPI spec — a blank one would make
    // every save produce an invalid document, so it is rejected the same way.
    @Test
    @AsAda
    void updateDetailsRejectsABlankVersion() {
        UUID specId = createApi("Storefront API");

        patchDetails(specId, "{\"title\":\"Storefront API\",\"version\":\"  \"}")
                .statusCode(400)
                .contentType("application/problem+json")
                .body("violations[0].field", equalTo("version"));
        patchDetails(specId, "{\"title\":\"Storefront API\"}")
                .statusCode(400)
                .contentType("application/problem+json")
                .body("violations[0].field", equalTo("version"));

        assertEquals("1.0.0", readBody(specId).path("info").path("version").asText());
    }

    // Unknown API → 404 problem+json on every management endpoint.
    @Test
    @AsAda
    void manageEndpointsReturn404ForUnknownApi() {
        UUID unknown = UUID.randomUUID();

        patchDetails(unknown, "{\"title\":\"X\",\"version\":\"1.0.0\"}")
                .statusCode(404)
                .contentType("application/problem+json");
        duplicateApi(unknown)
                .statusCode(404)
                .contentType("application/problem+json");
        deleteApi(unknown)
                .statusCode(404)
                .contentType("application/problem+json");
    }

    // Same 401 posture as every other endpoint (FEAT-001).
    @Test
    void unauthenticatedManageEndpointsAreRejected() {
        String base = "/api/v1/specs/" + UUID.randomUUID();
        given().contentType("application/json").body("{\"title\":\"X\",\"version\":\"1.0.0\"}")
                .when().patch(base).then().statusCode(401);
        given().when().post(base + "/duplicate").then().statusCode(401);
        given().when().delete(base).then().statusCode(401);
    }

    // FEAT-008 AC1/AC3 (JSON): the export is the stored document itself — textually identical
    // to the body column, so every node (unmodeled content included) rides along in authored
    // order by construction.
    @Test
    @AsAda
    void exportAsJsonStreamsTheStoredDocumentVerbatim() {
        UUID specId = productApi();
        postField(specId, "Product", "{\"name\":\"Unit price\",\"coreType\":\"DECIMAL_NUMBER\","
                + "\"required\":true}").statusCode(201);

        String exported = exportApi(specId, "json")
                .statusCode(200)
                .header("Content-Type", containsString("application/json"))
                .extract().asString();

        assertEquals(readRawBody(specId), exported,
                "the JSON export must be the stored body, byte for byte (AC1)");
    }

    // FEAT-008 AC1 (YAML): every node in authored property order. JsonNode.equals ignores
    // object key order, so both sides are normalized through one exact JSON writer and
    // compared as strings — an order-sensitive equivalence.
    @Test
    @AsAda
    void exportAsYamlIsFunctionallyEquivalentToTheStoredDocument() throws Exception {
        UUID specId = productApi();
        postField(specId, "Product", "{\"name\":\"First name\",\"coreType\":\"TEXT\"}")
                .statusCode(201);

        String exported = exportApi(specId, "yaml")
                .statusCode(200)
                .header("Content-Type", containsString("application/yaml"))
                .extract().asString();

        assertEquals(
                EXACT_JSON.writeValueAsString(EXACT_JSON.readTree(readRawBody(specId))),
                EXACT_JSON.writeValueAsString(EXACT_YAML.readTree(exported)),
                "the YAML must carry every node in authored order (AC1)");
    }

    // FEAT-008 AC1 × PRIN-003: unmodeled nodes (extensions, foreign members) and deliberately
    // unsorted keys — integer-like response codes included — survive both exports.
    @Test
    @AsAda
    void exportCarriesUnmodeledContentInAuthoredOrder() throws Exception {
        UUID specId = seedSpec("sub-ada", "Ada Lovelace", "Payments API", "Charges.", "2.3.0", 0, 0);
        setBody(specId, "{\"openapi\":\"3.1.1\",\"x-internal\":true,"
                + "\"info\":{\"title\":\"Payments API\",\"version\":\"2.3.0\","
                + "\"contact\":{\"name\":\"Platform team\"}},"
                + "\"paths\":{\"/z-charges\":{\"get\":{\"responses\":"
                + "{\"404\":{\"description\":\"gone\"},\"200\":{\"description\":\"ok\"}}}},"
                + "\"/a-refunds\":{}}}");

        assertEquals(readRawBody(specId), exportApi(specId, "json").statusCode(200)
                .extract().asString());

        JsonNode yamlTree = EXACT_YAML.readTree(exportApi(specId, "yaml").statusCode(200)
                .extract().asString());
        assertTrue(yamlTree.path("x-internal").asBoolean(), "unmodeled root node (PRIN-003)");
        assertEquals("Platform team", yamlTree.path("info").path("contact").path("name").asText());
        assertEquals(List.of("/z-charges", "/a-refunds"), fieldNamesOf(yamlTree.path("paths")));
        assertEquals(List.of("404", "200"), fieldNamesOf(yamlTree.path("paths")
                .path("/z-charges").path("get").path("responses")));
    }

    // FEAT-008 AC1: strings a YAML parser would re-type when unquoted stay strings.
    @Test
    @AsAda
    void exportAsYamlKeepsAmbiguousScalarsAsStrings() throws Exception {
        UUID specId = seedSpec("sub-ada", "Ada Lovelace", "Ops API", null, "1.0", 0, 0);
        setBody(specId, "{\"openapi\":\"3.1.1\",\"info\":{\"title\":\"Ops API\","
                + "\"version\":\"1.0\",\"description\":\"on\"},\"paths\":{}}");

        JsonNode info = EXACT_YAML.readTree(exportApi(specId, "yaml").statusCode(200)
                .extract().asString()).path("info");

        assertTrue(info.path("version").isTextual(), "\"1.0\" must not become a float");
        assertEquals("1.0", info.path("version").asText());
        assertTrue(info.path("description").isTextual(), "\"on\" must not become a boolean");
    }

    // FEAT-008 AC2/AC3: both formats for every stored spec version, and the exported
    // `openapi` node is the stored one — no conversion.
    @ParameterizedTest
    @CsvSource({"3.0, 3.0.4, yaml", "3.0, 3.0.4, json", "3.1, 3.1.1, yaml", "3.1, 3.1.1, json",
            "3.2, 3.2.0, yaml", "3.2, 3.2.0, json"})
    @AsAda
    void exportOffersBothFormatsAtTheStoredSpecVersion(String minor, String expectedPatch,
            String format) throws Exception {
        String id = given()
                .contentType("application/json")
                .body("{\"title\":\"Fleet API\",\"specVersion\":\"" + minor + "\"}")
                .when().post("/api/v1/specs")
                .then().statusCode(201).extract().path("id");

        String exported = exportApi(UUID.fromString(id), format).statusCode(200)
                .extract().asString();

        ObjectMapper reader = format.equals("yaml") ? EXACT_YAML : EXACT_JSON;
        assertEquals(expectedPatch, reader.readTree(exported).path("openapi").asText(),
                "the export keeps the stored spec version (AC3)");
    }

    // FEAT-008 AC3: the file is named from the API's title with the format's extension.
    @Test
    @AsAda
    void exportNamesTheFileFromTheTitle() {
        UUID specId = createApi("Payments API");

        exportApi(specId, "yaml").statusCode(200)
                .header("Content-Disposition", equalTo("attachment;"
                        + " filename=\"Payments API.yaml\";"
                        + " filename*=UTF-8''Payments%20API.yaml"));
        exportApi(specId, "json").statusCode(200)
                .header("Content-Disposition", equalTo("attachment;"
                        + " filename=\"Payments API.json\";"
                        + " filename*=UTF-8''Payments%20API.json"));
    }

    // AC3's edge: hostile titles — quoted-string breakers, path separators, unicode — yield a
    // safe ASCII filename plus an RFC 5987 filename* carrying the unicode.
    @Test
    @AsAda
    void exportSanitizesTheFilenameFromAHostileTitle() {
        String id = given().contentType("application/json")
                .body("{\"title\":\"Payments/API: \\\"v2\\\" ✨\"}")
                .when().post("/api/v1/specs").then().statusCode(201).extract().path("id");

        exportApi(UUID.fromString(id), "yaml").statusCode(200)
                .header("Content-Disposition", equalTo("attachment;"
                        + " filename=\"Payments API v2.yaml\";"
                        + " filename*=UTF-8''Payments%20API%20v2%20%E2%9C%A8.yaml"));
    }

    // Exporting is managing, not editing — the jump-back-in pointer stays put (FEAT-007
    // precedent: duplicate/delete behave the same).
    @Test
    @AsAda
    void exportDoesNotMoveThePointer() {
        UUID first = createApi("Storefront API");
        UUID second = createApi("Fleet API"); // the pointer sits on Fleet API

        exportApi(first, "yaml").statusCode(200);

        given()
                .when().get("/api/v1/specs/last-edited")
                .then()
                .statusCode(200)
                .body("specId", equalTo(second.toString()));
    }

    // Unknown API → the same 404 problem contract as every other spec endpoint.
    @Test
    @AsAda
    void exportReturns404ForUnknownApi() {
        exportApi(UUID.randomUUID(), "yaml")
                .statusCode(404)
                .contentType("application/problem+json");
    }

    // The format is the request's one decision — never a silent default. Absent → 400 (bean
    // validation, problem+json); unrecognized (case included, the enum is the contract) → 404,
    // the status JAX-RS mandates for a query-param conversion failure. Both formats work, so
    // the distinction never reaches the real client.
    @Test
    @AsAda
    void exportRejectsAMissingOrUnknownFormat() {
        UUID specId = createApi("Payments API");

        given().when().get("/api/v1/specs/" + specId + "/document").then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("violations[0].field", equalTo("format"));
        exportApi(specId, "xml").statusCode(404);
        exportApi(specId, "YAML").statusCode(404);
    }

    // Same 401 posture as every other endpoint (FEAT-001).
    @Test
    void unauthenticatedExportIsRejected() {
        given().when().get("/api/v1/specs/" + UUID.randomUUID() + "/document?format=yaml")
                .then().statusCode(401);
    }

    // ---- FEAT-009: view a capability's contract ----

    // AC1/AC3: the contract in stable facet order, projected from the document — identity
    // (plain-language label first, derived detail after), the derived content-negotiation
    // line, and the answers; the Request facet absent where no input travels (AC2).
    @Test
    @AsAda
    void getCapabilityContractReturnsTheFacets() {
        UUID specId = productApi();

        getCapability(specId, "Product", "LOOK_UP")
                .statusCode(200)
                .body("capability.capability", equalTo("LOOK_UP"))
                .body("capability.label", equalTo("Look up one product"))
                .body("capability.method", equalTo("GET"))
                .body("capability.path", equalTo("/products/{id}"))
                .body("description", nullValue())
                .body("singularNoun", equalTo("product"))
                .body("request", nullValue())
                .body("headers", hasSize(1))
                .body("headers[0].name", equalTo("Accept"))
                .body("headers[0].value", equalTo("application/json"))
                .body("headers[0].derived", equalTo(true))
                .body("answers.successStatus", equalTo("200"))
                .body("answers.successDescription", equalTo("The product."))
                .body("answers.failures", hasSize(5))
                .body("answers.failures[2].status", equalTo("404"))
                .body("answers.failures[2].present", equalTo(true));
    }

    // AC5: Add derives the shape's fields (identity stated server-assigned via its auto
    // visibility); Update states merge-patch semantics and enumerates nothing.
    @Test
    @AsAda
    void getCapabilityContractDerivesTheRequestFacet() {
        UUID specId = productApi();

        getCapability(specId, "Product", "ADD")
                .statusCode(200)
                .body("request.mergePatch", equalTo(false))
                .body("request.fields", hasSize(1))
                .body("request.fields[0].name", equalTo("id"))
                .body("request.fields[0].visibility", equalTo("AUTO"));
        getCapability(specId, "Product", "UPDATE")
                .statusCode(200)
                .body("request.mergePatch", equalTo(true))
                .body("request.fields", hasSize(0));
    }

    // AC4: a capability predating this feature shows its standard answers as available —
    // and viewing writes nothing: the stored text is byte-identical afterwards.
    @Test
    @AsAda
    void getCapabilityContractShowsLegacyAnswersWithoutWriting() {
        UUID specId = productApi();
        stripStandardErrorsFromStored(specId);
        String before = readRawBody(specId);

        getCapability(specId, "Product", "BROWSE")
                .statusCode(200)
                .body("answers.failures", hasSize(5))
                .body("answers.failures[0].present", equalTo(false))
                .body("answers.failures[4].present", equalTo(false));

        assertEquals(before, readRawBody(specId), "viewing must never mutate (AC4)");
    }

    // UC3/AC6: one adopt call — exactly the applicable references, the shared furniture, no
    // count change, and the pointer's first capability-level write, all in one transaction.
    @Test
    @AsAda
    void adoptStandardErrorsRetrofitsAndMovesThePointer() {
        UUID specId = productApi();
        stripStandardErrorsFromStored(specId);

        adoptStandardErrors(specId, "Product", "UPDATE")
                .statusCode(200)
                .body("answers.failures", hasSize(6))
                .body("answers.failures[0].present", equalTo(true))
                .body("answers.failures[5].present", equalTo(true));

        JsonNode body = readBody(specId);
        assertEquals("#/components/responses/NotFound", body.path("paths")
                .path("/products/{id}").path("patch").path("responses").path("404")
                .path("$ref").asText());
        assertNoVendorExtensions(body);
        given()
                .when().get("/api/v1/specs/{specId}", specId)
                .then()
                .body("resourceCount", equalTo(1))
                .body("operationCount", equalTo(5));
        given()
                .when().get("/api/v1/specs/last-edited")
                .then()
                .statusCode(200)
                .body("specId", equalTo(specId.toString()))
                .body("capabilityName", equalTo("Update a product"));
    }

    // AC7: a second adoption references the existing furniture, never duplicates it.
    @Test
    @AsAda
    void adoptStandardErrorsReusesTheFurniture() {
        UUID specId = productApi();
        stripStandardErrorsFromStored(specId);

        adoptStandardErrors(specId, "Product", "LOOK_UP").statusCode(200);
        adoptStandardErrors(specId, "Product", "REMOVE").statusCode(200);

        JsonNode components = readBody(specId).path("components");
        assertEquals(6, components.path("responses").size(),
                "six reusable responses, created once — never twelve");
        assertEquals(List.of("Product", "Error"), fieldNamesOf(components.path("schemas")));
    }

    // UC5/AC10: switching off strips exactly the applicable references and nothing else —
    // furniture and counts stay, the pointer records the capability, and adopting switches
    // back on to the born document.
    @Test
    @AsAda
    void removeStandardErrorsSwitchesOffAndBackOn() {
        UUID specId = productApi();
        String born = readRawBody(specId);

        removeStandardErrors(specId, "Product", "LOOK_UP").statusCode(204);

        JsonNode off = readBody(specId);
        assertEquals(List.of("200"),
                fieldNamesOf(off.path("paths").path("/products/{id}").path("get").path("responses")));
        assertEquals(6, off.path("components").path("responses").size());
        assertTrue(off.path("components").path("schemas").has("Error"));
        given()
                .when().get("/api/v1/specs/{specId}", specId)
                .then()
                .body("resourceCount", equalTo(1))
                .body("operationCount", equalTo(5));
        given()
                .when().get("/api/v1/specs/last-edited")
                .then()
                .body("capabilityName", equalTo("Look up one product"));
        getCapability(specId, "Product", "LOOK_UP")
                .statusCode(200)
                .body("answers.failures[0].present", equalTo(false));

        adoptStandardErrors(specId, "Product", "LOOK_UP").statusCode(200);
        try {
            assertEquals(new ObjectMapper().readTree(born), readBody(specId),
                    "off then on must restore the born document");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    // Unknown resource or capability → the 404 problem contract; a capability that was never
    // derived on the resource, and an unknown capability literal, behave the same.
    @Test
    @AsAda
    void capabilityEndpointsReturn404ForUnknownTargets() {
        UUID specId = createApi("Storefront API");
        postResource(specId, "{\"name\":\"Product\",\"capabilities\":[\"BROWSE\"]}")
                .statusCode(201);

        getCapability(specId, "Product", "REMOVE")
                .statusCode(404)
                .contentType("application/problem+json");
        getCapability(specId, "Gadget", "BROWSE")
                .statusCode(404)
                .contentType("application/problem+json");
        getCapability(specId, "Product", "FLY").statusCode(404);
        adoptStandardErrors(specId, "Product", "REMOVE")
                .statusCode(404)
                .contentType("application/problem+json");
        removeStandardErrors(specId, "Product", "REMOVE")
                .statusCode(404)
                .contentType("application/problem+json");
    }

    // The FEAT-009 reservation (AC9): Error, in any casing, can never become a resource —
    // the furniture is derived plumbing, and an adoption would wire failure bodies to it.
    @Test
    @AsAda
    void addResourceRejectsTheReservedErrorName() {
        UUID specId = createApi("Storefront API");

        postResource(specId, "{\"name\":\"Error\",\"capabilities\":[\"ADD\"]}")
                .statusCode(409)
                .contentType("application/problem+json")
                .body("detail", containsString("reserved"));
        postResource(specId, "{\"name\":\"error\",\"capabilities\":[\"ADD\"]}")
                .statusCode(409);

        assertUntouched(specId);
    }

    private io.restassured.response.ValidatableResponse getCapability(UUID specId,
            String schemaName, String capability) {
        return given().when().get("/api/v1/specs/" + specId + "/resources/" + schemaName
                + "/capabilities/" + capability).then();
    }

    private io.restassured.response.ValidatableResponse adoptStandardErrors(UUID specId,
            String schemaName, String capability) {
        return given().when().post("/api/v1/specs/" + specId + "/resources/" + schemaName
                + "/capabilities/" + capability + "/standard-errors").then();
    }

    private io.restassured.response.ValidatableResponse removeStandardErrors(UUID specId,
            String schemaName, String capability) {
        return given().when().delete("/api/v1/specs/" + specId + "/resources/" + schemaName
                + "/capabilities/" + capability + "/standard-errors").then();
    }

    /**
     * Rewrites the stored document to its pre-FEAT-009 shape: shared furniture removed, every
     * standard failure answer stripped — the state older capabilities are in (UC3).
     */
    private void stripStandardErrorsFromStored(UUID specId) {
        ObjectNode document = (ObjectNode) readBody(specId);
        ((ObjectNode) document.path("components")).remove("responses");
        ((ObjectNode) document.path("components").path("schemas")).remove("Error");
        for (JsonNode pathItem : document.path("paths")) {
            for (String method : List.of("get", "post", "patch", "delete")) {
                if (pathItem.has(method)) {
                    ObjectNode responses = (ObjectNode) pathItem.path(method).path("responses");
                    for (String status : List.of("400", "401", "404", "422", "429", "500")) {
                        responses.remove(status);
                    }
                }
            }
        }
        setBody(specId, document.toString());
    }

    private io.restassured.response.ValidatableResponse exportApi(UUID specId, String format) {
        return given().when().get("/api/v1/specs/" + specId + "/document?format=" + format).then();
    }

    private io.restassured.response.ValidatableResponse patchDetails(UUID specId, String json) {
        return given().contentType("application/json").body(json)
                .when().patch("/api/v1/specs/" + specId).then();
    }

    private io.restassured.response.ValidatableResponse duplicateApi(UUID specId) {
        return given().when().post("/api/v1/specs/" + specId + "/duplicate").then();
    }

    private io.restassured.response.ValidatableResponse deleteApi(UUID specId) {
        return given().when().delete("/api/v1/specs/" + specId).then();
    }

    /** An API with one Product resource (all capabilities) — the FEAT-006 test fixture. */
    private UUID productApi() {
        UUID specId = createApi("Storefront API");
        postResource(specId, "{\"name\":\"Product\",\"capabilities\":" + ALL_CAPABILITIES + "}")
                .statusCode(201);
        return specId;
    }

    private io.restassured.response.ValidatableResponse postField(UUID specId, String schemaName,
            String json) {
        return given().contentType("application/json").body(json)
                .when().post("/api/v1/specs/" + specId + "/resources/" + schemaName + "/fields")
                .then();
    }

    private io.restassured.response.ValidatableResponse patchField(UUID specId, String schemaName,
            String propertyName, String json) {
        return given().contentType("application/json").body(json)
                .when().patch("/api/v1/specs/" + specId + "/resources/" + schemaName + "/fields/"
                        + propertyName)
                .then();
    }

    private io.restassured.response.ValidatableResponse deleteField(UUID specId, String schemaName,
            String propertyName) {
        return given()
                .when().delete("/api/v1/specs/" + specId + "/resources/" + schemaName + "/fields/"
                        + propertyName)
                .then();
    }

    /** "Nothing is persisted", field flavor: the shape still carries only id. */
    private void assertProductFieldsUntouched(UUID specId) {
        JsonNode schema = readBody(specId).path("components").path("schemas").path("Product");
        assertEquals(List.of("id"), fieldNamesOf(schema.path("properties")));
        assertEquals(List.of("id"), streamOf(schema.path("required")));
    }

    private static List<String> fieldNamesOf(JsonNode properties) {
        List<String> names = new ArrayList<>();
        properties.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static List<String> streamOf(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asText()));
        return values;
    }

    private UUID createApi(String title) {
        String id = given().contentType("application/json").body("{\"title\":\"" + title + "\"}")
                .when().post("/api/v1/specs").then().statusCode(201).extract().path("id");
        return UUID.fromString(id);
    }

    private io.restassured.response.ValidatableResponse postResource(UUID specId, String json) {
        return given().contentType("application/json").body(json)
                .when().post("/api/v1/specs/" + specId + "/resources").then();
    }

    /** "Nothing is persisted": the rejected add left no schema, no path, no count delta. */
    private void assertUntouched(UUID specId) {
        given()
                .when().get("/api/v1/specs/{specId}", specId)
                .then()
                .body("resourceCount", equalTo(0))
                .body("operationCount", equalTo(0))
                .body("resources", hasSize(0));
        JsonNode body = readBody(specId);
        assertTrue(body.path("paths").isMissingNode() || body.path("paths").isEmpty());
        assertTrue(body.path("components").isMissingNode() || body.path("components").isEmpty());
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
            spec.specVersion = "3.1.1";
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
        try {
            return new ObjectMapper().readTree(readRawBody(specId));
        } catch (Exception e) {
            throw new AssertionError("spec.body is not valid JSON", e);
        }
    }

    /** The persisted document as stored text — the FEAT-008 verbatim-export baseline. */
    private String readRawBody(UUID specId) {
        return QuarkusTransaction.requiringNew()
                .call(() -> specRepository.findById(specId).body);
    }
}
