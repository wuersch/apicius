package dev.apicius.document.apitomy;

import dev.apicius.document.CapabilityView;
import dev.apicius.document.DocumentEngine;
import dev.apicius.document.DocumentProjection;
import dev.apicius.document.ResourceView;
import dev.apicius.document.SpecVersion;
import dev.apicius.document.derivation.CanonicalDerivation;
import dev.apicius.document.derivation.Capability;
import dev.apicius.document.derivation.DerivedOperation;
import dev.apicius.document.derivation.ResourceDerivation;
import io.apitomy.datamodels.Library;
import io.apitomy.datamodels.models.Referenceable;
import io.apitomy.datamodels.models.Schema;
import io.apitomy.datamodels.models.ModelType;
import io.apitomy.datamodels.models.openapi.OpenApiInfo;
import io.apitomy.datamodels.models.openapi.OpenApiMediaType;
import io.apitomy.datamodels.models.openapi.OpenApiOperation;
import io.apitomy.datamodels.models.openapi.OpenApiParameter;
import io.apitomy.datamodels.models.openapi.OpenApiParametersParent;
import io.apitomy.datamodels.models.openapi.OpenApiPathItem;
import io.apitomy.datamodels.models.openapi.OpenApiPaths;
import io.apitomy.datamodels.models.openapi.OpenApiResponse;
import io.apitomy.datamodels.models.openapi.OpenApiResponses;
import io.apitomy.datamodels.models.openapi.OpenApiSchema;
import io.apitomy.datamodels.models.openapi.v3x.OpenApi3xComponents;
import io.apitomy.datamodels.models.openapi.v3x.OpenApi3xDocument;
import io.apitomy.datamodels.models.openapi.v3x.OpenApi3xMediaType;
import io.apitomy.datamodels.models.openapi.v3x.OpenApi3xOperation;
import io.apitomy.datamodels.models.openapi.v3x.OpenApi3xPathItem;
import io.apitomy.datamodels.models.openapi.v3x.OpenApi3xRequestBody;
import io.apitomy.datamodels.models.openapi.v3x.OpenApi3xResponse;
import io.apitomy.datamodels.models.openapi.v3x.OpenApi3xSchema;
import io.apitomy.datamodels.models.openapi.v3x.v30.OpenApi30Schema;
import io.apitomy.datamodels.models.openapi.v3x.v31.OpenApi31Schema;
import io.apitomy.datamodels.models.openapi.v3x.v32.OpenApi32Schema;
import io.apitomy.datamodels.models.union.StringUnionValueImpl;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The {@code apitomy-data-models} adapter (ADR-0009) — the only class allowed to import
 * {@code io.apitomy.*}.
 */
@ApplicationScoped
public class ApitomyDocumentEngine implements DocumentEngine {

    private static final String JSON = "application/json";
    /** Update's request media type (RFC 7386) — the one non-JSON-plain body in ADR-0010. */
    private static final String MERGE_PATCH_JSON = "application/merge-patch+json";

    @Override
    public String createEmptyDocument(SpecVersion version, String title, String description) {
        OpenApi3xDocument document = (OpenApi3xDocument) Library.createDocument(toModelType(version));
        // Pin the exact latest patch (FEAT-003 AC1/AC3) — Apicius policy, not the library default.
        document.setOpenapi(version.latestPatch());
        OpenApiInfo info = document.createInfo();
        info.setTitle(title);
        info.setVersion("1.0.0");
        if (description != null) {
            info.setDescription(description);
        }
        document.setInfo(info);
        return Library.writeDocumentToJSONString(document);
    }

    @Override
    public String addResource(String body, ResourceDerivation derivation, String description) {
        OpenApi3xDocument document = (OpenApi3xDocument) Library.readDocumentFromJSONString(body);

        OpenApi3xComponents components = document.getComponents();
        if (components == null) {
            components = document.createComponents();
            document.setComponents(components);
        }
        components.addSchema(derivation.schemaName(), resourceSchema(components, description));

        OpenApiPaths paths = document.getPaths();
        if (paths == null) {
            paths = document.createPaths();
            document.setPaths(paths);
        }
        // A path item exists only when a chosen operation lives on it (AC4).
        for (Map.Entry<String, List<DerivedOperation>> entry : byPath(derivation).entrySet()) {
            OpenApi3xPathItem pathItem = (OpenApi3xPathItem) paths.createPathItem();
            paths.addItem(entry.getKey(), pathItem);
            if (entry.getKey().equals(derivation.itemPath())) {
                addIdParameter(pathItem);
            }
            for (DerivedOperation derived : entry.getValue()) {
                attach(pathItem, derived, buildOperation(pathItem, derived, derivation));
            }
        }
        return Library.writeDocumentToJSONString(document);
    }

