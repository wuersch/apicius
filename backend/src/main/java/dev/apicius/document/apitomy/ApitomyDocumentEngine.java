package dev.apicius.document.apitomy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.apicius.document.AnswersFacetView;
import dev.apicius.document.CapabilityContractView;
import dev.apicius.document.CapabilityView;
import dev.apicius.document.DocumentEngine;
import dev.apicius.document.FailureAnswerView;
import dev.apicius.document.HeaderLineView;
import dev.apicius.document.PagingFacetView;
import dev.apicius.document.RequestFacetView;
import dev.apicius.document.DocumentProjection;
import dev.apicius.document.FieldView;
import dev.apicius.document.ResourceView;
import dev.apicius.document.SpecVersion;
import dev.apicius.document.derivation.CanonicalDerivation;
import dev.apicius.document.derivation.Capability;
import dev.apicius.document.derivation.CoreType;
import dev.apicius.document.derivation.DerivedOperation;
import dev.apicius.document.derivation.FieldEdit;
import dev.apicius.document.derivation.FieldKind;
import dev.apicius.document.derivation.FieldVisibility;
import dev.apicius.document.derivation.Paging;
import dev.apicius.document.derivation.ResourceDerivation;
import dev.apicius.document.derivation.StandardErrors;
import io.apitomy.datamodels.Library;
import io.apitomy.datamodels.models.Document;
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
import io.apitomy.datamodels.models.util.JsonUtil;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        return serialize(document);
    }

    @Override
    public String updateInfo(String body, String title, String description, String version) {
        OpenApi3xDocument document = (OpenApi3xDocument) Library.readDocumentFromJSONString(body);
        // Mutate the existing info in place — recreating it would drop unmodeled members
        // (contact, license, extensions) and violate the lossless rule (PRIN-003, AC1).
        OpenApiInfo info = infoOf(document);
        info.setTitle(title);
        info.setVersion(version);
        info.setDescription(description);
        return serialize(document);
    }

    @Override
    public String retitle(String body, String title) {
        OpenApi3xDocument document = (OpenApi3xDocument) Library.readDocumentFromJSONString(body);
        infoOf(document).setTitle(title);
        return serialize(document);
    }

    /** Every Apicius-born document has an info; a pathological one gains an empty one to edit. */
    private static OpenApiInfo infoOf(OpenApi3xDocument document) {
        OpenApiInfo info = document.getInfo();
        if (info == null) {
            info = document.createInfo();
            document.setInfo(info);
        }
        return info;
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
        // Every operation answers the standard failures (FEAT-009 AC8), so the shared
        // furniture is ensured up front — created on the first write, reused ever after (AC7).
        ensureErrorFurniture(components);

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
        return serialize(document);
    }

    @Override
    public String addField(String body, String schemaName, FieldEdit field) {
        OpenApi3xDocument document = (OpenApi3xDocument) Library.readDocumentFromJSONString(body);
        OpenApiSchema schema = schemaOf(document, schemaName);
        // New fields append — document order is preserved as-is (FEAT-006 non-goal).
        schema.addProperty(field.propertyName(), propertySchema(schema, field));
        rewriteRequired(schema, null, field.propertyName(), field.required());
        return serialize(document);
    }

    @Override
    public String updateField(String body, String schemaName, String propertyName, FieldEdit field) {
        OpenApi3xDocument document = (OpenApi3xDocument) Library.readDocumentFromJSONString(body);
        OpenApiSchema schema = schemaOf(document, schemaName);
        // Rewritten in place (AC6): a fresh schema at the old position, whether or not the
        // name changed — replacing wholesale sidesteps clearing stale constructs one by one.
        int position = List.copyOf(schema.getProperties().keySet()).indexOf(propertyName);
        schema.removeProperty(propertyName);
        schema.insertProperty(field.propertyName(), propertySchema(schema, field), position);
        rewriteRequired(schema, propertyName, field.propertyName(), field.required());
        return serialize(document);
    }

    @Override
    public String removeField(String body, String schemaName, String propertyName) {
        OpenApi3xDocument document = (OpenApi3xDocument) Library.readDocumentFromJSONString(body);
        OpenApiSchema schema = schemaOf(document, schemaName);
        schema.removeProperty(propertyName);
        rewriteRequired(schema, propertyName, null, false);
        return serialize(document);
    }

    @Override
    public CapabilityContractView capabilityContract(String body, String schemaName,
            Capability capability) {
        OpenApi3xDocument document = (OpenApi3xDocument) Library.readDocumentFromJSONString(body);
        Located located = locate(document, schemaName, capability);
        OpenApi3xOperation operation = located.operation();
        DerivedOperation derived = located.derived();

        OpenApiResponses responses = operation.getResponses();
        OpenApiResponse success =
                responses == null ? null : responses.getItem(derived.successStatus());
        List<FailureAnswerView> failures = new ArrayList<>();
        for (StandardErrors.Answer answer : StandardErrors.applicableTo(capability,
                isItemPath(derived, located.derivation()))) {
            OpenApiResponse answered = responses == null ? null : responses.getItem(answer.status());
            failures.add(new FailureAnswerView(answer.status(), isStandardReference(answered, answer)));
        }

        return new CapabilityContractView(
                new CapabilityView(capability, labelOf(operation, derived), derived.method(),
                        derived.path()),
                operation.getDescription(),
                located.derivation().singularNoun(),
                requestFacet(document, schemaName, capability),
                pagingFacet(operation, derived, capability),
                List.of(new HeaderLineView("Accept", JSON, true)),
                new AnswersFacetView(derived.successStatus(),
                        success == null ? derived.successDescription() : success.getDescription(),
                        List.copyOf(failures)));
    }

    @Override
    public String adoptStandardErrors(String body, String schemaName, Capability capability) {
        OpenApi3xDocument document = (OpenApi3xDocument) Library.readDocumentFromJSONString(body);
        Located located = locate(document, schemaName, capability);

        OpenApi3xComponents components = document.getComponents();
        if (components == null) {
            components = document.createComponents();
            document.setComponents(components);
        }
        ensureErrorFurniture(components);
        referenceStandardAnswers(located.operation(), StandardErrors.applicableTo(capability,
                isItemPath(located.derived(), located.derivation())));
        return serialize(document);
    }

    /** The applicability table's one document-shape input: does this operation address one resource? */
    private static boolean isItemPath(DerivedOperation derived, ResourceDerivation derivation) {
        return derived.path().equals(derivation.itemPath());
    }

    /** The label rule, single-sourced: the summary carries it; a missing one falls back derived. */
    private static String labelOf(OpenApiOperation operation, DerivedOperation derived) {
        String summary = operation.getSummary();
        return summary == null || summary.isBlank() ? derived.label() : summary;
    }

    @Override
    public String removeStandardErrors(String body, String schemaName, Capability capability) {
        OpenApi3xDocument document = (OpenApi3xDocument) Library.readDocumentFromJSONString(body);
        Located located = locate(document, schemaName, capability);
        OpenApiResponses responses = located.operation().getResponses();
        if (responses != null) {
            for (StandardErrors.Answer answer : StandardErrors.applicableTo(capability,
                    isItemPath(located.derived(), located.derivation()))) {
                if (isStandardReference(responses.getItem(answer.status()), answer)) {
                    responses.removeItem(answer.status());
                }
            }
        }
        return serialize(document);
    }

    @Override
    public String enablePaging(String body, String schemaName, Capability capability) {
        OpenApi3xDocument document = (OpenApi3xDocument) Library.readDocumentFromJSONString(body);
        Located located = locate(document, schemaName, capability);
        writePagingConstructs(located.operation(), located.derived());
        return serialize(document);
    }

    @Override
    public String disablePaging(String body, String schemaName, Capability capability) {
        OpenApi3xDocument document = (OpenApi3xDocument) Library.readDocumentFromJSONString(body);
        Located located = locate(document, schemaName, capability);
        if (isPaged(located.operation(), located.derived())) {
            removePagingConstructs(located.operation(), located.derived());
        }
        return serialize(document);
    }

    /**
     * The structural on-state (FEAT-010 AC7): every paging construct present — both query
     * parameters and the wrapper's {@code pagination} member. Presence of the constructs is
     * the state, like {@link #isStandardReference}: no marker, no extension.
     */
    private static boolean isPaged(OpenApi3xOperation operation, DerivedOperation derived) {
        OpenApiSchema wrapper = wrapperSchemaOf(operation, derived);
        return queryParameter(operation, Paging.PAGE_PARAMETER) != null
                && queryParameter(operation, Paging.LIMIT_PARAMETER) != null
                && wrapper != null && wrapper.getProperties() != null
                && wrapper.getProperties().containsKey(Paging.PAGINATION_MEMBER);
    }

    /**
     * The Paging facet (FEAT-010) — only where paging applies (Browse), {@code null}
     * otherwise. Off with a {@code page}/{@code limit} query parameter present means a
     * designer-authored parameter claims the name (UC5): named as a conflict, blocking enable.
     */
    private static PagingFacetView pagingFacet(OpenApi3xOperation operation,
            DerivedOperation derived, Capability capability) {
        if (!Paging.appliesTo(capability)) {
            return null;
        }
        boolean on = isPaged(operation, derived);
        List<String> conflicts = new ArrayList<>();
        if (!on) {
            for (String name : List.of(Paging.PAGE_PARAMETER, Paging.LIMIT_PARAMETER)) {
                if (queryParameter(operation, name) != null) {
                    conflicts.add(name);
                }
            }
        }
        return new PagingFacetView(on, List.copyOf(conflicts));
    }

    /** The operation's query parameter of that name, or null — path parameters never match. */
    private static OpenApiParameter queryParameter(OpenApi3xOperation operation, String name) {
        List<OpenApiParameter> parameters = ((OpenApiParametersParent) operation).getParameters();
        if (parameters == null) {
            return null;
        }
        for (OpenApiParameter parameter : parameters) {
            if (name.equals(parameter.getName()) && "query".equals(parameter.getIn())) {
                return parameter;
            }
        }
        return null;
    }

    /** Browse's success wrapper — the inline {@code {data: [X]}} object the paging member joins. */
    private static OpenApiSchema wrapperSchemaOf(OpenApi3xOperation operation,
            DerivedOperation derived) {
        OpenApiResponses responses = operation.getResponses();
        OpenApiResponse success =
                responses == null ? null : responses.getItem(derived.successStatus());
        if (success == null) {
            return null;
        }
        Map<String, OpenApi3xMediaType> content = ((OpenApi3xResponse) success).getContent();
        OpenApiMediaType mediaType = content == null ? null : content.get(JSON);
        return mediaType == null ? null : mediaType.getSchema();
    }

    /**
     * Writes the full paging contract (FEAT-010) onto a list operation: the two optional
     * query parameters and the wrapper's required {@code pagination} member, remove-then-add
     * per construct — idempotent, and non-canonical content at those spots is replaced (the
     * adopt precedent). Shared by birth derivation and {@link #enablePaging} so they can
     * never disagree.
     */
    private static void writePagingConstructs(OpenApi3xOperation operation,
            DerivedOperation derived) {
        removeQueryParameters(operation);
        OpenApiParametersParent parent = (OpenApiParametersParent) operation;
        parent.addParameter(pagingParameter(parent, Paging.PAGE_PARAMETER,
                "The page to return.", Paging.PAGE_MINIMUM, null, Paging.PAGE_DEFAULT));
        parent.addParameter(pagingParameter(parent, Paging.LIMIT_PARAMETER,
                "How many results per page.", Paging.LIMIT_MINIMUM, Paging.LIMIT_MAXIMUM,
                Paging.LIMIT_DEFAULT));

        OpenApiSchema wrapper = wrapperSchemaOf(operation, derived);
        wrapper.removeProperty(Paging.PAGINATION_MEMBER);
        wrapper.addProperty(Paging.PAGINATION_MEMBER, paginationSchema(wrapper));
        List<String> required = wrapper.getRequired() == null
                ? new ArrayList<>() : new ArrayList<>(wrapper.getRequired());
        if (!required.contains(Paging.PAGINATION_MEMBER)) {
            required.add(Paging.PAGINATION_MEMBER);
        }
        wrapper.setRequired(required);
    }

    /** The opt-out's exact removal set (FEAT-010 AC2) — data and everything else untouched. */
    private static void removePagingConstructs(OpenApi3xOperation operation,
            DerivedOperation derived) {
        removeQueryParameters(operation);
        OpenApiSchema wrapper = wrapperSchemaOf(operation, derived);
        wrapper.removeProperty(Paging.PAGINATION_MEMBER);
        if (wrapper.getRequired() != null) {
            List<String> required = new ArrayList<>(wrapper.getRequired());
            required.remove(Paging.PAGINATION_MEMBER);
            wrapper.setRequired(required.isEmpty() ? null : required);
        }
    }

    /**
     * Drops the operation's {@code page}/{@code limit} query parameters — and the parameters
     * key itself once empty, so opting out leaves no empty leftover (AC2). Only reachable when
     * the parameters are the canonical constructs: a designer-authored claimant blocks enable
     * (UC5), and disable is a no-op while paging is off.
     */
    private static void removeQueryParameters(OpenApi3xOperation operation) {
        OpenApiParametersParent parent = (OpenApiParametersParent) operation;
        for (String name : List.of(Paging.PAGE_PARAMETER, Paging.LIMIT_PARAMETER)) {
            OpenApiParameter parameter = queryParameter(operation, name);
            if (parameter != null) {
                parent.removeParameter(parameter);
            }
        }
        if (parent.getParameters() != null && parent.getParameters().isEmpty()) {
            parent.clearParameters();
        }
    }

    /** One paging query parameter: optional, whole number, document-declared bounds/default. */
    private static OpenApiParameter pagingParameter(OpenApiParametersParent parent, String name,
            String description, int minimum, Integer maximum, int defaultValue) {
        OpenApiParameter parameter = parent.createParameter();
        parameter.setName(name);
        parameter.setIn("query");
        parameter.setDescription(description);
        parameter.setRequired(false);
        OpenApiSchema schema = parameter.createSchema();
        setType(schema, "integer");
        schema.setMinimum(minimum);
        if (maximum != null) {
            schema.setMaximum(maximum);
        }
        schema.setDefault(IntNode.valueOf(defaultValue));
        parameter.setSchema(schema);
        return parameter;
    }

    /** The pagination member, per the modern-petstore reference: four required whole numbers. */
    private static Schema paginationSchema(OpenApiSchema wrapper) {
        Schema pagination = wrapper.createSchema();
        setType(pagination, "object");
        pagination.addProperty("page", boundedInteger(wrapper, "The page this answer is for.",
                Paging.PAGE_MINIMUM));
        pagination.addProperty("limit", boundedInteger(wrapper, "How many results per page.",
                Paging.LIMIT_MINIMUM));
        pagination.addProperty("totalItems", boundedInteger(wrapper,
                "How many results exist in all.", 0));
        pagination.addProperty("totalPages", boundedInteger(wrapper,
                "How many pages exist in all.", 0));
        pagination.setRequired(Paging.PAGINATION_FIELDS);
        return pagination;
    }

    private static Schema boundedInteger(OpenApiSchema parent, String description, int minimum) {
        Schema property = describedProperty(parent, "integer", description);
        property.setMinimum(minimum);
        return property;
    }

    /** One capability's operation plus the derivation that locates it. */
    private record Located(ResourceDerivation derivation, DerivedOperation derived,
            OpenApi3xOperation operation) {
    }

    /**
     * {@link #recognize} narrowed to one capability: each candidate segmentation of the schema
     * name is derived and checked against the document's actual paths. That a match exists is
     * the caller's (the service's) verified rule, like {@link #schemaOf}.
     */
    private static Located locate(OpenApi3xDocument document, String schemaName,
            Capability capability) {
        OpenApiPaths paths = document.getPaths();
        for (List<String> words : CanonicalDerivation.recognitionCandidates(schemaName)) {
            ResourceDerivation derivation =
                    CanonicalDerivation.derive(String.join(" ", words), EnumSet.of(capability));
            DerivedOperation derived = derivation.operations().getFirst();
            OpenApiPathItem pathItem = paths == null ? null : paths.getItem(derived.path());
            OpenApiOperation operation =
                    pathItem == null ? null : operationAt(pathItem, derived.method());
            if (operation != null) {
                return new Located(derivation, derived, (OpenApi3xOperation) operation);
            }
        }
        throw new IllegalArgumentException(
                "'" + schemaName + "' has no derived operation for " + capability);
    }

    /**
     * The Request facet (FEAT-009 UC2, AC5), derived from the resource's shape — Add sends the
     * fields (the identity's auto visibility states server-assigned), Update states merge-patch
     * semantics; every other capability takes no body, so the facet is absent (AC2).
     */
    private static RequestFacetView requestFacet(OpenApi3xDocument document, String schemaName,
            Capability capability) {
        return switch (capability) {
            case ADD -> new RequestFacetView(false, fieldsOf(schemaOf(document, schemaName)));
            case UPDATE -> new RequestFacetView(true, List.of());
            default -> null;
        };
    }

    /** Present iff the status answers with the canonical shared reference (FEAT-009 AC4). */
    private static boolean isStandardReference(OpenApiResponse response,
            StandardErrors.Answer answer) {
        return response != null && ("#/components/responses/" + answer.responseName())
                .equals(((Referenceable) response).get$ref());
    }

    /**
     * The shared error furniture (FEAT-009): the {@code Error} schema and the six reusable
     * responses, created only where absent — first write creates, every later one reuses (AC7).
     */
    private static void ensureErrorFurniture(OpenApi3xComponents components) {
        Map<String, OpenApiSchema> schemas = components.getSchemas();
        if (schemas == null || !schemas.containsKey(StandardErrors.ERROR_SCHEMA_NAME)) {
            components.addSchema(StandardErrors.ERROR_SCHEMA_NAME, errorSchema(components));
        }
        for (StandardErrors.Answer answer : StandardErrors.Answer.values()) {
            Map<String, OpenApiResponse> responses = components.getResponses();
            if (responses == null || !responses.containsKey(answer.responseName())) {
                components.addResponse(answer.responseName(), standardResponse(components, answer));
            }
        }
    }

    /**
     * The one shared failure shape — RFC 9457 problem details, served as problem+json. Field
     * for field the modern-petstore reference shape
     * ({@code docs/misc/examples/modern-petstore-3.2.openapi}).
     */
    private static OpenApiSchema errorSchema(OpenApi3xComponents components) {
        OpenApiSchema schema = components.createSchema();
        setType(schema, "object");
        schema.setDescription("A problem detail (RFC 9457) describing why a request failed.");
        schema.addProperty("type", uriProperty(schema,
                "A URI reference identifying the type of problem."));
        schema.addProperty("title", describedProperty(schema, "string",
                "A short, human-readable summary of the problem type."));
        schema.addProperty("status", describedProperty(schema, "integer",
                "The HTTP status code."));
        schema.addProperty("detail", describedProperty(schema, "string",
                "A human-readable explanation specific to this occurrence."));
        schema.addProperty("instance", uriProperty(schema,
                "A URI reference identifying this specific occurrence."));
        Schema errors = describedProperty(schema, "array",
                "Field-level problems, present when the input failed validation.");
        Schema problem = schema.createSchema();
        setType(problem, "object");
        problem.addProperty("field", describedProperty(schema, "string",
                "The input field the problem is about."));
        problem.addProperty("message", describedProperty(schema, "string",
                "What is wrong with the field."));
        problem.addProperty("code", describedProperty(schema, "string",
                "A machine-readable code for the kind of problem."));
        problem.setRequired(List.of("field", "message"));
        ((OpenApi3xSchema) errors).setItems((OpenApi3xSchema) problem);
        schema.addProperty("errors", errors);
        schema.setRequired(List.of("type", "title", "status"));
        return schema;
    }

    private static Schema uriProperty(OpenApiSchema parent, String description) {
        Schema property = describedProperty(parent, "string", description);
        property.setFormat("uri");
        return property;
    }

    private static Schema describedProperty(OpenApiSchema parent, String type, String description) {
        Schema property = parent.createSchema();
        setType(property, type);
        property.setDescription(description);
        return property;
    }

    /** One reusable failure answer: its plain-language description and the problem+json body. */
    private static OpenApiResponse standardResponse(OpenApi3xComponents components,
            StandardErrors.Answer answer) {
        OpenApi3xResponse response = (OpenApi3xResponse) components.createResponse();
        response.setDescription(answer.description());
        response.addContent("application/problem+json", (OpenApi3xMediaType) refSchema(
                response.createMediaType(), StandardErrors.ERROR_SCHEMA_NAME));
        return response;
    }

    /**
     * Wires every applicable failure status to its shared response (FEAT-009). Remove-then-add
     * keeps the output canonical and replaces whatever sat at the status before — FEAT-005's
     * inline 404 included, which is the deliberate "replace" of the adopt decision. A
     * re-adoption therefore rewrites to the same form: adopt is idempotent.
     */
    private static void referenceStandardAnswers(OpenApi3xOperation operation,
            List<StandardErrors.Answer> applicable) {
        OpenApiResponses responses = operation.getResponses();
        for (StandardErrors.Answer answer : applicable) {
            responses.removeItem(answer.status());
            OpenApiResponse reference = responses.createResponse();
            ((Referenceable) reference).set$ref("#/components/responses/" + answer.responseName());
            responses.addItem(answer.status(), reference);
        }
    }

    @Override
    public DocumentProjection project(String body) {
        OpenApi3xDocument document = (OpenApi3xDocument) Library.readDocumentFromJSONString(body);
        Map<String, OpenApiSchema> schemas = document.getComponents() == null
                ? Map.of() : document.getComponents().getSchemas();
        OpenApiPaths paths = document.getPaths();

        // The error furniture is derived plumbing, never user data (FEAT-009 AC9): its
        // reserved name is excluded from the concept projection entirely — the service's
        // reservation guard, not this list, keeps the name unoccupiable.
        List<ResourceView> resources = new ArrayList<>();
        for (Map.Entry<String, OpenApiSchema> schema : schemas.entrySet()) {
            if (StandardErrors.isReservedSchemaName(schema.getKey())) {
                continue;
            }
            recognize(schema.getKey(), schema.getValue(), paths).ifPresent(resources::add);
        }
        return new DocumentProjection(
                schemas.keySet().stream()
                        .filter(name -> !StandardErrors.isReservedSchemaName(name))
                        .toList(),
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

    /**
     * The identity house rule as a field (ADR-0010): the server assigns identity, so auto
     * visibility ({@code readOnly}) keeps {@code id} out of what clients send and one schema
     * serves request and response bodies. Projected like any other field (FEAT-006 AC11).
     */
    private static final FieldEdit IDENTITY_FIELD =
            new FieldEdit("id", new FieldKind(CoreType.TEXT, null, false), true,
                    FieldVisibility.AUTO, null);

    /** The resource's schema: its identity house rule and nothing more — fields are edits. */
    private static OpenApiSchema resourceSchema(OpenApi3xComponents components, String description) {
        OpenApiSchema schema = components.createSchema();
        if (description != null) {
            schema.setDescription(description);
        }
        setType(schema, "object");
        schema.addProperty("id", propertySchema(schema, IDENTITY_FIELD));
        schema.setRequired(List.of("id"));
        return schema;
    }

    /** One property, serialized per ADR-0011's table — the single writer for field constructs. */
    private static Schema propertySchema(OpenApiSchema parent, FieldEdit field) {
        Schema property = parent.createSchema();
        if (field.kind().list()) {
            setType(property, "array");
            Schema items = parent.createSchema();
            writeScalar(items, field.kind());
            ((OpenApi3xSchema) property).setItems((OpenApi3xSchema) items);
        } else {
            writeScalar(property, field.kind());
        }
        if (field.description() != null) {
            property.setDescription(field.description());
        }
        // Visibility attaches to the field, list or not — readOnly is the same mechanism
        // ADR-0010's id uses; the single-value model keeps both from ever appearing (AC10).
        switch (field.visibility()) {
            case AUTO -> property.setReadOnly(true);
            case WRITE_ONLY -> ((OpenApi3xSchema) property).setWriteOnly(true);
            case NORMAL -> { }
        }
        return property;
    }

    private static void writeScalar(Schema schema, FieldKind kind) {
        setType(schema, kind.serializedType());
        if (kind.serializedFormat() != null) {
            schema.setFormat(kind.serializedFormat());
        }
    }

    /**
     * {@code required} membership follows the field (AC6, AC8): the one edited entry is
     * replaced, added, or dropped in place; every other member keeps its position. Null out
     * an emptied list — {@code required: []} is invalid in the 3.0 dialect.
     */
    private static void rewriteRequired(OpenApiSchema schema, String oldName, String newName,
            boolean required) {
        List<String> current = schema.getRequired() == null ? List.of() : schema.getRequired();
        List<String> updated = new ArrayList<>();
        boolean present = false;
        for (String name : current) {
            if (name.equals(oldName)) {
                present = true;
                if (required) {
                    updated.add(newName);
                }
            } else {
                updated.add(name);
            }
        }
        if (!present && required && newName != null) {
            updated.add(newName);
        }
        schema.setRequired(updated.isEmpty() ? null : updated);
    }

    /** The named schema; that it exists is the caller's (the service's) verified rule. */
    private static OpenApiSchema schemaOf(OpenApi3xDocument document, String schemaName) {
        return document.getComponents().getSchemas().get(schemaName);
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

        if (Paging.appliesTo(derived.capability())) {
            writePagingConstructs(operation, derived); // FEAT-010 AC1: lists page from birth.
        }
        referenceStandardAnswers(operation, StandardErrors.applicableTo(derived.capability(),
                isItemPath(derived, derivation)));
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
     * Browse's 200: an <em>inline</em> {@code {data: [X]}} wrapper — {@code data} required,
     * never a bare array or a named schema — evolvable (FEAT-010's {@code pagination} member
     * arrives compatibly) without polluting {@code components/schemas} with derived plumbing
     * (FEAT-005 § Derivation, keyed per the modern-petstore reference).
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
        wrapper.addProperty("data", items);
        wrapper.setRequired(List.of("data"));
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
                return Optional.of(new ResourceView(schemaName, schema.getDescription(),
                        capabilities, fieldsOf(schema)));
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
            capabilities.add(new CapabilityView(derived.capability(), labelOf(operation, derived),
                    derived.method(), derived.path()));
        }
        return capabilities;
    }

    /**
     * The shape's fields, in document order, read back through ADR-0011's table — for fields,
     * "recognition is derivation inverted" is a direct reverse lookup (the property name is
     * the identity; nothing lossy to segment). A {@code (type, format)} outside the table
     * cannot occur in an Apicius-authored document; until import (FEAT-004) owns displaying
     * foreign pairs as-is, such a property is skipped rather than mangled (PRIN-003).
     */
    private static List<FieldView> fieldsOf(OpenApiSchema schema) {
        Map<String, Schema> properties = schema.getProperties();
        if (properties == null) {
            return List.of();
        }
        List<String> required = schema.getRequired() == null ? List.of() : schema.getRequired();
        List<FieldView> fields = new ArrayList<>();
        for (Map.Entry<String, Schema> property : properties.entrySet()) {
            fieldOf(property.getKey(), property.getValue(),
                    required.contains(property.getKey())).ifPresent(fields::add);
        }
        return fields;
    }

    private static Optional<FieldView> fieldOf(String name, Schema property, boolean required) {
        boolean list = "array".equals(typeOf(property));
        Schema scalar = list ? ((OpenApi3xSchema) property).getItems() : property;
        if (scalar == null) {
            return Optional.empty();
        }
        return FieldKind.recognizeScalar(typeOf(scalar), scalar.getFormat())
                .map(kind -> new FieldKind(kind.core(), kind.refinement(), list))
                .map(kind -> new FieldView(name, kind, required, visibilityOf(property),
                        property.getDescription()));
    }

    /** {@link #setType}'s read half: the 3.0 plain string, or the 3.1/3.2 union's string. */
    private static String typeOf(Schema schema) {
        if (schema instanceof OpenApi30Schema v30) {
            return v30.getType();
        }
        var type = schema instanceof OpenApi31Schema v31
                ? v31.getType() : ((OpenApi32Schema) schema).getType();
        return type == null || !type.isString() ? null : type.asString();
    }

    private static FieldVisibility visibilityOf(Schema property) {
        if (Boolean.TRUE.equals(property.isReadOnly())) {
            return FieldVisibility.AUTO;
        }
        if (Boolean.TRUE.equals(((OpenApi3xSchema) property).isWriteOnly())) {
            return FieldVisibility.WRITE_ONLY;
        }
        return FieldVisibility.NORMAL;
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

    /** The schema keywords the library models as {@code Number} (see {@link #serialize}). */
    private static final Set<String> NUMERIC_KEYWORDS =
            Set.of("minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf");

    /**
     * The one serialization chokepoint. The library writes every {@code Number}-typed keyword
     * via {@code doubleValue()} ({@code JsonUtil.setNumberProperty}), so a whole-number bound
     * decays to {@code 1.0} on every write — FEAT-010's paging bounds and an import's own
     * integer bounds alike. Whole-valued bounds are normalized back to whole numbers: same
     * validation semantics, and the serialized document reads like the hand-written reference.
     */
    private static String serialize(Document document) {
        ObjectNode json = Library.writeDocument(document);
        normalizeWholeBounds(json);
        return JsonUtil.stringify(json);
    }

    private static void normalizeWholeBounds(JsonNode node) {
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            for (String key : keyList(object)) {
                JsonNode value = object.get(key);
                if (NUMERIC_KEYWORDS.contains(key) && value.isDouble()
                        && value.doubleValue() == Math.rint(value.doubleValue())
                        && !Double.isInfinite(value.doubleValue())) {
                    object.set(key, LongNode.valueOf((long) value.doubleValue()));
                } else {
                    normalizeWholeBounds(value);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                normalizeWholeBounds(child);
            }
        }
    }

    private static List<String> keyList(ObjectNode object) {
        List<String> keys = new ArrayList<>();
        object.fieldNames().forEachRemaining(keys::add);
        return keys;
    }
}
