package dev.apicius.test;

import dev.apicius.repository.AppUserRepository;
import dev.apicius.repository.LastEditedLocationRepository;
import dev.apicius.repository.SpecRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base for {@code @QuarkusTest} classes touching the database: each test starts from a clean slate.
 *
 * <p>Explicit cleanup is needed because Quarkus does not reset the database between tests —
 * {@code drop-and-create} runs once per test-JVM boot, so rows accumulate across tests. And
 * {@code @TestTransaction} can't isolate these tests: the RestAssured calls provision inside the
 * server's own committed transaction (and {@link dev.apicius.service.UserProvisioningService}
 * inserts via {@code requiringNew}), neither of which a test-scoped rollback would undo.
 *
 * <p>Deletion order follows the foreign keys (children before parents) — there is deliberately no
 * {@code ON DELETE CASCADE} in the schema: what happens to a departed user's specs is a product
 * decision a future feature must make, not one test plumbing may preempt.
 */
public abstract class CleanDatabaseTest {

    @Inject
    protected AppUserRepository repository;

    @Inject
    protected SpecRepository specRepository;

    @Inject
    protected LastEditedLocationRepository lastEditedLocationRepository;

    @BeforeEach
    void cleanDatabase() {
        QuarkusTransaction.requiringNew().run(() -> {
            lastEditedLocationRepository.deleteAll();
            specRepository.deleteAll();
            repository.deleteAll();
        });
    }
}