    @Override
    public DocumentProjection project(String body) {
        OpenApi3xDocument document = (OpenApi3xDocument) Library.readDocumentFromJSONString(body);
        Map<String, OpenApiSchema> schemas = document.getComponents() == null
                ? Map.of() : document.getComponents().getSchemas();
        OpenApiPaths paths = document.getPaths();

        List<ResourceView> resources = new ArrayList<>();
        for (Map.Entry<String, OpenApiSchema> schema : schemas.entrySet()) {
            recognize(schema.getKey(), schema.getValue(), paths).ifPresent(resources::add);
        }
        return new DocumentProjection(
                List.copyOf(schemas.keySet()),
                paths == null ? List.of() : List.copyOf(paths.getItemNames()),
                resources);
    }

    /** Ordered by first appearance, so collection precedes item like the ADR-0010 table. */
    private static Map<String, List<DerivedOperation>> byPath(ResourceDerivation derivation) {
        Map<String, List<DerivedOperation>> byPath = new LinkedHashMap<>();
        for (DerivedOperation derived : derivation.operations()) {
            byPath.computeIfAbsent(derived.path(), path -> new ArrayList<>()).add(derived);
        }
        return byPath;
    }

    /** The resource's schema: its identity house rule and nothing more — fields come later. */
    private static OpenApiSchema resourceSchema(OpenApi3xComponents components, String description) {
        OpenApiSchema schema = components.createSchema();
        if (description != null) {
            schema.setDescription(description);
        }
        setType(schema, "object");
        Schema id = schema.createSchema();
        setType(id, "string");
        // The server assigns identity: readOnly keeps id out of what clients send, so one
        // schema serves request and response bodies (ADR-0010).
        id.setReadOnly(true);
        schema.addProperty("id", id);
        schema.setRequired(List.of("id"));
        return schema;
    }

    private OpenApi3xOperation buildOperation(OpenApi3xPathItem pathItem, DerivedOperation derived,
            ResourceDerivation derivation) {
        OpenApi3xOperation operation = (OpenApi3xOperation) pathItem.createOperation();
        operation.setSummary(derived.label());
        operation.setOperationId(derived.operationId());

        switch (derived.capability()) {
            case ADD -> addRequestBody(operation, JSON, derivation.schemaName());
            case UPDATE -> addRequestBody(operation, MERGE_PATCH_JSON, derivation.schemaName());
            default -> { }
        }

        OpenApiResponses responses = operation.createResponses();
        operation.setResponses(responses);
        OpenApi3xResponse success = (OpenApi3xResponse) responses.createResponse();
        success.setDescription(derived.successDescription());
        switch (derived.capability()) {
            case BROWSE -> addContent(success, pagedWrapper(success, derivation.schemaName()));
            case LOOK_UP, ADD, UPDATE -> addContent(success, refSchema(success.createMediaType(), derivation.schemaName()));
            case REMOVE -> { } // 204 — empty by design
        }
        responses.addItem(derived.successStatus(), success);

        // Item-path operations declare the 404; description only — the error-body model is a
        // separate future house rule (ADR-0010).
        if (derived.path().equals(derivation.itemPath())) {
            OpenApiResponse notFound = responses.createResponse();
            notFound.setDescription(derivation.notFoundDescription());
            responses.addItem("404", notFound);
        }
        return operation;
    }

    private static void attach(OpenApi3xPathItem pathItem, DerivedOperation derived,
            OpenApi3xOperation operation) {
        switch (derived.method()) {
            case "GET" -> pathItem.setGet(operation);
            case "POST" -> pathItem.setPost(operation);
            case "PATCH" -> pathItem.setPatch(operation);
            case "DELETE" -> pathItem.setDelete(operation);
            default -> throw new IllegalArgumentException("Underived method: " + derived.method());
        }
    }

    private static void addRequestBody(OpenApi3xOperation operation, String mediaType, String schemaName) {
        OpenApi3xRequestBody body = operation.createRequestBody();
        body.setRequired(true);
        body.addContent(mediaType, refSchema(body.createMediaType(), schemaName));
        operation.setRequestBody(body);
    }

    private static void addContent(OpenApi3xResponse response, OpenApiMediaType mediaType) {
        response.addContent(JSON, (OpenApi3xMediaType) mediaType);
    }

