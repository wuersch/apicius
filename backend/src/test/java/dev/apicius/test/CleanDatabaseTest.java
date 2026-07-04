package dev.apicius.test;

import dev.apicius.repository.AppUserRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

/** Base for {@code @QuarkusTest} classes touching the database: each test starts from a clean slate. */
public abstract class CleanDatabaseTest {

    @Inject
    protected AppUserRepository repository;

    @BeforeEach
    void cleanDatabase() {
        QuarkusTransaction.requiringNew().run(repository::deleteAll);
    }
}
