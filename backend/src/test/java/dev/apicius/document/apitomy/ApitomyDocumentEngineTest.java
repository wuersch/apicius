package dev.apicius.document.apitomy;

import static dev.apicius.test.JsonAssertions.assertNoVendorExtensions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.apicius.document.DocumentProjection;
import dev.apicius.document.ResourceView;
import dev.apicius.document.SpecVersion;
import dev.apicius.document.derivation.CanonicalDerivation;
import dev.apicius.document.derivation.Capability;
import dev.apicius.document.derivation.ResourceDerivation;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/** Plain unit test (no Quarkus): the adapter is deliberately CDI-trivial. */
class ApitomyDocumentEngineTest {

    private final ApitomyDocumentEngine engine = new ApitomyDocumentEngine();
    private final ObjectMapper mapper = new ObjectMapper();

    // AC1: the seeded document carries the identity the dialog collected, and nothing else.
    @Test
    void seedsTitleAndAutoVersion() throws Exception {
        JsonNode document = parse(engine.createEmptyDocument(SpecVersion.V3_1, "Storefront API", null));

        assertEquals("3.1.1", document.path("openapi").asText());
        assertEquals("Storefront API", document.path("info").path("title").asText());
        assertEquals("1.0.0", document.path("info").path("version").asText());
    }

    // AC2: description present when provided…
    @Test
    void includesDescriptionWhenProvided() throws Exception {
        JsonNode document = parse(
                engine.createEmptyDocument(SpecVersion.V3_1, "Storefront API", "Sell products online."));

        assertEquals("Sell products online.", document.path("info").path("description").asText());
    }

    // …and absent (not null, not empty-string) when omitted.
    @Test
    void omitsDescriptionKeyWhenNull() throws Exception {
        JsonNode document = parse(engine.createEmptyDocument(SpecVersion.V3_1, "Storefront API", null));

        assertFalse(document.path("info").has("description"),
                "info.description must be absent, not null/empty (AC2)");
    }

    // AC3: the chosen minor pins the exact latest patch in the document.
    @ParameterizedTest
    @CsvSource({"V3_0, 3.0.4", "V3_1, 3.1.1", "V3_2, 3.2.0"})
    void pinsTheLatestPatchOfTheChosenMinor(SpecVersion version, String expected) throws Exception {
        JsonNode document = parse(engine.createEmptyDocument(version, "Fleet API", null));

        assertEquals(expected, document.path("openapi").asText());
    }

    // The container is empty: creation precedes all resource/capability authoring (FEAT-003).
    @Test
    void createsNoPathsOrComponents() throws Exception {
        JsonNode document = parse(engine.createEmptyDocument(SpecVersion.V3_1, "Storefront API", null));

        assertTrue(document.path("paths").isMissingNode() || document.path("paths").isEmpty());
        assertTrue(document.path("components").isMissingNode() || document.path("components").isEmpty());
    }

    // ---------------------------------------------------------------- FEAT-005: addResource

    // AC1: all five capabilities derive the full ADR-0010 table — schema, both paths, exact
    // operations, labels as summaries.
    @Test
    void addResourceDerivesTheFullCanonicalTable() throws Exception {
        JsonNode document = parse(addProduct(EnumSet.allOf(Capability.class)));

        JsonNode schema = document.path("components").path("schemas").path("Product");
        assertEquals("object", schema.path("type").asText());
        assertEquals("string", schema.path("properties").path("id").path("type").asText());
        assertTrue(schema.path("properties").path("id").path("readOnly").asBoolean());
        assertEquals("id", schema.path("required").get(0).asText());

        JsonNode collection = document.path("paths").path("/products");
        JsonNode item = document.path("paths").path("/products/{id}");
        assertEquals("listProducts", collection.path("get").path("operationId").asText());
        assertEquals("createProduct", collection.path("post").path("operationId").asText());
        assertEquals("getProduct", item.path("get").path("operationId").asText());
        assertEquals("updateProduct", item.path("patch").path("operationId").asText());
        assertEquals("deleteProduct", item.path("delete").path("operationId").asText());

        assertEquals("Browse all products", collection.path("get").path("summary").asText());
        assertEquals("Look up one product", item.path("get").path("summary").asText());
        assertEquals("Add a product", collection.path("post").path("summary").asText());
        assertEquals("Update a product", item.path("patch").path("summary").asText());
        assertEquals("Remove a product", item.path("delete").path("summary").asText());
    }

