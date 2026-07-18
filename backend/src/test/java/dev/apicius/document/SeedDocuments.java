package dev.apicius.document;

import dev.apicius.document.apitomy.ApitomyDocumentEngine;
import dev.apicius.document.derivation.CanonicalDerivation;
import dev.apicius.document.derivation.Capability;
import dev.apicius.document.derivation.CoreType;
import dev.apicius.document.derivation.DeclarationEdit;
import dev.apicius.document.derivation.DeclarationLocation;
import dev.apicius.document.derivation.FieldEdit;
import dev.apicius.document.derivation.FieldKind;
import dev.apicius.document.derivation.FieldVisibility;
import dev.apicius.document.derivation.ParameterKind;
import dev.apicius.document.derivation.Refinement;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Regenerates the dev-seed spec bodies for {@code src/main/resources/import.sql} by composing
 * the real engine — the same guarantee the seed header states: bodies are engine-written, never
 * hand-authored. Run after any feature changes what a canonical document looks like:
 *
 * <pre>
 *   ./mvnw -q test-compile exec:java -Dexec.mainClass=dev.apicius.document.SeedDocuments \
 *       -Dexec.classpathScope=test
 * </pre>
 *
 * and paste each printed line into the matching {@code body} column. The four APIs are a
 * deliberate variability matrix — every shipped state appears somewhere and is absent somewhere
 * else: Storefront shows everything on (paging, standard errors, authored declarations, custom
 * capability/answer notes), Billing a single errors opt-out, Fleet a paging opt-out with mostly
 * ghost descriptions, Notifications the fully-opted-out "before" state.
 */
public final class SeedDocuments {

    private static final DocumentEngine ENGINE = new ApitomyDocumentEngine();

    private SeedDocuments() {
    }

    public static void main(String[] args) {
        print("Storefront API", storefront());
        print("Billing API", billing());
        print("Fleet API", fleet());
        print("Notifications API", notifications());
    }

    // Ada's flagship: every shipped state switched on, with deliberate gaps (undescribed
    // capabilities, an undescribed field) so ghost invitations stay visible too.
    private static String storefront() {
        String body = ENGINE.createEmptyDocument(SpecVersion.V3_1, "Storefront API", null);
        body = ENGINE.updateApiDescription(body, "Sell products online.");

        body = resource(body, "Product", "Something you sell.", ALL);
        body = field(body, "Product", "name", CoreType.TEXT, null, true,
                "The display name shoppers see.");
        body = field(body, "Product", "price", CoreType.DECIMAL_NUMBER, null, true,
                "Unit price in USD.");
        body = field(body, "Product", "inStock", CoreType.YES_NO, null, false, null);
        body = ENGINE.addField(body, "Product", new FieldEdit("tags",
                new FieldKind(CoreType.TEXT, null, true), false, FieldVisibility.NORMAL,
                "Free-form labels for search."));
        body = ENGINE.addField(body, "Product", new FieldEdit("createdAt",
                new FieldKind(CoreType.DATE_TIME, null, false), false, FieldVisibility.AUTO,
                null));

        body = resource(body, "Order", "A purchase placed by a customer.", ALL);
        body = field(body, "Order", "status", CoreType.TEXT, null, true,
                "Where the order is in its lifecycle.");
        body = field(body, "Order", "total", CoreType.DECIMAL_NUMBER, null, true,
                "Grand total in USD.");
        body = ENGINE.addField(body, "Order", new FieldEdit("placedAt",
                new FieldKind(CoreType.DATE_TIME, null, false), false, FieldVisibility.AUTO,
                null));

        body = resource(body, "Customer", "Someone who buys.", ALL);
        body = field(body, "Customer", "email", CoreType.TEXT, Refinement.EMAIL, true,
                "Where receipts go.");
        body = field(body, "Customer", "fullName", CoreType.TEXT, null, true, null);
        body = ENGINE.addField(body, "Customer", new FieldEdit("password",
                new FieldKind(CoreType.TEXT, Refinement.PASSWORD, false), false,
                FieldVisibility.WRITE_ONLY, null));

        body = resource(body, "Category", "How products are grouped.",
                EnumSet.of(Capability.BROWSE, Capability.LOOK_UP, Capability.ADD));
        body = field(body, "Category", "name", CoreType.TEXT, null, true, null);

        body = resource(body, "Review", "What shoppers think of a product.",
                EnumSet.of(Capability.BROWSE, Capability.ADD, Capability.REMOVE));
        body = field(body, "Review", "rating", CoreType.WHOLE_NUMBER, null, true,
                "1 to 5 stars.");
        body = field(body, "Review", "comment", CoreType.TEXT, null, false, null);

        // FEAT-011: authored declarations on Browse products — a described "one of" filter,
        // an undescribed scalar one, a required request header, an always-sent response header.
        body = ENGINE.addDeclaration(body, "Product", Capability.BROWSE,
                DeclarationLocation.QUERY_PARAMETER, new DeclarationEdit("status",
                        new ParameterKind.OneOf(List.of("available", "pending", "sold")),
                        false, "Narrow the list to one lifecycle state."));
        body = ENGINE.addDeclaration(body, "Product", Capability.BROWSE,
                DeclarationLocation.QUERY_PARAMETER, new DeclarationEdit("priceMax",
                        scalar(CoreType.DECIMAL_NUMBER, null), false, null));
        body = ENGINE.addDeclaration(body, "Product", Capability.LOOK_UP,
                DeclarationLocation.REQUEST_HEADER, new DeclarationEdit("Request-Id",
                        scalar(CoreType.TEXT, Refinement.UUID), true, "For tracing."));
        body = ENGINE.addDeclaration(body, "Product", Capability.BROWSE,
                DeclarationLocation.RESPONSE_HEADER, new DeclarationEdit("Sync-Token",
                        scalar(CoreType.TEXT, null), true, "Where this listing left off."));

        // FEAT-012: one capability described, one answer in the designer's words — the rest
        // stay at ghosts/derived defaults on purpose.
        body = ENGINE.updateCapabilityDescription(body, "Product", Capability.BROWSE,
                "Anyone can browse the catalog — no sign-in needed. Results come back 20 at "
                        + "a time, newest first.");
        body = ENGINE.updateSuccessAnswerDescription(body, "Product", Capability.LOOK_UP,
                "The product, with its current stock state.");
        return body;
    }

