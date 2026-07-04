package dev.apicius.test;

import dev.apicius.repository.AppUserRepository;
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
 */
public abstract class CleanDatabaseTest {

    @Inject
    protected AppUserRepository repository;

    @BeforeEach
    void cleanDatabase() {
        QuarkusTransaction.requiringNew().run(repository::deleteAll);
    }
}
