package petstore.model;

import java.time.LocalDate;

public record ReservationQuery(
        String searchText,
        ReservationStatus status,
        LocalDate from,
        LocalDate to,
        SortField sortField,
        SortDirection sortDirection
) {
    public static ReservationQuery all() {
        return new ReservationQuery(null, null, null, null, null, null);
    }
}