    // AC1: the derived response/request constructs, exactly per the table.
    @Test
    void addResourceDerivesBodiesAndResponsesPerTheTable() throws Exception {
        JsonNode document = parse(addProduct(EnumSet.allOf(Capability.class)));
        JsonNode collection = document.path("paths").path("/products");
        JsonNode item = document.path("paths").path("/products/{id}");
        String ref = "#/components/schemas/Product";

        // Browse: 200 with the inline {items: [X]} wrapper — no named wrapper schema exists.
        JsonNode wrapper = collection.path("get").path("responses").path("200")
                .path("content").path("application/json").path("schema");
        assertEquals("object", wrapper.path("type").asText());
        assertEquals("array", wrapper.path("properties").path("items").path("type").asText());
        assertEquals(ref, wrapper.path("properties").path("items").path("items").path("$ref").asText());
        assertEquals(1, document.path("components").path("schemas").size(),
                "the wrapper must be inline, never a named schema");

        // Add: required application/json request, 201 ref.
        JsonNode post = collection.path("post");
        assertTrue(post.path("requestBody").path("required").asBoolean());
        assertEquals(ref, post.path("requestBody").path("content").path("application/json")
                .path("schema").path("$ref").asText());
        assertEquals(ref, post.path("responses").path("201").path("content")
                .path("application/json").path("schema").path("$ref").asText());

        // Update: merge-patch+json request (RFC 7386), 200 ref.
        JsonNode patch = item.path("patch");
        assertTrue(patch.path("requestBody").path("required").asBoolean());
        assertEquals(ref, patch.path("requestBody").path("content")
                .path("application/merge-patch+json").path("schema").path("$ref").asText());

        // Remove: 204 without content; item operations carry a body-less, plain-language 404.
        JsonNode delete = item.path("delete");
        assertFalse(delete.path("responses").path("204").has("content"));
        assertEquals("No product with this id exists.",
                delete.path("responses").path("404").path("description").asText());
        assertFalse(delete.path("responses").path("404").has("content"));
        assertTrue(item.path("get").path("responses").has("404"));
        assertTrue(item.path("patch").path("responses").has("404"));
        assertFalse(collection.path("get").path("responses").has("404"));

        // The {id} parameter is declared once, at the item path-item level.
        JsonNode parameter = item.path("parameters").get(0);
        assertEquals("id", parameter.path("name").asText());
        assertEquals("path", parameter.path("in").asText());
        assertTrue(parameter.path("required").asBoolean());
        assertEquals("string", parameter.path("schema").path("type").asText());
        assertFalse(collection.has("parameters"));
    }

    // AC1: schema description present when provided, absent (not null/empty) when not.
    @Test
    void addResourceIncludesSchemaDescriptionOnlyWhenProvided() throws Exception {
        String empty = engine.createEmptyDocument(SpecVersion.V3_1, "Storefront API", null);
        ResourceDerivation derivation =
                CanonicalDerivation.derive("Product", EnumSet.of(Capability.ADD));

        JsonNode with = parse(engine.addResource(empty, derivation, "Something you sell."));
        JsonNode without = parse(engine.addResource(empty, derivation, null));

        assertEquals("Something you sell.", with.path("components").path("schemas")
                .path("Product").path("description").asText());
        assertFalse(without.path("components").path("schemas").path("Product").has("description"));
    }

    // AC3: only the ADR-0010 constructs are written — no x- extensions anywhere, and the
    // pre-existing document (info, an earlier resource) is untouched.
    @Test
    void addResourceWritesNothingApiciusSpecificAndTouchesNothingElse() throws Exception {
        String withProduct = addProduct(EnumSet.allOf(Capability.class));
        JsonNode before = parse(withProduct);

        ResourceDerivation review =
                CanonicalDerivation.derive("Review", EnumSet.of(Capability.BROWSE));
        JsonNode after = parse(engine.addResource(withProduct, review, null));

        assertNoVendorExtensions(after);
        assertEquals(before.path("info"), after.path("info"));
        assertEquals(before.path("openapi"), after.path("openapi"));
        assertEquals(before.path("components").path("schemas").path("Product"),
                after.path("components").path("schemas").path("Product"));
        assertEquals(before.path("paths").path("/products"), after.path("paths").path("/products"));
        assertEquals(before.path("paths").path("/products/{id}"),
                after.path("paths").path("/products/{id}"));
    }

    // AC4: a subset derives exactly the chosen operations; a path item with no chosen
    // operation does not exist at all.
    @Test
    void addResourceOmitsThePathItemNoChosenOperationLivesOn() throws Exception {
        JsonNode collectionOnly = parse(addProduct(EnumSet.of(Capability.ADD)));
        assertTrue(collectionOnly.path("paths").has("/products"));
        assertFalse(collectionOnly.path("paths").has("/products/{id}"));
        assertEquals(1, collectionOnly.path("paths").path("/products").size(),
                "only post may exist on the collection");

        JsonNode itemOnly = parse(addProduct(EnumSet.of(Capability.LOOK_UP)));
        assertFalse(itemOnly.path("paths").has("/products"));
        assertTrue(itemOnly.path("paths").has("/products/{id}"));
    }

    // The dialect branch: identical output JSON on every supported version.
    @ParameterizedTest
    @EnumSource(SpecVersion.class)
    void addResourceDerivesIdenticallyAcrossSpecVersions(SpecVersion version) throws Exception {
        String empty = engine.createEmptyDocument(version, "Storefront API", null);
        ResourceDerivation derivation =
                CanonicalDerivation.derive("Product", EnumSet.allOf(Capability.class));

        JsonNode document = parse(engine.addResource(empty, derivation, null));

        JsonNode schema = document.path("components").path("schemas").path("Product");
        assertEquals("object", schema.path("type").asText());
        assertEquals("string", schema.path("properties").path("id").path("type").asText());
        assertEquals(5, operationCount(document));
    }

