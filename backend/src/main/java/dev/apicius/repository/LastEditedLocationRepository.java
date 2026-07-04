package dev.apicius.repository;

import dev.apicius.domain.LastEditedLocation;
import dev.apicius.repository.projection.LastEditedLocationProjection;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class LastEditedLocationRepository implements PanacheRepositoryBase<LastEditedLocation, UUID> {

    /**
     * A projection join carries the spec facts the jump-back-in card needs, so neither the
     * location nor the spec entity (and its {@code body}) is hydrated (FEAT-002 AC1/AC5).
     */
    public Optional<LastEditedLocationProjection> findByUserId(UUID userId) {
        return getEntityManager().createQuery("""
                select new dev.apicius.repository.projection.LastEditedLocationProjection(
                    s.id, s.title, s.apiVersion, l.capabilityName, l.updatedAt)
                from LastEditedLocation l
                join l.spec s
                where l.user.id = :userId
                """, LastEditedLocationProjection.class)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst();
    }
}
