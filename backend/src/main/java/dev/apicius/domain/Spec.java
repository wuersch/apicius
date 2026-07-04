package dev.apicius.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * An API under design: the authoritative OpenAPI document ({@code body}, ADR-0004) plus the
 * denormalized summary projection the "My APIs" home reads (ADR-0008, FEAT-002). The projection
 * columns are kept in sync at the write chokepoint (create / import / save — FEAT-003/004 and the
 * future editor); list views read only them, never {@code body}.
 */
@Entity
@Table(name = "spec")
public class Spec {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    /** Provenance only — the list is not filtered by owner (FEAT-002). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_spec_owner_id"))
    public AppUser owner;

    /** Projection of {@code info.title}. */
    @Column(nullable = false)
    public String title;

    /** Projection of {@code info.description}. */
    @Column(columnDefinition = "text")
    public String description;

    /** Projection of {@code info.version} — the API's own version, not the OpenAPI version. */
    @Column(name = "api_version", nullable = false)
    public String apiVersion;

    /** App-derived at write time (ADR-0008) — "resource" is an Apicius concept, not a spec field. */
    @Column(name = "resource_count", nullable = false)
    public int resourceCount;

    @Column(name = "operation_count", nullable = false)
    public int operationCount;

    /**
     * The parsed superset model plus the lossless preservation bag (ADR-0004). Written by
     * create/import (FEAT-003/004); the home path must never deserialize it (FEAT-002 AC5).
     * Mapped as {@code String} until the first writer settles the Java representation (ADR-0009).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Version
    public int version;
}