    // ---------------------------------------------------------------- FEAT-005: project

    @Test
    void projectsAnEmptyDocumentAsEmpty() {
        DocumentProjection projection =
                engine.project(engine.createEmptyDocument(SpecVersion.V3_1, "Storefront API", null));

        assertEquals(List.of(), projection.schemaNames());
        assertEquals(List.of(), projection.paths());
        assertEquals(List.of(), projection.resources());
    }

    // AC8: what addResource wrote, project reads back — recognition is derivation inverted.
    @Test
    void projectionRoundTripsACreatedResource() {
        String empty = engine.createEmptyDocument(SpecVersion.V3_1, "Storefront API", null);
        ResourceDerivation derivation = CanonicalDerivation.derive("Order item",
                EnumSet.of(Capability.BROWSE, Capability.ADD, Capability.REMOVE));

        DocumentProjection projection =
                engine.project(engine.addResource(empty, derivation, "A line of an order."));

        assertEquals(List.of("OrderItem"), projection.schemaNames());
        assertEquals(List.of("/order-items", "/order-items/{id}"), projection.paths());
        ResourceView resource = projection.resources().getFirst();
        assertEquals("OrderItem", resource.name());
        assertEquals("A line of an order.", resource.description());
        assertEquals(List.of(Capability.BROWSE, Capability.ADD, Capability.REMOVE),
                resource.capabilities().stream().map(c -> c.capability()).toList());
        assertEquals(List.of("Browse all order items", "Add an order item", "Remove an order item"),
                resource.capabilities().stream().map(c -> c.label()).toList());
        assertEquals(List.of("GET /order-items", "POST /order-items", "DELETE /order-items/{id}"),
                resource.capabilities().stream().map(c -> c.method() + " " + c.path()).toList());
    }

    // The summary is the round-trip carrier: edited → shown; missing → canonical fallback,
    // never a lost structural match (ADR-0010).
    @Test
    void projectionPrefersTheSummaryAndFallsBackWhenItIsMissing() throws Exception {
        ObjectNode document = (ObjectNode) parse(addProduct(EnumSet.of(Capability.BROWSE, Capability.ADD)));
        ObjectNode collection = (ObjectNode) document.path("paths").path("/products");
        ((ObjectNode) collection.path("get")).put("summary", "See what's on the shelf");
        ((ObjectNode) collection.path("post")).remove("summary");

        DocumentProjection projection = engine.project(mapper.writeValueAsString(document));

        assertEquals(List.of("See what's on the shelf", "Add a product"),
                projection.resources().getFirst().capabilities().stream().map(c -> c.label()).toList());
    }

    // PascalCase is a lossy encoding of the noun ("iPhone" and "I phone" both become IPhone),
    // so recognition tries candidate segmentations against the document's actual paths —
    // a created resource must never vanish from the projection, whatever its noun.
    @ParameterizedTest
    @CsvSource({"Category 5, Category5", "iPhone, IPhone", "Product2, Product2", "A B, AB"})
    void projectionRoundTripsNamesWhosePascalCaseIsAmbiguous(String noun, String schemaName) {
        String empty = engine.createEmptyDocument(SpecVersion.V3_1, "Storefront API", null);
        ResourceDerivation derivation =
                CanonicalDerivation.derive(noun, EnumSet.allOf(Capability.class));

        DocumentProjection projection =
                engine.project(engine.addResource(empty, derivation, null));

        assertEquals(1, projection.resources().size(),
                "the created resource must be recognized, not silently dropped");
        ResourceView resource = projection.resources().getFirst();
        assertEquals(schemaName, resource.name());
        assertEquals(5, resource.capabilities().size());
        assertEquals(derivation.collectionPath(), resource.capabilities().getFirst().path());
    }

    // A capability-less schema is a datatype (glossary): visible to uniqueness via
    // schemaNames, absent from resources.
    @Test
    void projectionListsACapabilityLessSchemaAsNoResource() throws Exception {
        ObjectNode document = (ObjectNode) parse(addProduct(EnumSet.of(Capability.BROWSE)));
        ObjectNode money = ((ObjectNode) document.path("components").path("schemas")).putObject("Money");
        money.put("type", "object");

        DocumentProjection projection = engine.project(mapper.writeValueAsString(document));

        assertEquals(List.of("Product", "Money"), projection.schemaNames());
        assertEquals(List.of("Product"),
                projection.resources().stream().map(ResourceView::name).toList());
    }

    private String addProduct(Set<Capability> capabilities) {
        String empty = engine.createEmptyDocument(SpecVersion.V3_1, "Storefront API", null);
        return engine.addResource(empty,
                CanonicalDerivation.derive("Product", capabilities), null);
    }

    private static int operationCount(JsonNode document) {
        int count = 0;
        for (JsonNode pathItem : document.path("paths")) {
            for (String method : List.of("get", "post", "patch", "delete", "put")) {
                if (pathItem.has(method)) {
                    count++;
                }
            }
        }
        return count;
    }

    private JsonNode parse(String body) throws Exception {
        return mapper.readTree(body);
    }
}
