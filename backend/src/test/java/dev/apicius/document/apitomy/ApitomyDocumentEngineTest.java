package dev.apicius.document.apitomy;

import static dev.apicius.test.JsonAssertions.assertNoVendorExtensions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.apicius.document.AnswersFacetView;
import dev.apicius.document.CapabilityContractView;
import dev.apicius.document.CapabilityView;
import dev.apicius.document.DeclarationView;
import dev.apicius.document.DocumentProjection;
import dev.apicius.document.FailureAnswerView;
import dev.apicius.document.FieldView;
import dev.apicius.document.HeaderLineView;
import dev.apicius.document.PagingFacetView;
import dev.apicius.document.RequestFacetView;
import dev.apicius.document.ResourceView;
import dev.apicius.document.SpecVersion;
import dev.apicius.document.derivation.CanonicalDerivation;
import dev.apicius.document.derivation.Capability;
import dev.apicius.document.derivation.CoreType;
import dev.apicius.document.derivation.DeclarationEdit;
import dev.apicius.document.derivation.DeclarationLocation;
import dev.apicius.document.derivation.DerivedOperation;
import dev.apicius.document.derivation.FieldEdit;
import dev.apicius.document.derivation.FieldKind;
import dev.apicius.document.derivation.FieldVisibility;
import dev.apicius.document.derivation.ParameterKind;
import dev.apicius.document.derivation.Refinement;
import dev.apicius.document.derivation.ResourceDerivation;
import dev.apicius.document.derivation.StandardErrors;
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

        // Browse: 200 with the inline {data: [X]} wrapper, data required (the modern-petstore
        // key) — no named wrapper schema exists. FEAT-010 adds the required pagination member.
        JsonNode wrapper = collection.path("get").path("responses").path("200")
                .path("content").path("application/json").path("schema");
        assertEquals("object", wrapper.path("type").asText());
        assertEquals("array", wrapper.path("properties").path("data").path("type").asText());
        assertEquals(ref, wrapper.path("properties").path("data").path("items").path("$ref").asText());
        assertEquals(List.of("data", "pagination"), textsOf(wrapper.path("required")));
        assertEquals(2, document.path("components").path("schemas").size(),
                "the wrapper must be inline, never a named schema — only the resource and "
                        + "the FEAT-009 Error furniture exist");

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

        // Remove: 204 without content; item operations reference the shared 404 (FEAT-009).
        JsonNode delete = item.path("delete");
        assertFalse(delete.path("responses").path("204").has("content"));
        assertEquals("#/components/responses/NotFound",
                delete.path("responses").path("404").path("$ref").asText());
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

    // ---------------------------------------------------------------- FEAT-006: fields

    // AC1: a field is exactly its ADR-0011 constructs — type, description, required entry.
    @Test
    void addFieldWritesTheProperty() throws Exception {
        JsonNode document = parse(engine.addField(addProduct(EnumSet.of(Capability.BROWSE)),
                "Product", field("price", CoreType.DECIMAL_NUMBER, null, false, true,
                        FieldVisibility.NORMAL, "Unit price in USD.")));

        JsonNode price = document.path("components").path("schemas").path("Product")
                .path("properties").path("price");
        assertEquals("number", price.path("type").asText());
        assertEquals("Unit price in USD.", price.path("description").asText());
        assertFalse(price.has("format"));
        assertFalse(price.has("readOnly"));
        assertFalse(price.has("writeOnly"));
        assertEquals(List.of("id", "price"), requiredOf(document, "Product"));
    }

    // AC4: a refinement is format on the type; a list is array + items — exactly per the table.
    @Test
    void addFieldWritesRefinementAndListPerTheTable() throws Exception {
        String withEmail = engine.addField(addProduct(EnumSet.of(Capability.BROWSE)), "Product",
                field("contact", CoreType.TEXT, Refinement.EMAIL, false, false,
                        FieldVisibility.NORMAL, null));
        JsonNode contact = parse(withEmail).path("components").path("schemas").path("Product")
                .path("properties").path("contact");
        assertEquals("string", contact.path("type").asText());
        assertEquals("email", contact.path("format").asText());

        JsonNode related = parse(engine.addField(withEmail, "Product",
                field("relatedProducts", CoreType.TEXT, Refinement.UUID, true, false,
                        FieldVisibility.NORMAL, null)))
                .path("components").path("schemas").path("Product")
                .path("properties").path("relatedProducts");
        assertEquals("array", related.path("type").asText());
        assertEquals("string", related.path("items").path("type").asText());
        assertEquals("uuid", related.path("items").path("format").asText());
        assertFalse(related.has("format"), "format belongs to the items, not the array");
        assertFalse(related.path("items").has("required"));
    }

    // AC10 by construction, both writable halves: auto is readOnly, write-only is writeOnly —
    // one value, so a property can never carry both.
    @ParameterizedTest
    @CsvSource({"NORMAL, false, false", "AUTO, true, false", "WRITE_ONLY, false, true"})
    void addFieldWritesVisibilityAsOneConstruct(FieldVisibility visibility, boolean readOnly,
            boolean writeOnly) throws Exception {
        JsonNode token = parse(engine.addField(addProduct(EnumSet.of(Capability.BROWSE)),
                "Product", field("syncToken", CoreType.TEXT, null, false, false, visibility, null)))
                .path("components").path("schemas").path("Product")
                .path("properties").path("syncToken");

        assertEquals(readOnly, token.path("readOnly").asBoolean());
        assertEquals(writeOnly, token.path("writeOnly").asBoolean());
    }

    // AC5, the override half: an explicit normal visibility on Text-as-password persists
    // format: password without writeOnly — the writer never re-applies the house rule.
    @Test
    void addFieldHonorsAnOverriddenPasswordVisibility() throws Exception {
        JsonNode password = parse(engine.addField(addProduct(EnumSet.of(Capability.BROWSE)),
                "Product", field("password", CoreType.TEXT, Refinement.PASSWORD, false, false,
                        FieldVisibility.NORMAL, null)))
                .path("components").path("schemas").path("Product")
                .path("properties").path("password");

        assertEquals("password", password.path("format").asText());
        assertFalse(password.has("writeOnly"));
    }

    // AC3: only ADR-0011 constructs are written — no x- extensions, and everything the edit
    // does not own (info, paths, the other schema, sibling fields) is untouched.
    @Test
    void addFieldWritesNothingApiciusSpecificAndTouchesNothingElse() throws Exception {
        String withReview = engine.addResource(addProduct(EnumSet.allOf(Capability.class)),
                CanonicalDerivation.derive("Review", EnumSet.of(Capability.BROWSE)), null);
        String withName = engine.addField(withReview, "Product",
                field("name", CoreType.TEXT, null, false, true, FieldVisibility.NORMAL, null));
        JsonNode before = parse(withName);

        JsonNode after = parse(engine.addField(withName, "Product",
                field("price", CoreType.DECIMAL_NUMBER, null, false, false,
                        FieldVisibility.NORMAL, null)));

        assertNoVendorExtensions(after);
        assertEquals(before.path("info"), after.path("info"));
        assertEquals(before.path("openapi"), after.path("openapi"));
        assertEquals(before.path("paths"), after.path("paths"));
        assertEquals(before.path("components").path("schemas").path("Review"),
                after.path("components").path("schemas").path("Review"));
        assertEquals(before.path("components").path("schemas").path("Product").path("properties")
                .path("name"), after.path("components").path("schemas").path("Product")
                .path("properties").path("name"));
    }

    // AC6: rewritten in place — a rename keeps the property's position, required membership
    // follows the field, and stale constructs of the old kind do not survive the rewrite.
    @Test
    void updateFieldRewritesThePropertyInPlace() throws Exception {
        String body = addProduct(EnumSet.of(Capability.BROWSE));
        body = engine.addField(body, "Product", field("name", CoreType.TEXT, null, false, true,
                FieldVisibility.NORMAL, null));
        body = engine.addField(body, "Product", field("price", CoreType.TEXT, Refinement.EMAIL,
                false, true, FieldVisibility.WRITE_ONLY, "Wrongly typed."));
        body = engine.addField(body, "Product", field("inStock", CoreType.YES_NO, null, false,
                false, FieldVisibility.NORMAL, null));
        JsonNode before = parse(body);

        JsonNode after = parse(engine.updateField(body, "Product", "price",
                field("unitPrice", CoreType.DECIMAL_NUMBER, null, false, true,
                        FieldVisibility.NORMAL, null)));

        JsonNode properties = after.path("components").path("schemas").path("Product")
                .path("properties");
        assertEquals(List.of("id", "name", "unitPrice", "inStock"), keysOf(properties),
                "the rename must keep the property's position");
        JsonNode unitPrice = properties.path("unitPrice");
        assertEquals("number", unitPrice.path("type").asText());
        assertFalse(unitPrice.has("format"), "the old refinement must not survive the rewrite");
        assertFalse(unitPrice.has("writeOnly"), "the old visibility must not survive the rewrite");
        assertFalse(unitPrice.has("description"), "the old description must not survive the rewrite");
        assertEquals(List.of("id", "name", "unitPrice"), requiredOf(after, "Product"),
                "required membership follows the field, in place");
        assertEquals(before.path("components").path("schemas").path("Product").path("properties")
                .path("name"), properties.path("name"));
    }

    // AC6: dropping required on an update removes the entry; gaining it appends.
    @Test
    void updateFieldMovesRequiredMembershipWithTheChoice() throws Exception {
        String body = engine.addField(addProduct(EnumSet.of(Capability.BROWSE)), "Product",
                field("name", CoreType.TEXT, null, false, true, FieldVisibility.NORMAL, null));

        JsonNode dropped = parse(engine.updateField(body, "Product", "name",
                field("name", CoreType.TEXT, null, false, false, FieldVisibility.NORMAL, null)));
        assertEquals(List.of("id"), requiredOf(dropped, "Product"));

        JsonNode regained = parse(engine.updateField(
                engine.updateField(body, "Product", "name",
                        field("name", CoreType.TEXT, null, false, false, FieldVisibility.NORMAL, null)),
                "Product", "name",
                field("name", CoreType.TEXT, null, false, true, FieldVisibility.NORMAL, null)));
        assertEquals(List.of("id", "name"), requiredOf(regained, "Product"));
    }

    // AC8: the property and its required entry are gone without other trace; a shape reduced
    // to id alone remains valid and projects.
    @Test
    void removeFieldDropsThePropertyAndItsRequiredEntryOnly() throws Exception {
        String body = engine.addField(addProduct(EnumSet.of(Capability.BROWSE)), "Product",
                field("name", CoreType.TEXT, null, false, true, FieldVisibility.NORMAL, null));
        JsonNode before = parse(body);

        String reduced = engine.removeField(body, "Product", "name");
        JsonNode after = parse(reduced);

        assertEquals(List.of("id"), keysOf(after.path("components").path("schemas")
                .path("Product").path("properties")));
        assertEquals(List.of("id"), requiredOf(after, "Product"));
        assertEquals(before.path("paths"), after.path("paths"));
        assertEquals(before.path("info"), after.path("info"));
        assertEquals(1, engine.project(reduced).resources().getFirst().fields().size());
    }

    // The dialect branch, extended to field constructs: identical output on every version
    // (ADR-0011's table is deliberately dialect-stable; patterns.md's trigger stays unhit).
    @ParameterizedTest
    @EnumSource(SpecVersion.class)
    void addFieldSerializesIdenticallyAcrossSpecVersions(SpecVersion version) throws Exception {
        String empty = engine.createEmptyDocument(version, "Storefront API", null);
        String body = engine.addResource(empty,
                CanonicalDerivation.derive("Product", EnumSet.of(Capability.BROWSE)), null);

        JsonNode tokens = parse(engine.addField(body, "Product",
                field("tokens", CoreType.TEXT, Refinement.UUID, true, true,
                        FieldVisibility.AUTO, "Server-issued tokens.")))
                .path("components").path("schemas").path("Product").path("properties")
                .path("tokens");

        assertEquals(parse("""
                {"type":"array","items":{"type":"string","format":"uuid"},
                 "description":"Server-issued tokens.","readOnly":true}"""), tokens);
    }

    // AC11: what the field writer wrote, project reads back — the table read backwards, id
    // included as an ordinary auto-visibility field.
    @Test
    void projectionRoundTripsAuthoredFields() {
        String body = addProduct(EnumSet.of(Capability.BROWSE));
        body = engine.addField(body, "Product", field("name", CoreType.TEXT, null, false, true,
                FieldVisibility.NORMAL, null));
        body = engine.addField(body, "Product", field("contact", CoreType.TEXT, Refinement.EMAIL,
                false, false, FieldVisibility.NORMAL, "How to reach the seller."));
        body = engine.addField(body, "Product", field("tags", CoreType.TEXT, null, true, false,
                FieldVisibility.NORMAL, null));
        body = engine.addField(body, "Product", field("createdAt", CoreType.DATE_TIME, null,
                false, false, FieldVisibility.AUTO, null));

        List<FieldView> fields = engine.project(body).resources().getFirst().fields();

        assertEquals(List.of(
                new FieldView("id", new FieldKind(CoreType.TEXT, null, false), true,
                        FieldVisibility.AUTO, null),
                new FieldView("name", new FieldKind(CoreType.TEXT, null, false), true,
                        FieldVisibility.NORMAL, null),
                new FieldView("contact", new FieldKind(CoreType.TEXT, Refinement.EMAIL, false),
                        false, FieldVisibility.NORMAL, "How to reach the seller."),
                new FieldView("tags", new FieldKind(CoreType.TEXT, null, true), false,
                        FieldVisibility.NORMAL, null),
                new FieldView("createdAt", new FieldKind(CoreType.DATE_TIME, null, false), false,
                        FieldVisibility.AUTO, null)),
                fields);
    }

    // ------------------------------------------------------- FEAT-009: standard error answers

    // AC8: every derived operation answers its standard set from birth — exactly the
    // applicable references per the table, plus its success answer, and nothing else.
    @ParameterizedTest
    @EnumSource(Capability.class)
    void addResourceCarriesStandardErrorsFromBirth(Capability capability) throws Exception {
        JsonNode document = parse(addProduct(EnumSet.of(capability)));
        DerivedOperation derived = CanonicalDerivation
                .derive("Product", EnumSet.of(capability)).operations().getFirst();
        JsonNode responses = document.path("paths").path(derived.path())
                .path(derived.method().toLowerCase()).path("responses");
        List<StandardErrors.Answer> applicable =
                StandardErrors.applicableTo(capability, derived.path().endsWith("/{id}"));

        for (StandardErrors.Answer answer : applicable) {
            assertEquals("#/components/responses/" + answer.responseName(),
                    responses.path(answer.status()).path("$ref").asText());
        }
        assertEquals(1 + applicable.size(), responses.size(),
                "the success answer plus exactly the applicable failures");
    }

    // The shared furniture, exactly as FEAT-009 specifies it: the Error schema (RFC 9457
    // problem details) and one reusable response per standard failure answer.
    @Test
    void addResourceWritesTheSharedErrorFurniture() throws Exception {
        JsonNode document = parse(addProduct(EnumSet.of(Capability.BROWSE)));

        // The shape mirrors the modern-petstore reference (docs/misc/examples).
        assertEquals(parse("""
                {"type":"object",
                 "description":"A problem detail (RFC 9457) describing why a request failed.",
                 "properties":{
                   "type":{"type":"string","format":"uri","description":"A URI reference identifying the type of problem."},
                   "title":{"type":"string","description":"A short, human-readable summary of the problem type."},
                   "status":{"type":"integer","description":"The HTTP status code."},
                   "detail":{"type":"string","description":"A human-readable explanation specific to this occurrence."},
                   "instance":{"type":"string","format":"uri","description":"A URI reference identifying this specific occurrence."},
                   "errors":{"type":"array",
                     "description":"Field-level problems, present when the input failed validation.",
                     "items":{"type":"object",
                       "properties":{
                         "field":{"type":"string","description":"The input field the problem is about."},
                         "message":{"type":"string","description":"What is wrong with the field."},
                         "code":{"type":"string","description":"A machine-readable code for the kind of problem."}},
                       "required":["field","message"]}}},
                 "required":["type","title","status"]}"""),
                document.path("components").path("schemas").path("Error"));
        assertEquals(parse("""
                {"description":"No resource with this id exists.",
                 "content":{"application/problem+json":{"schema":{"$ref":"#/components/schemas/Error"}}}}"""),
                document.path("components").path("responses").path("NotFound"));
        assertEquals(List.of("BadRequest", "Unauthorized", "NotFound", "UnprocessableEntity",
                "TooManyRequests", "InternalServerError"),
                keysOf(document.path("components").path("responses")));
    }

    // AC7: the furniture is created once and referenced, never duplicated, by later writes.
    @Test
    void furnitureIsCreatedOnceAndReused() throws Exception {
        String withProduct = addProduct(EnumSet.allOf(Capability.class));
        JsonNode document = parse(engine.addResource(withProduct,
                CanonicalDerivation.derive("Review", EnumSet.of(Capability.LOOK_UP)), null));

        assertEquals(List.of("Product", "Error", "Review"),
                keysOf(document.path("components").path("schemas")));
        assertEquals(6, document.path("components").path("responses").size());
        assertEquals("#/components/responses/NotFound", document.path("paths")
                .path("/reviews/{id}").path("get").path("responses").path("404")
                .path("$ref").asText());
    }

    // UC3/AC6: adopting on a pre-FEAT-009 operation replaces its inline 404 with the shared
    // reference and adds exactly the applicable set; sibling operations stay untouched.
    @Test
    void adoptStandardErrorsRetrofitsALegacyOperation() throws Exception {
        String legacy = stripStandardErrors(addProduct(EnumSet.allOf(Capability.class)));
        JsonNode before = parse(legacy);
        assertEquals("No product with this id exists.", before.path("paths")
                .path("/products/{id}").path("get").path("responses").path("404")
                .path("description").asText());

        JsonNode after = parse(engine.adoptStandardErrors(legacy, "Product", Capability.LOOK_UP));

        JsonNode responses = after.path("paths").path("/products/{id}").path("get").path("responses");
        assertEquals(parse("""
                {"$ref":"#/components/responses/NotFound"}"""), responses.path("404"));
        assertEquals(List.of("200", "400", "401", "404", "429", "500"), keysOf(responses));
        assertEquals(6, after.path("components").path("responses").size());
        assertNoVendorExtensions(after);
        assertEquals(before.path("info"), after.path("info"));
        assertEquals(before.path("paths").path("/products"), after.path("paths").path("/products"));
        assertEquals(before.path("paths").path("/products/{id}").path("patch"),
                after.path("paths").path("/products/{id}").path("patch"));
        assertEquals(before.path("paths").path("/products/{id}").path("delete"),
                after.path("paths").path("/products/{id}").path("delete"));
    }

    // UC5/AC10: the opt-out removes exactly the applicable references — furniture, sibling
    // operations, and the success answer stay untouched.
    @Test
    void removeStandardErrorsStripsExactlyTheApplicableReferences() throws Exception {
        String body = addProduct(EnumSet.allOf(Capability.class));
        JsonNode before = parse(body);

        JsonNode after = parse(engine.removeStandardErrors(body, "Product", Capability.UPDATE));

        JsonNode responses = after.path("paths").path("/products/{id}").path("patch").path("responses");
        assertEquals(List.of("200"), keysOf(responses));
        assertEquals(before.path("paths").path("/products/{id}").path("patch")
                .path("responses").path("200"), responses.path("200"));
        assertEquals(before.path("components").path("schemas").path("Error"),
                after.path("components").path("schemas").path("Error"));
        assertEquals(6, after.path("components").path("responses").size());
        assertEquals(before.path("paths").path("/products"), after.path("paths").path("/products"));
        assertEquals(before.path("paths").path("/products/{id}").path("get"),
                after.path("paths").path("/products/{id}").path("get"));
        assertNoVendorExtensions(after);
    }

    // AC10: off is reversible — switching back on yields the document adoption first
    // produced; removing when already absent is a no-op.
    @Test
    void removeStandardErrorsRoundTripsWithAdopt() throws Exception {
        String born = addProduct(EnumSet.allOf(Capability.class));

        String off = engine.removeStandardErrors(born, "Product", Capability.LOOK_UP);
        String offTwice = engine.removeStandardErrors(off, "Product", Capability.LOOK_UP);
        String backOn = engine.adoptStandardErrors(off, "Product", Capability.LOOK_UP);

        assertEquals(parse(off), parse(offTwice), "removing absent answers must be a no-op");
        assertEquals(parse(born), parse(backOn), "off then on must restore the born document");
    }

    // UC5 leaves a FEAT-005-era inline 404 alone: only canonical references are removed —
    // refuse-don't-mangle for content the toggle doesn't own.
    @Test
    void removeStandardErrorsLeavesNonCanonicalAnswersAlone() throws Exception {
        String legacy = stripStandardErrors(addProduct(EnumSet.allOf(Capability.class)));

        JsonNode after = parse(engine.removeStandardErrors(legacy, "Product", Capability.REMOVE));

        assertEquals("No product with this id exists.", after.path("paths")
                .path("/products/{id}").path("delete").path("responses").path("404")
                .path("description").asText());
    }

    // Adopt is idempotent by design: re-adoption rewrites to the same canonical form.
    @Test
    void adoptStandardErrorsIsIdempotent() throws Exception {
        String legacy = stripStandardErrors(addProduct(EnumSet.allOf(Capability.class)));

        String once = engine.adoptStandardErrors(legacy, "Product", Capability.UPDATE);
        String twice = engine.adoptStandardErrors(once, "Product", Capability.UPDATE);

        assertEquals(parse(once), parse(twice));
    }

    // The dialect branch, extended to the furniture: identical output on every version.
    @ParameterizedTest
    @EnumSource(SpecVersion.class)
    void errorFurnitureSerializesIdenticallyAcrossSpecVersions(SpecVersion version) throws Exception {
        String empty = engine.createEmptyDocument(version, "Storefront API", null);
        JsonNode components = parse(engine.addResource(empty,
                CanonicalDerivation.derive("Product", EnumSet.allOf(Capability.class)), null))
                .path("components");

        JsonNode reference = parse(addProduct(EnumSet.allOf(Capability.class))).path("components");
        assertEquals(reference.path("schemas").path("Error"), components.path("schemas").path("Error"));
        assertEquals(reference.path("responses"), components.path("responses"));
    }

    // ------------------------------------------------------- FEAT-009: capabilityContract

    // AC1: identity, description, and the Answers facet in one projection — the label
    // preferring the summary, the success answer read from the document, failures present.
    @Test
    void capabilityContractProjectsIdentityAndAnswers() {
        String body = addProduct(EnumSet.allOf(Capability.class));

        CapabilityContractView contract =
                engine.capabilityContract(body, "Product", Capability.LOOK_UP);

        assertEquals(new CapabilityView(Capability.LOOK_UP, "Look up one product", "GET",
                "/products/{id}"), contract.identity());
        assertEquals(null, contract.description());
        assertEquals("product", contract.singularNoun());
        assertEquals("200", contract.answers().successStatus());
        assertEquals("The product.", contract.answers().successDescription());
        assertEquals(List.of(
                new FailureAnswerView("400", true),
                new FailureAnswerView("401", true),
                new FailureAnswerView("404", true),
                new FailureAnswerView("429", true),
                new FailureAnswerView("500", true)),
                contract.answers().failures());
    }

    // The summary and the operation description are the document's, not re-derived — edits
    // survive the projection (the FEAT-005 round-trip stance; FEAT-012 writes descriptions).
    @Test
    void capabilityContractPrefersTheDocumentsOwnWording() throws Exception {
        ObjectNode document = (ObjectNode) parse(addProduct(EnumSet.of(Capability.BROWSE)));
        ObjectNode get = (ObjectNode) document.path("paths").path("/products").path("get");
        get.put("summary", "See what's on the shelf");
        get.put("description", "Anyone can browse the catalog.");

        CapabilityContractView contract = engine.capabilityContract(
                mapper.writeValueAsString(document), "Product", Capability.BROWSE);

        assertEquals("See what's on the shelf", contract.identity().label());
        assertEquals("Anyone can browse the catalog.", contract.description());
    }

    // AC2: the Request facet is absent — not empty — where no input travels.
    @ParameterizedTest
    @EnumSource(value = Capability.class, names = {"BROWSE", "LOOK_UP", "REMOVE"})
    void capabilityContractOmitsTheRequestFacetWhereNoInputTravels(Capability capability) {
        String body = addProduct(EnumSet.of(capability));

        assertEquals(null, engine.capabilityContract(body, "Product", capability).request());
    }

    // AC5: Add derives the shape's fields (identity included, stated server-assigned via its
    // auto visibility); Update states merge-patch semantics and enumerates nothing.
    @Test
    void capabilityContractDerivesTheRequestFacet() {
        String body = engine.addField(
                addProduct(EnumSet.of(Capability.ADD, Capability.UPDATE)), "Product",
                field("name", CoreType.TEXT, null, false, true, FieldVisibility.NORMAL, null));

        RequestFacetView add = engine.capabilityContract(body, "Product", Capability.ADD).request();
        RequestFacetView update = engine.capabilityContract(body, "Product", Capability.UPDATE).request();

        assertFalse(add.mergePatch());
        assertEquals(List.of("id", "name"),
                add.fields().stream().map(FieldView::name).toList());
        assertEquals(FieldVisibility.AUTO, add.fields().getFirst().visibility());
        assertTrue(update.mergePatch());
        assertEquals(List.of(), update.fields());
    }

    // AC3: the derived content-negotiation line travels with every capability — and no
    // corresponding parameter exists in the document (addResource writes none).
    @Test
    void capabilityContractCarriesTheDerivedContentNegotiationLine() throws Exception {
        String body = addProduct(EnumSet.of(Capability.REMOVE));

        assertEquals(List.of(new HeaderLineView("Accept", "application/json", true)),
                engine.capabilityContract(body, "Product", Capability.REMOVE).headers().derived());
        assertFalse(parse(body).path("paths").path("/products/{id}").path("delete")
                .has("parameters"));
    }

    // AC4: a capability that predates this feature shows its standard answers as absent —
    // available to adopt, nothing written by the view.
    @Test
    void capabilityContractShowsLegacyAnswersAsAbsent() throws Exception {
        String legacy = stripStandardErrors(addProduct(EnumSet.allOf(Capability.class)));

        AnswersFacetView answers =
                engine.capabilityContract(legacy, "Product", Capability.UPDATE).answers();

        assertEquals("The updated product.", answers.successDescription());
        assertEquals(List.of(
                new FailureAnswerView("400", false),
                new FailureAnswerView("401", false),
                new FailureAnswerView("404", false),
                new FailureAnswerView("422", false),
                new FailureAnswerView("429", false),
                new FailureAnswerView("500", false)),
                answers.failures());
    }

    // AC9: the furniture is derived plumbing — never a datatype, resource, or capability in
    // the concept projection.
    @Test
    void projectionNeverListsTheErrorFurniture() {
        DocumentProjection projection = engine.project(addProduct(EnumSet.allOf(Capability.class)));

        assertEquals(List.of("Product"), projection.schemaNames());
        assertEquals(List.of("Product"),
                projection.resources().stream().map(ResourceView::name).toList());
    }

    // ------------------------------------------------------------------- FEAT-010: paging

    // AC1: Browse pages from birth — the two optional query parameters and the wrapper's
    // required pagination member, exactly per the contract (bounds, defaults, whole numbers).
    @Test
    void addResourceDerivesPagingFromBirth() throws Exception {
        JsonNode get = parse(addProduct(EnumSet.of(Capability.BROWSE)))
                .path("paths").path("/products").path("get");

        assertEquals(parse("""
                [{"name":"page","in":"query","description":"The page to return.",
                  "required":false,"schema":{"type":"integer","minimum":1,"default":1}},
                 {"name":"limit","in":"query","description":"How many results per page.",
                  "required":false,
                  "schema":{"type":"integer","minimum":1,"maximum":100,"default":20}}]"""),
                get.path("parameters"));
        JsonNode wrapper = get.path("responses").path("200")
                .path("content").path("application/json").path("schema");
        assertEquals(parse("""
                {"required":["page","limit","totalItems","totalPages"],
                 "type":"object",
                 "properties":{
                   "page":{"type":"integer","minimum":1,
                     "description":"The page this answer is for."},
                   "limit":{"type":"integer","minimum":1,
                     "description":"How many results per page."},
                   "totalItems":{"type":"integer","minimum":0,
                     "description":"How many results exist in all."},
                   "totalPages":{"type":"integer","minimum":0,
                     "description":"How many pages exist in all."}}}"""),
                wrapper.path("properties").path("pagination"));
        assertEquals(List.of("data", "pagination"), textsOf(wrapper.path("required")));
    }

    // Paging is a list concern: no other capability's operation gains parameters (the {id}
    // path parameter lives at the path-item level, not on operations).
    @ParameterizedTest
    @EnumSource(value = Capability.class, names = "BROWSE", mode = EnumSource.Mode.EXCLUDE)
    void addResourceDerivesNoPagingElsewhere(Capability capability) throws Exception {
        JsonNode document = parse(addProduct(EnumSet.of(capability)));
        DerivedOperation derived = CanonicalDerivation
                .derive("Product", EnumSet.of(capability)).operations().getFirst();

        assertFalse(document.path("paths").path(derived.path())
                .path(derived.method().toLowerCase()).has("parameters"));
    }

    // UC2/AC2: the opt-out removes exactly the paging constructs — the result is
    // byte-for-byte the born document minus page, limit, pagination, and its required entry;
    // data stays, the wrapper is never a bare array.
    @Test
    void disablePagingRemovesExactlyThePagingConstructs() throws Exception {
        String born = addProduct(EnumSet.allOf(Capability.class));

        JsonNode off = parse(engine.disablePaging(born, "Product", Capability.BROWSE));

        assertEquals(parse(stripPaging(born)), off);
        JsonNode wrapper = off.path("paths").path("/products").path("get")
                .path("responses").path("200").path("content").path("application/json")
                .path("schema");
        assertEquals(List.of("data"), keysOf(wrapper.path("properties")));
        assertEquals(List.of("data"), textsOf(wrapper.path("required")));
        assertFalse(off.path("paths").path("/products").path("get").has("parameters"),
                "no empty parameters leftover (AC2)");
        assertNoVendorExtensions(off);
    }

    // AC3: off then on restores the born document; disabling when off is a no-op; enabling
    // when on is idempotent.
    @Test
    void disablePagingRoundTripsWithEnable() throws Exception {
        String born = addProduct(EnumSet.of(Capability.BROWSE));

        String off = engine.disablePaging(born, "Product", Capability.BROWSE);
        String offTwice = engine.disablePaging(off, "Product", Capability.BROWSE);
        String backOn = engine.enablePaging(off, "Product", Capability.BROWSE);
        String onTwice = engine.enablePaging(backOn, "Product", Capability.BROWSE);

        assertEquals(parse(off), parse(offTwice), "disabling absent paging must be a no-op");
        assertEquals(parse(born), parse(backOn), "off then on must restore the born document");
        assertEquals(parse(backOn), parse(onTwice), "enable must be idempotent");
    }

    // UC4/AC5: adopting on a pre-FEAT-010 Browse writes exactly the paging constructs —
    // sibling operations and everything else stay untouched.
    @Test
    void enablePagingRetrofitsALegacyOperation() throws Exception {
        String legacy = stripPaging(addProduct(EnumSet.allOf(Capability.class)));
        JsonNode before = parse(legacy);

        JsonNode after = parse(engine.enablePaging(legacy, "Product", Capability.BROWSE));

        assertEquals(parse(addProduct(EnumSet.allOf(Capability.class))), after,
                "adoption must yield exactly the born-paged document");
        assertEquals(before.path("paths").path("/products").path("post"),
                after.path("paths").path("/products").path("post"));
        assertEquals(before.path("paths").path("/products/{id}"),
                after.path("paths").path("/products/{id}"));
        assertNoVendorExtensions(after);
    }

    // The Paging facet: on for a born Browse, absent (null) wherever paging doesn't apply.
    @Test
    void capabilityContractProjectsThePagingFacet() {
        String body = addProduct(EnumSet.allOf(Capability.class));

        assertEquals(new PagingFacetView(true, List.of()),
                engine.capabilityContract(body, "Product", Capability.BROWSE).paging());
        for (Capability other : EnumSet.complementOf(EnumSet.of(Capability.BROWSE))) {
            assertEquals(null, engine.capabilityContract(body, "Product", other).paging());
        }
    }

    // UC4: a pre-FEAT-010 Browse shows paging as available-but-absent — off, no conflicts.
    @Test
    void capabilityContractShowsLegacyPagingAsOff() throws Exception {
        String legacy = stripPaging(addProduct(EnumSet.of(Capability.BROWSE)));

        assertEquals(new PagingFacetView(false, List.of()),
                engine.capabilityContract(legacy, "Product", Capability.BROWSE).paging());
    }

    // UC5: a designer-authored query parameter claiming page/limit is named as a conflict —
    // paging reads off, and the claimant is what blocks enabling (AC6 is the service's rule).
    @Test
    void capabilityContractNamesAuthoredPageOrLimitAsConflicts() throws Exception {
        String legacy = stripPaging(addProduct(EnumSet.of(Capability.BROWSE)));
        ObjectNode document = (ObjectNode) parse(legacy);
        ObjectNode get = (ObjectNode) document.path("paths").path("/products").path("get");
        ObjectNode authored = get.putArray("parameters").addObject();
        authored.put("name", "page").put("in", "query");
        authored.putObject("schema").put("type", "string");

        assertEquals(new PagingFacetView(false, List.of("page")),
                engine.capabilityContract(mapper.writeValueAsString(document), "Product",
                        Capability.BROWSE).paging());
    }

    // ------------------------------------------------------------ FEAT-011: declarations

    // AC1: the operation gains exactly one query parameter — derived name, the kind's
    // serialization, required per the optionality choice, the description when given.
    @Test
    void addDeclarationWritesAQueryParameter() throws Exception {
        String body = engine.addDeclaration(addProduct(EnumSet.of(Capability.BROWSE)), "Product",
                Capability.BROWSE, DeclarationLocation.QUERY_PARAMETER,
                new DeclarationEdit("priceMax", scalar(CoreType.DECIMAL_NUMBER, null), false,
                        "The budget cap."));

        JsonNode parameters =
                parse(body).path("paths").path("/products").path("get").path("parameters");
        assertEquals(3, parameters.size(), "appended after the paging pair");
        assertEquals(parse("""
                {"name":"priceMax","in":"query","description":"The budget cap.",
                 "required":false,"schema":{"type":"number"}}"""),
                parameters.get(2));
        assertNoVendorExtensions(parse(body));
    }

    // AC1/AC7: a "one of" kind serializes as an inline value list on a text schema.
    @Test
    void addDeclarationWritesAOneOfQueryParameter() throws Exception {
        String body = engine.addDeclaration(addProduct(EnumSet.of(Capability.BROWSE)), "Product",
                Capability.BROWSE, DeclarationLocation.QUERY_PARAMETER,
                new DeclarationEdit("status",
                        new ParameterKind.OneOf(List.of("available", "pending", "sold")),
                        false, null));

        assertEquals(parse("""
                {"name":"status","in":"query","required":false,
                 "schema":{"type":"string","enum":["available","pending","sold"]}}"""),
                parse(body).path("paths").path("/products").path("get").path("parameters").get(2));
    }

    // AC2: a request header is the same guarantee with in: header.
    @Test
    void addDeclarationWritesARequestHeader() throws Exception {
        String body = engine.addDeclaration(addProduct(EnumSet.of(Capability.LOOK_UP)), "Product",
                Capability.LOOK_UP, DeclarationLocation.REQUEST_HEADER,
                new DeclarationEdit("Request-Id", scalar(CoreType.TEXT, Refinement.UUID), true,
                        "For tracing."));

        assertEquals(parse("""
                [{"name":"Request-Id","in":"header","description":"For tracing.",
                  "required":true,"schema":{"type":"string","format":"uuid"}}]"""),
                parse(body).path("paths").path("/products/{id}").path("get").path("parameters"));
    }

    // AC3: a response header lands on the success answer as a Header Object — and the shared
    // failure answers are byte-identical before and after.
    @Test
    void addDeclarationWritesAResponseHeaderOnTheSuccessAnswerOnly() throws Exception {
        String born = addProduct(EnumSet.of(Capability.BROWSE));

        String body = engine.addDeclaration(born, "Product", Capability.BROWSE,
                DeclarationLocation.RESPONSE_HEADER,
                new DeclarationEdit("Sync-Token", scalar(CoreType.TEXT, null), false,
                        "Where this listing left off."));

        JsonNode before = parse(born).path("paths").path("/products").path("get").path("responses");
        JsonNode after = parse(body).path("paths").path("/products").path("get").path("responses");
        assertEquals(parse("""
                {"Sync-Token":{"description":"Where this listing left off.",
                 "required":false,"schema":{"type":"string"}}}"""),
                after.path("200").path("headers"));
        for (String status : List.of("400", "401", "422", "429", "500")) {
            assertEquals(before.path(status), after.path(status),
                    status + " must be byte-identical (AC3)");
        }
    }

    // AC1/AC5 across all three locations: adding touches nothing else, and removing leaves
    // no other trace — add then remove is byte-for-byte the original, emptied containers
    // included (LOOK_UP's operation carries no parameters or headers at birth).
    @ParameterizedTest
    @EnumSource(DeclarationLocation.class)
    void addThenRemoveDeclarationRestoresTheOriginal(DeclarationLocation location) throws Exception {
        String born = addProduct(EnumSet.of(Capability.LOOK_UP));
        DeclarationEdit edit = new DeclarationEdit(
                location == DeclarationLocation.QUERY_PARAMETER ? "syncToken" : "Sync-Token",
                scalar(CoreType.TEXT, null), false, null);

        String added = engine.addDeclaration(born, "Product", Capability.LOOK_UP, location, edit);
        String removed = engine.removeDeclaration(added, "Product", Capability.LOOK_UP, location,
                edit.name());

        assertEquals(parse(born), parse(removed));
    }

    // AC5: a change rewrites in place — same position among its siblings, a rename and a
    // rekind included; the neighbors are untouched.
    @Test
    void updateDeclarationRewritesAQueryParameterInPlace() throws Exception {
        String body = engine.addDeclaration(addProduct(EnumSet.of(Capability.BROWSE)), "Product",
                Capability.BROWSE, DeclarationLocation.QUERY_PARAMETER,
                new DeclarationEdit("priceMax", scalar(CoreType.DECIMAL_NUMBER, null), false,
                        "The budget cap."));
        body = engine.addDeclaration(body, "Product", Capability.BROWSE,
                DeclarationLocation.QUERY_PARAMETER,
                new DeclarationEdit("status",
                        new ParameterKind.OneOf(List.of("available", "sold")), false, null));

        String updated = engine.updateDeclaration(body, "Product", Capability.BROWSE,
                DeclarationLocation.QUERY_PARAMETER, "priceMax",
                new DeclarationEdit("budget", scalar(CoreType.WHOLE_NUMBER, null), true, null));

        JsonNode parameters =
                parse(updated).path("paths").path("/products").path("get").path("parameters");
        assertEquals(parse("""
                {"name":"budget","in":"query","required":true,"schema":{"type":"integer"}}"""),
                parameters.get(2), "rewritten at its old position, stale constructs gone");
        assertEquals(parse(body).path("paths").path("/products").path("get").path("parameters")
                .get(3), parameters.get(3), "the neighbor is untouched");
    }

    @Test
    void updateDeclarationRewritesAResponseHeaderInPlace() throws Exception {
        String body = engine.addDeclaration(addProduct(EnumSet.of(Capability.LOOK_UP)), "Product",
                Capability.LOOK_UP, DeclarationLocation.RESPONSE_HEADER,
                new DeclarationEdit("Sync-Token", scalar(CoreType.TEXT, null), false, null));
        body = engine.addDeclaration(body, "Product", Capability.LOOK_UP,
                DeclarationLocation.RESPONSE_HEADER,
                new DeclarationEdit("Request-Id", scalar(CoreType.TEXT, Refinement.UUID), false,
                        null));

        String updated = engine.updateDeclaration(body, "Product", Capability.LOOK_UP,
                DeclarationLocation.RESPONSE_HEADER, "Sync-Token",
                new DeclarationEdit("Listing-Token", scalar(CoreType.TEXT, null), false, null));

        assertEquals(List.of("Listing-Token", "Request-Id"),
                keysOf(parse(updated).path("paths").path("/products/{id}").path("get")
                        .path("responses").path("200").path("headers")),
                "renamed in place, not re-appended");
    }

    // The contract projects every authored declaration in its facet — kinds read back through
    // the table ("one of" included), the paging pair excluded from Filters while paging owns it.
    @Test
    void capabilityContractProjectsAuthoredDeclarations() {
        String body = addProduct(EnumSet.of(Capability.BROWSE));
        body = engine.addDeclaration(body, "Product", Capability.BROWSE,
                DeclarationLocation.QUERY_PARAMETER,
                new DeclarationEdit("status",
                        new ParameterKind.OneOf(List.of("available", "pending", "sold")),
                        false, null));
        body = engine.addDeclaration(body, "Product", Capability.BROWSE,
                DeclarationLocation.REQUEST_HEADER,
                new DeclarationEdit("Request-Id", scalar(CoreType.TEXT, Refinement.UUID), true,
                        "For tracing."));
        body = engine.addDeclaration(body, "Product", Capability.BROWSE,
                DeclarationLocation.RESPONSE_HEADER,
                // required on a response header is the "always sent" promise.
                new DeclarationEdit("Sync-Token", scalar(CoreType.TEXT, null), true, null));

        CapabilityContractView contract =
                engine.capabilityContract(body, "Product", Capability.BROWSE);

        assertEquals(List.of(new DeclarationView("status",
                new ParameterKind.OneOf(List.of("available", "pending", "sold")), false, null)),
                contract.queryParameters(), "the paging pair is the Paging facet's, not a filter");
        assertEquals(List.of(new HeaderLineView("Accept", "application/json", true)),
                contract.headers().derived());
        assertEquals(List.of(new DeclarationView("Request-Id",
                scalar(CoreType.TEXT, Refinement.UUID), true, "For tracing.")),
                contract.headers().authored());
        assertEquals(List.of(new DeclarationView("Sync-Token", scalar(CoreType.TEXT, null),
                true, null)), contract.answers().successHeaders());
    }

    // With paging off, an authored page/limit is a filter like any other — and the named
    // conflict that blocks re-enabling (the pagingFacet test's other half).
    @Test
    void capabilityContractShowsAuthoredPageAsAFilterWhilePagingIsOff() {
        String off = engine.disablePaging(addProduct(EnumSet.of(Capability.BROWSE)), "Product",
                Capability.BROWSE);
        String body = engine.addDeclaration(off, "Product", Capability.BROWSE,
                DeclarationLocation.QUERY_PARAMETER,
                new DeclarationEdit("page", scalar(CoreType.TEXT, null), false, null));

        CapabilityContractView contract =
                engine.capabilityContract(body, "Product", Capability.BROWSE);

        assertEquals(List.of("page"),
                contract.queryParameters().stream().map(DeclarationView::name).toList());
        assertEquals(new PagingFacetView(false, List.of("page")), contract.paging());
    }

    // Declarations not yet authored are empty facets — present (they always apply), not null.
    @Test
    void capabilityContractProjectsEmptyDeclarationFacetsAtBirth() {
        CapabilityContractView contract = engine.capabilityContract(
                addProduct(EnumSet.of(Capability.BROWSE)), "Product", Capability.BROWSE);

        assertEquals(List.of(), contract.queryParameters());
        assertEquals(List.of(), contract.headers().authored());
        assertEquals(List.of(), contract.answers().successHeaders());
    }

    private static ParameterKind.Scalar scalar(CoreType core, Refinement refinement) {
        return new ParameterKind.Scalar(new FieldKind(core, refinement, false));
    }

    /**
     * A pre-FEAT-010 document: the paging constructs stripped from every Browse — what
     * derivation produced before this feature shipped.
     */
    private String stripPaging(String body) throws Exception {
        ObjectNode document = (ObjectNode) parse(body);
        ObjectNode get = (ObjectNode) document.path("paths").path("/products").path("get");
        get.remove("parameters");
        ObjectNode wrapper = (ObjectNode) get.path("responses").path("200")
                .path("content").path("application/json").path("schema");
        ((ObjectNode) wrapper.path("properties")).remove("pagination");
        wrapper.putArray("required").add("data");
        return mapper.writeValueAsString(document);
    }

    /**
     * A pre-FEAT-009 document: the shared furniture removed and the born failure answers
     * stripped back to FEAT-005's inline noun-specific 404 — what derivation produced before
     * this feature shipped.
     */
    private String stripStandardErrors(String body) throws Exception {
        ObjectNode document = (ObjectNode) parse(body);
        ((ObjectNode) document.path("components")).remove("responses");
        ((ObjectNode) document.path("components").path("schemas")).remove("Error");
        for (JsonNode pathItem : document.path("paths")) {
            for (String method : List.of("get", "post", "patch", "delete")) {
                if (!pathItem.has(method)) {
                    continue;
                }
                ObjectNode responses = (ObjectNode) pathItem.path(method).path("responses");
                for (StandardErrors.Answer answer : StandardErrors.Answer.values()) {
                    responses.remove(answer.status());
                }
            }
        }
        JsonNode item = document.path("paths").path("/products/{id}");
        for (String method : List.of("get", "patch", "delete")) {
            if (item.has(method)) {
                ((ObjectNode) item.path(method).path("responses")).putObject("404")
                        .put("description", "No product with this id exists.");
            }
        }
        return mapper.writeValueAsString(document);
    }

    private static FieldEdit field(String propertyName, CoreType core, Refinement refinement,
            boolean list, boolean required, FieldVisibility visibility, String description) {
        return new FieldEdit(propertyName, new FieldKind(core, refinement, list), required,
                visibility, description);
    }

    private static List<String> keysOf(JsonNode object) {
        List<String> keys = new java.util.ArrayList<>();
        object.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

    private static List<String> textsOf(JsonNode array) {
        List<String> texts = new java.util.ArrayList<>();
        array.forEach(node -> texts.add(node.asText()));
        return texts;
    }

    private static List<String> requiredOf(JsonNode document, String schemaName) {
        List<String> required = new java.util.ArrayList<>();
        document.path("components").path("schemas").path(schemaName).path("required")
                .forEach(node -> required.add(node.asText()));
        return required;
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
