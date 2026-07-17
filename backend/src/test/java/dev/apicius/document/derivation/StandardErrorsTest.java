package dev.apicius.document.derivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.apicius.document.derivation.StandardErrors.Answer;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Plain unit test: FEAT-009's applicability table, pinned per capability. */
class StandardErrorsTest {

    // The table: 400/401/429/500 everywhere; 404 on {id} paths; 422 where input goes beyond
    // the id (Add, Update, Browse) — ascending status order.
    @Test
    void appliesTheTablePerCapability() {
        assertEquals(List.of(Answer.BAD_REQUEST, Answer.UNAUTHORIZED,
                Answer.UNPROCESSABLE_ENTITY, Answer.TOO_MANY_REQUESTS,
                Answer.INTERNAL_SERVER_ERROR),
                StandardErrors.applicableTo(Capability.BROWSE, false));
        assertEquals(List.of(Answer.BAD_REQUEST, Answer.UNAUTHORIZED,
                Answer.UNPROCESSABLE_ENTITY, Answer.TOO_MANY_REQUESTS,
                Answer.INTERNAL_SERVER_ERROR),
                StandardErrors.applicableTo(Capability.ADD, false));
        assertEquals(List.of(Answer.BAD_REQUEST, Answer.UNAUTHORIZED, Answer.NOT_FOUND,
                Answer.TOO_MANY_REQUESTS, Answer.INTERNAL_SERVER_ERROR),
                StandardErrors.applicableTo(Capability.LOOK_UP, true));
        assertEquals(List.of(Answer.BAD_REQUEST, Answer.UNAUTHORIZED, Answer.NOT_FOUND,
                Answer.UNPROCESSABLE_ENTITY, Answer.TOO_MANY_REQUESTS,
                Answer.INTERNAL_SERVER_ERROR),
                StandardErrors.applicableTo(Capability.UPDATE, true));
        assertEquals(List.of(Answer.BAD_REQUEST, Answer.UNAUTHORIZED, Answer.NOT_FOUND,
                Answer.TOO_MANY_REQUESTS, Answer.INTERNAL_SERVER_ERROR),
                StandardErrors.applicableTo(Capability.REMOVE, true));
    }

    // AC9's reservation is case-insensitive, matching every other name-conflict rule.
    @Test
    void reservesTheErrorSchemaNameCaseInsensitively() {
        assertTrue(StandardErrors.isReservedSchemaName("Error"));
        assertTrue(StandardErrors.isReservedSchemaName("ERROR"));
        assertTrue(StandardErrors.isReservedSchemaName("error"));
        assertFalse(StandardErrors.isReservedSchemaName("Errors"));
    }
}
