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

    /**
     * Atomically records (insert-or-move) the user's single jump-back-in pointer. A native
     * {@code ON CONFLICT} upsert on purpose: a read-then-write upsert loses the race against
     * {@code uq_last_edited_location_user_id} when the same user creates twice concurrently,
     * rolling back the loser's whole transaction. Last-writer-wins is exactly the pointer's
     * semantic, so the database's atomic upsert is the correct tool (PostgreSQL per ADR-0004).
     */
    public void upsertForUser(UUID userId, UUID specId, String capabilityName) {
        getEntityManager().createNativeQuery("""
                insert into last_edited_location
                    (id, user_id, spec_id, capability_name, created_at, updated_at, version)
                values (gen_random_uuid(), :userId, :specId, :capabilityName, now(), now(), 0)
                on conflict (user_id) do update set
                    spec_id = excluded.spec_id,
                    capability_name = excluded.capability_name,
                    updated_at = now(),
                    version = last_edited_location.version + 1
                """)
                .setParameter("userId", userId)
                .setParameter("specId", specId)
                .setParameter("capabilityName", capabilityName)
                .executeUpdate();
    }
}
