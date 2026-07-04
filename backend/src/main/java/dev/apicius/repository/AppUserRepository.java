package dev.apicius.repository;

import dev.apicius.domain.AppUser;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AppUserRepository implements PanacheRepositoryBase<AppUser, UUID> {

    public Optional<AppUser> findByOidcSubject(String oidcSubject) {
        return find("oidcSubject", oidcSubject).firstResultOptional();
    }
}