    /**
     * Browse's 200: an <em>inline</em> {@code {items: [X]}} wrapper, never a bare array or a
     * named schema — evolvable (page fields can be added compatibly) without polluting
     * {@code components/schemas} with derived plumbing (ADR-0010).
     */
    private static OpenApiMediaType pagedWrapper(OpenApi3xResponse response, String schemaName) {
        OpenApiMediaType mediaType = response.createMediaType();
        OpenApiSchema wrapper = mediaType.createSchema();
        setType(wrapper, "object");
        Schema items = wrapper.createSchema();
        setType(items, "array");
        Schema itemRef = wrapper.createSchema();
        ((Referenceable) itemRef).set$ref("#/components/schemas/" + schemaName);
        ((OpenApi3xSchema) items).setItems((OpenApi3xSchema) itemRef);
        wrapper.addProperty("items", items);
        mediaType.setSchema(wrapper);
        return mediaType;
    }

    private static OpenApiMediaType refSchema(OpenApiMediaType mediaType, String schemaName) {
        OpenApiSchema ref = mediaType.createSchema();
        ((Referenceable) ref).set$ref("#/components/schemas/" + schemaName);
        mediaType.setSchema(ref);
        return mediaType;
    }

    /** The {@code {id}} path parameter, once at the path-item level — required for a valid document. */
    private static void addIdParameter(OpenApi3xPathItem pathItem) {
        OpenApiParametersParent parent = (OpenApiParametersParent) pathItem;
        OpenApiParameter parameter = parent.createParameter();
        parameter.setName("id");
        parameter.setIn("path");
        parameter.setRequired(true);
        OpenApiSchema schema = parameter.createSchema();
        setType(schema, "string");
        parameter.setSchema(schema);
        parent.addParameter(parameter);
    }

    /**
     * The 3.0 schema dialect types with a plain string; 3.1/3.2 (JSON Schema 2020-12) allow
     * string-or-array, so their generated models take a union. Everything FEAT-005 writes
     * serializes identically across versions — this branch is the seed of a dialect-aware
     * schema writer the day some construct's <em>output</em> must differ (see patterns.md).
     */
    private static void setType(Schema schema, String type) {
        if (schema instanceof OpenApi30Schema v30) {
            v30.setType(type);
        } else if (schema instanceof OpenApi31Schema v31) {
            v31.setType(new StringUnionValueImpl(type));
        } else {
            ((OpenApi32Schema) schema).setType(new StringUnionValueImpl(type));
        }
    }

    /**
     * Derivation inverted (ADR-0010): a schema is a resource when at least one canonical
     * operation sits at its derived path. PascalCase is a lossy encoding of the noun's words,
     * so each candidate segmentation is tried against the document's actual paths — the
     * writer's path-uniqueness rule means at most one candidate can match. The label prefers
     * the operation's {@code summary}; a missing summary degrades only the label, never the
     * structural match.
     */
    private static Optional<ResourceView> recognize(String schemaName, OpenApiSchema schema,
            OpenApiPaths paths) {
        if (paths == null) {
            return Optional.empty();
        }
        for (List<String> words : CanonicalDerivation.recognitionCandidates(schemaName)) {
            List<CapabilityView> capabilities = capabilitiesAt(
                    CanonicalDerivation.derive(String.join(" ", words), EnumSet.allOf(Capability.class)),
                    paths);
            if (!capabilities.isEmpty()) {
                return Optional.of(new ResourceView(schemaName, schema.getDescription(), capabilities));
            }
        }
        return Optional.empty();
    }

    private static List<CapabilityView> capabilitiesAt(ResourceDerivation derivation, OpenApiPaths paths) {
        List<CapabilityView> capabilities = new ArrayList<>();
        for (DerivedOperation derived : derivation.operations()) {
            OpenApiPathItem pathItem = paths.getItem(derived.path());
            OpenApiOperation operation = pathItem == null ? null : operationAt(pathItem, derived.method());
            if (operation == null) {
                continue;
            }
            String summary = operation.getSummary();
            String label = summary == null || summary.isBlank() ? derived.label() : summary;
            capabilities.add(new CapabilityView(derived.capability(), label, derived.method(), derived.path()));
        }
        return capabilities;
    }

    private static OpenApiOperation operationAt(OpenApiPathItem pathItem, String method) {
        return switch (method) {
            case "GET" -> pathItem.getGet();
            case "POST" -> pathItem.getPost();
            case "PATCH" -> pathItem.getPatch();
            case "DELETE" -> pathItem.getDelete();
            default -> null;
        };
    }

    private static ModelType toModelType(SpecVersion version) {
        return switch (version) {
            case V3_0 -> ModelType.OPENAPI30;
            case V3_1 -> ModelType.OPENAPI31;
            case V3_2 -> ModelType.OPENAPI32;
        };
    }
}
