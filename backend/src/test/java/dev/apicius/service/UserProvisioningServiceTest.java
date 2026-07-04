package dev.apicius.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.apicius.domain.AppUser;
import dev.apicius.test.CleanDatabaseTest;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserProvisioningServiceTest extends CleanDatabaseTest {

    @Inject
    UserProvisioningService service;

    // AC2 edge case: two first-requests race on uq_app_user_oidc_subject — the loser
    // must reuse the winner's row instead of failing or duplicating it.
    @Test
    void lostFirstProvisioningRaceReusesWinnersRow() {
        QuarkusTransaction.requiringNew().run(() -> {
            AppUser winner = new AppUser();
            winner.oidcSubject = "sub-race";
            winner.displayName = "Winner";
            winner.email = "winner@example.com";
            repository.persist(winner);
        });

        AppUser result = service.tryCreate("sub-race", "Loser", "loser@example.com");

        assertEquals("Winner", result.displayName);
        assertEquals(1, repository.count());
    }
}
