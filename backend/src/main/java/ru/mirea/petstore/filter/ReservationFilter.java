package ru.mirea.petstore.filter;

import ru.mirea.petstore.domain.Priority;
import ru.mirea.petstore.domain.ReservationStatus;

import java.math.BigDecimal;
import java.util.List;

/** Разобранный фильтр заявок. Значения уже проверены и не подставляются в SQL строкой. */
public record ReservationFilter(
        String freeText,
        String client,
        ReservationStatus status,
        Boolean needsCare,
        Priority priority,
        String tag,
        Boolean archived,
        List<DepositConstraint> deposits) {

    public enum Operator { LT, LTE, EQ, GTE, GT }

    public record DepositConstraint(Operator operator, BigDecimal value) {
    }
}
