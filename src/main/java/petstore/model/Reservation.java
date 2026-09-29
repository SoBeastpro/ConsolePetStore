package petstore.model;

import java.time.LocalDate;

public class Reservation {
    private long id;
    private long clientId;
    private String animalName;
    private ReservationStatus status;
    private LocalDate createdAt;
    private String comment;

    public Reservation(long id, long clientId, String animalName, ReservationStatus status,
                       LocalDate createdAt, String comment) {
        this.id = id;
        this.clientId = clientId;
        this.animalName = animalName;
        this.status = status;
        this.createdAt = createdAt;
        this.comment = comment;
    }

    public Reservation(long clientId, String animalName, ReservationStatus status,
                       LocalDate createdAt, String comment) {
        this(0, clientId, animalName, status, createdAt, comment);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public String getAnimalName() {
        return animalName;
    }

    public void setAnimalName(String animalName) {
        this.animalName = animalName;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