    // Grace's mature API: defaults on, except one deliberate standard-errors opt-out.
    private static String billing() {
        String body = ENGINE.createEmptyDocument(SpecVersion.V3_1, "Billing API", null);
        body = ENGINE.updateApiDescription(body, "Invoices, payments & refunds.");

        body = resource(body, "Invoice", "A bill a customer owes.", ALL);
        body = field(body, "Invoice", "number", CoreType.TEXT, null, true,
                "Human-readable invoice number.");
        body = field(body, "Invoice", "amount", CoreType.DECIMAL_NUMBER, null, true,
                "Amount due in USD.");
        body = field(body, "Invoice", "dueOn", CoreType.DATE, null, false, null);
        body = field(body, "Invoice", "paid", CoreType.YES_NO, null, false, null);

        body = resource(body, "Payment", "Money received against an invoice.", ALL);
        body = field(body, "Payment", "amount", CoreType.DECIMAL_NUMBER, null, true, null);
        body = field(body, "Payment", "method", CoreType.TEXT, null, false,
                "How the customer paid.");
        body = ENGINE.addField(body, "Payment", new FieldEdit("capturedAt",
                new FieldKind(CoreType.DATE_TIME, null, false), false, FieldVisibility.AUTO,
                null));

        body = resource(body, "Refund", "Money returned to a customer.", ALL);
        body = field(body, "Refund", "amount", CoreType.DECIMAL_NUMBER, null, true, null);
        body = field(body, "Refund", "reason", CoreType.TEXT, null, false, null);

        body = resource(body, "CreditNote", "A correction issued against an invoice.",
                EnumSet.of(Capability.BROWSE, Capability.LOOK_UP, Capability.ADD));
        body = field(body, "CreditNote", "amount", CoreType.DECIMAL_NUMBER, null, true, null);

        // Described declarations, and the one opt-out: removing a refund answers without the
        // standard failure contract (the dashed-chips state).
        body = ENGINE.addDeclaration(body, "Invoice", Capability.BROWSE,
                DeclarationLocation.QUERY_PARAMETER, new DeclarationEdit("dueBefore",
                        scalar(CoreType.DATE, null), false,
                        "Only invoices due on or before this day."));
        body = ENGINE.addDeclaration(body, "Payment", Capability.ADD,
                DeclarationLocation.REQUEST_HEADER, new DeclarationEdit("Idempotency-Key",
                        scalar(CoreType.TEXT, Refinement.UUID), true,
                        "Send the same key to retry safely."));
        body = ENGINE.removeStandardErrors(body, "Refund", Capability.REMOVE);
        return body;
    }

