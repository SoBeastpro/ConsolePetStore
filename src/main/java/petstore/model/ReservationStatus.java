package petstore.model;

import java.util.List;

public enum ReservationStatus {
    CREATED("Создана"),
    CONFIRMED("Подтверждена"),
    COMPLETED("Завершена"),
    CANCELLED("Отменена");

    private final String title;

    ReservationStatus(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }

    public List<ReservationStatus> allowedTargets() {
        return switch (this) {
            case CREATED -> List.of(CREATED, CONFIRMED, CANCELLED);
            case CONFIRMED -> List.of(CONFIRMED, COMPLETED, CANCELLED);
            case COMPLETED -> List.of(COMPLETED);
            case CANCELLED -> List.of(CANCELLED);
        };
    }
}
