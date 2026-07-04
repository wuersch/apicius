package dev.apicius.service;

import dev.apicius.domain.AppUser;
import dev.apicius.repository.AppUserRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Objects;

@ApplicationScoped
public class UserProvisioningService {

    @Inject
    AppUserRepository repository;

    /**
     * Find-or-create the {@code app_user} for a token subject, refreshing display name and
     * email from the current claims (FEAT-001 AC1/AC2). Called by the provisioning filter
     * on every authenticated request.
     */
    @Transactional
    public AppUser provision(String oidcSubject, String displayName, String email) {
        AppUser user = repository.findByOidcSubject(oidcSubject)
                .orElseGet(() -> tryCreate(oidcSubject, displayName, email));
        // Assign only on change so dirty-checking doesn't UPDATE (and bump updated_at)
        // on every authenticated request when the claims are unchanged.
        if (!Objects.equals(user.displayName, displayName)) {
            user.displayName = displayName;
        }
        if (!Objects.equals(user.email, email)) {
            user.email = email;
        }
        return user;
    }

    /**
     * Insert in a separate transaction so that losing the first-provisioning race
     * (uq_app_user_oidc_subject) doesn't poison the caller's transaction; the loser
     * reuses the winner's row (AC2).
     */
    AppUser tryCreate(String oidcSubject, String displayName, String email) {
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                AppUser user = new AppUser();
                user.oidcSubject = oidcSubject;
                user.displayName = displayName;
                user.email = email;
                repository.persist(user);
            });
        } catch (RuntimeException raced) {
            return repository.findByOidcSubject(oidcSubject).orElseThrow(() -> raced);
        }
        return repository.findByOidcSubject(oidcSubject).orElseThrow();
    }
}
