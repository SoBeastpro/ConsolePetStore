package petstore.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StatisticsReport(
        long clientCount,
        long reservationCount,
        long createdCount,
        long confirmedCount,
        long completedCount,
        long cancelledCount,
        BigDecimal completedShare,
        LocalDate earliestCreatedAt,
        LocalDate latestCreatedAt,
        Long reservationsInPeriod
) {
}
