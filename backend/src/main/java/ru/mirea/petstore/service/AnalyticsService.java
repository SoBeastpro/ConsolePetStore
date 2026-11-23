package ru.mirea.petstore.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petstore.dto.AnalyticsDtos.AnalyticsResponse;
import ru.mirea.petstore.dto.AnalyticsDtos.NamedCount;
import ru.mirea.petstore.repository.ReservationRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AnalyticsService {
    private final ReservationRepository reservations;

    public AnalyticsService(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse summary() {
        BigDecimal average = reservations.averageDepositForActive();
        if (average != null) average = average.setScale(2, RoundingMode.HALF_UP);
        return new AnalyticsResponse(
                reservations.count(),
                reservations.countByArchivedFalse(),
                reservations.countByArchivedTrue(),
                reservations.countByArchivedFalseAndNeedsCareTrue(),
                average,
                map(reservations.countActiveByClient()),
                map(reservations.countActiveByStatus()),
                map(reservations.countActiveByPriority()));
    }

    private static List<NamedCount> map(List<Object[]> rows) {
        return rows.stream().map(row -> new NamedCount(String.valueOf(row[0]), ((Number) row[1]).longValue())).toList();
    }
}
