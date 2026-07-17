package dev.apicius.document.derivation;

import java.util.ArrayList;
import java.util.List;

/**
 * FEAT-009's standard error answers as executable code — the single source shared by birth
 * derivation (new operations carry the answers from the start), adoption (retrofitting an
 * older operation), and structural detection (the contract projection's present/absent
 * state), so they can never disagree: {@link CanonicalDerivation}'s pattern, applied to
 * failures. Pure and deterministic; no engine types.
 */
public final class StandardErrors {

    /**
     * The shared failure shape's schema name. Reserved: the furniture is derived plumbing,
     * never user data (FEAT-009 AC9), so no resource or datatype may take this name — a later
     * adoption would otherwise wire failure bodies to user data.
     */
    public static final String ERROR_SCHEMA_NAME = "Error";

    /**
     * The six standard failure answers, ascending by status. {@code responseName} is the
     * reusable response's key under {@code components/responses}; {@code description} is the
     * document's plain-language wording — noun-neutral by construction, since the response is
     * shared. The UI's punchier names ("not signed in", "our fault") are frontend vocabulary.
     */
    public enum Answer {
        BAD_REQUEST("400", "BadRequest", "The request couldn't be read."),
        UNAUTHORIZED("401", "Unauthorized", "Not signed in."),
        NOT_FOUND("404", "NotFound", "No resource with this id exists."),
        UNPROCESSABLE_ENTITY("422", "UnprocessableEntity", "The input is invalid."),
        TOO_MANY_REQUESTS("429", "TooManyRequests", "Too many requests."),
        INTERNAL_SERVER_ERROR("500", "InternalServerError", "Something went wrong on our side.");

        private final String status;
        private final String responseName;
        private final String description;

        Answer(String status, String responseName, String description) {
            this.status = status;
            this.responseName = responseName;
            this.description = description;
        }

        public String status() {
            return status;
        }

        public String responseName() {
            return responseName;
        }

        public String description() {
            return description;
        }
    }

    private StandardErrors() {
    }

    /**
     * The applicability table (FEAT-009): 400/401/429/500 on every operation; 404 on
     * operations addressing one resource (the item path); 422 on operations accepting input
     * beyond the id (Add, Update, Browse). Ascending status order.
     */
    public static List<Answer> applicableTo(Capability capability, boolean itemPath) {
        List<Answer> answers = new ArrayList<>();
        answers.add(Answer.BAD_REQUEST);
        answers.add(Answer.UNAUTHORIZED);
        if (itemPath) {
            answers.add(Answer.NOT_FOUND);
        }
        if (capability == Capability.ADD || capability == Capability.UPDATE
                || capability == Capability.BROWSE) {
            answers.add(Answer.UNPROCESSABLE_ENTITY);
        }
        answers.add(Answer.TOO_MANY_REQUESTS);
        answers.add(Answer.INTERNAL_SERVER_ERROR);
        return List.copyOf(answers);
    }

    /** The reservation backing AC9, case-insensitive like every other name-conflict rule. */
    public static boolean isReservedSchemaName(String schemaName) {
        return ERROR_SCHEMA_NAME.equalsIgnoreCase(schemaName);
    }
}
