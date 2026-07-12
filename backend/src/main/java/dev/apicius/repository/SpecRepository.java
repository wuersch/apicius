package dev.apicius.repository;

import dev.apicius.domain.Spec;
import dev.apicius.repository.projection.SpecSummaryProjection;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SpecRepository implements PanacheRepositoryBase<Spec, UUID> {

    /**
     * A JPQL constructor expression selects only the summary columns — {@code body} never
     * appears in the generated SQL and no {@code Spec} entity is hydrated (FEAT-002 AC5).
     */
    public List<SpecSummaryProjection> listSummariesOrderedByTitle() {
        return getEntityManager().createQuery("""
                select new dev.apicius.repository.projection.SpecSummaryProjection(
                    s.id, s.title, s.description, s.apiVersion, s.specVersion,
                    s.resourceCount, s.operationCount, s.updatedAt)
                from Spec s
                order by lower(s.title)
                """, SpecSummaryProjection.class).getResultList();
    }
}