    // Ada's young API: paging deliberately off on Browse vehicles; descriptions mostly absent
    // so the ghost invitations show.
    private static String fleet() {
        String body = ENGINE.createEmptyDocument(SpecVersion.V3_1, "Fleet API", null);
        body = ENGINE.updateApiDescription(body, "Vehicles, trips & drivers.");

        body = resource(body, "Vehicle", "A car or van in the fleet.", ALL);
        body = field(body, "Vehicle", "plate", CoreType.TEXT, null, true, "License plate.");
        body = field(body, "Vehicle", "seats", CoreType.WHOLE_NUMBER, Refinement.INT32, false,
                null);
        body = field(body, "Vehicle", "active", CoreType.YES_NO, null, false, null);

        body = resource(body, "Trip", "One journey by one vehicle.", EnumSet.of(
                Capability.BROWSE, Capability.LOOK_UP, Capability.ADD, Capability.REMOVE));
        body = field(body, "Trip", "distanceKm", CoreType.DECIMAL_NUMBER, Refinement.DOUBLE,
                false, null);
        body = field(body, "Trip", "startedAt", CoreType.DATE_TIME, null, true, null);

        body = resource(body, "Driver", "Someone licensed to drive fleet vehicles.",
                EnumSet.of(Capability.BROWSE, Capability.LOOK_UP, Capability.ADD));
        body = field(body, "Driver", "fullName", CoreType.TEXT, null, true, null);
        body = field(body, "Driver", "hiredOn", CoreType.DATE, null, false, null);

        body = ENGINE.disablePaging(body, "Vehicle", Capability.BROWSE);
        return body;
    }

    // Grace's draft: the fully-opted-out "before" state — errors off everywhere, paging off,
    // nothing described beyond the API itself.
    private static String notifications() {
        String body = ENGINE.createEmptyDocument(SpecVersion.V3_1, "Notifications API", null);
        body = ENGINE.updateApiDescription(body, "Email, SMS & push messages.");

        body = resource(body, "Message", null, ALL);
        body = field(body, "Message", "subject", CoreType.TEXT, null, true, null);
        body = field(body, "Message", "body", CoreType.TEXT, null, false, null);
        body = ENGINE.addField(body, "Message", new FieldEdit("channels",
                new FieldKind(CoreType.TEXT, null, true), false, FieldVisibility.NORMAL, null));
        body = ENGINE.addField(body, "Message", new FieldEdit("sentAt",
                new FieldKind(CoreType.DATE_TIME, null, false), false, FieldVisibility.AUTO,
                null));

        body = resource(body, "Template", null,
                EnumSet.of(Capability.BROWSE, Capability.LOOK_UP, Capability.ADD));
        body = field(body, "Template", "name", CoreType.TEXT, null, true, null);
        body = field(body, "Template", "body", CoreType.TEXT, null, true, null);

        for (Capability capability : ALL) {
            body = ENGINE.removeStandardErrors(body, "Message", capability);
        }
        for (Capability capability : EnumSet.of(Capability.BROWSE, Capability.LOOK_UP,
                Capability.ADD)) {
            body = ENGINE.removeStandardErrors(body, "Template", capability);
        }
        body = ENGINE.disablePaging(body, "Message", Capability.BROWSE);
        body = ENGINE.disablePaging(body, "Template", Capability.BROWSE);
        return body;
    }

    private static final Set<Capability> ALL = EnumSet.allOf(Capability.class);

    private static String resource(String body, String noun, String description,
            Set<Capability> capabilities) {
        return ENGINE.addResource(body,
                CanonicalDerivation.derive(spaced(noun), capabilities), description);
    }

    /** {@code CreditNote} → {@code Credit Note} — derive() takes the dialog's spaced words. */
    private static String spaced(String pascal) {
        return pascal.replaceAll("(?<=[a-z])(?=[A-Z])", " ");
    }

    private static String field(String body, String schemaName, String propertyName,
            CoreType core, Refinement refinement, boolean required, String description) {
        return ENGINE.addField(body, schemaName, new FieldEdit(propertyName,
                new FieldKind(core, refinement, false), required, FieldVisibility.NORMAL,
                description));
    }

    private static ParameterKind.Scalar scalar(CoreType core, Refinement refinement) {
        return new ParameterKind.Scalar(new FieldKind(core, refinement, false));
    }

    private static void print(String title, String body) {
        DocumentProjection projection = ENGINE.project(body);
        int operations = projection.resources().stream()
                .mapToInt(resource -> resource.capabilities().size()).sum();
        System.out.println("-- " + title + " · " + projection.resources().size()
                + " resources · " + operations + " operations");
        System.out.println(compact(body).replace("'", "''"));
        System.out.println();
    }

    /** One line per spec in import.sql; Jackson keeps key order, so nothing else moves. */
    private static String compact(String body) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(mapper.readTree(body));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
