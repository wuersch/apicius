package dev.apicius.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A designer's identity, provisioned from OIDC token claims on the first authenticated
 * request (FEAT-001, ADR-0004, ADR-0005).
 */
@Entity
@Table(name = "app_user", uniqueConstraints = @UniqueConstraint(name = "uq_app_user_oidc_subject", columnNames = "oidc_subject"))
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    /** The IdP's stable subject (token {@code sub} claim) — the identity key across sign-ins. */
    @Column(name = "oidc_subject", nullable = false, updatable = false)
    public String oidcSubject;

    @Column(name = "display_name", nullable = false)
    public String displayName;

    @Column(name = "email")
    public String email;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Version
    public int version;
}
