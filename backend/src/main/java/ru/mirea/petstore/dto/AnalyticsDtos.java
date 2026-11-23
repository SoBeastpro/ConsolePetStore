package ru.mirea.petstore.dto;

import java.math.BigDecimal;
import java.util.List;

/** Сводные показатели аналитической панели. */
public final class AnalyticsDtos {
    private AnalyticsDtos() {
    }

    public record NamedCount(String name, long count) {
    }

    public record AnalyticsResponse(
            long total,
            long active,
            long archived,
            long needsCare,
            BigDecimal averageDeposit,
            List<NamedCount> byClient,
            List<NamedCount> byStatus,
            List<NamedCount> byPriority) {
    }
}
