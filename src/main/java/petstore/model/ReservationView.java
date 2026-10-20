package petstore.model;

import java.time.LocalDate;

public record ReservationView(
        long id,
        long clientId,
        String clientName,
        String animalName,
        ReservationStatus status,
        LocalDate createdAt,
        String comment
) {
}
