package ru.mirea.petstore.service;

import org.junit.jupiter.api.Test;
import ru.mirea.petstore.domain.Priority;
import ru.mirea.petstore.domain.ReservationStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityCalculatorTest {
    private final PriorityCalculator calculator = new PriorityCalculator();

    @Test
    void urgentCareWithSmallDepositIsCritical() {
        assertThat(calculator.calculate(ReservationStatus.CREATED, new BigDecimal("3000.00"), true))
                .isEqualTo(Priority.CRITICAL);
    }

    @Test
    void comfortableDepositWithoutCareIsLow() {
        assertThat(calculator.calculate(ReservationStatus.CONFIRMED, new BigDecimal("25000.00"), false))
                .isEqualTo(Priority.LOW);
    }

    @Test
    void finishedReservationHasNoPriority() {
        assertThat(calculator.calculate(ReservationStatus.COMPLETED, new BigDecimal("1000.00"), true))
                .isEqualTo(Priority.NOT_APPLICABLE);
        assertThat(calculator.calculate(ReservationStatus.CANCELLED, new BigDecimal("1000.00"), true))
                .isEqualTo(Priority.NOT_APPLICABLE);
    }
}
