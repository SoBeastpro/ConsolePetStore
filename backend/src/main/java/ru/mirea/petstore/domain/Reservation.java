package ru.mirea.petstore.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Заявка на резерв животного. Основная сущность информационной системы. */
@Entity
@Table(name = "reservations")
public class Reservation extends AuditedVersionedEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "animal_name", nullable = false, length = 100)
    private String animalName;

    @Column(length = 500)
    private String comment;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ShelterClient client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReservationStatus status;

    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "needs_care", nullable = false)
    private boolean needsCare;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Priority priority;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archived_by")
    private UserAccount archivedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private UserAccount updatedBy;

    @ElementCollection
    @CollectionTable(name = "reservation_tag", joinColumns = @JoinColumn(name = "reservation_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    protected Reservation() {
    }

    public Reservation(UUID id, String code, String animalName, UserAccount createdBy) {
        this.id = id;
        this.code = code;
        this.animalName = animalName;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getAnimalName() { return animalName; }
    public void setAnimalName(String animalName) { this.animalName = animalName; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDate getReservationDate() { return reservationDate; }
    public void setReservationDate(LocalDate reservationDate) { this.reservationDate = reservationDate; }
    public ShelterClient getClient() { return client; }
    public void setClient(ShelterClient client) { this.client = client; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public boolean isNeedsCare() { return needsCare; }
    public void setNeedsCare(boolean needsCare) { this.needsCare = needsCare; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public UserAccount getArchivedBy() { return archivedBy; }
    public void setArchivedBy(UserAccount archivedBy) { this.archivedBy = archivedBy; }
    public UserAccount getCreatedBy() { return createdBy; }
    public UserAccount getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UserAccount updatedBy) { this.updatedBy = updatedBy; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }
}
