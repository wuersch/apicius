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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A designer's single most recent editing location — the API and, when recorded, the capability —
 * powering the home's jump-back-in card (ADR-0008, FEAT-002 AC1). One row per user, overwritten by
 * the future editor whenever the designer works somewhere else; nothing writes it yet.
 */
@Entity
@Table(name = "last_edited_location",
        uniqueConstraints = @UniqueConstraint(name = "uq_last_edited_location_user_id", columnNames = "user_id"))
public class LastEditedLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_last_edited_location_user_id"))
    public AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spec_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_last_edited_location_spec_id"))
    public Spec spec;

    /**
     * Plain-language display label of the capability last edited (PRIN-002), denormalized by the
     * writer so the home never opens the document to render it; {@code null} = API-level (AC1).
     */
    @Column(name = "capability_name")
    public String capabilityName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    /** Doubles as "when the designer was last there" — shown as the card's relative age. */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Version
    public int version;
}
