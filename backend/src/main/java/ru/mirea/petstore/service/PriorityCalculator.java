package ru.mirea.petstore.service;

import org.springframework.stereotype.Component;
import ru.mirea.petstore.domain.Priority;
import ru.mirea.petstore.domain.ReservationStatus;

import java.math.BigDecimal;

/**
 * Считает приоритет заявки.
 * Завершённые и отменённые заявки приоритета не имеют.
 * Срочный уход и маленький задаток повышают приоритет.
 */
@Component
public class PriorityCalculator {
    private static final BigDecimal LOW_DEPOSIT = new BigDecimal("5000.00");
    private static final BigDecimal COMFORT_DEPOSIT = new BigDecimal("20000.00");

    public Priority calculate(ReservationStatus status, BigDecimal deposit, boolean needsCare) {
        if (status == ReservationStatus.COMPLETED || status == ReservationStatus.CANCELLED) {
            return Priority.NOT_APPLICABLE;
        }
        if (needsCare && (deposit == null || deposit.compareTo(LOW_DEPOSIT) < 0)) return Priority.CRITICAL;
        if (needsCare || (deposit != null && deposit.compareTo(LOW_DEPOSIT) < 0)) return Priority.HIGH;
        if (deposit == null || deposit.compareTo(COMFORT_DEPOSIT) < 0) return Priority.MEDIUM;
        return Priority.LOW;
    }
}
