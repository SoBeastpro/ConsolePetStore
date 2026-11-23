package ru.mirea.petstore.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/** Заметка сотрудника к заявке на резерв. */
@Entity
@Table(name = "reservation_note")
public class ReservationNote extends AuditedVersionedEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(nullable = false)
    private boolean pinned;

    protected ReservationNote() {
    }

    public ReservationNote(UUID id, Reservation reservation, UserAccount author, String body, boolean pinned) {
        this.id = id;
        this.reservation = reservation;
        this.author = author;
        this.body = body;
        this.pinned = pinned;
    }

    public UUID getId() { return id; }
    public Reservation getReservation() { return reservation; }
    public UserAccount getAuthor() { return author; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
}
