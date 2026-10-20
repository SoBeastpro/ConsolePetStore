package petstore.service;

import petstore.model.Reservation;
import petstore.model.ReservationStatus;
import petstore.model.StatisticsReport;
import petstore.repository.EntityRepository;
import petstore.repository.ReservationRepository;
import petstore.model.Client;
import petstore.util.DateRanges;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class StatisticsService {
    private final EntityRepository<Client> clientRepository;
    private final ReservationRepository reservationRepository;

    public StatisticsService(EntityRepository<Client> clientRepository, ReservationRepository reservationRepository) {
        this.clientRepository = clientRepository;
        this.reservationRepository = reservationRepository;
    }

    public StatisticsReport report(LocalDate from, LocalDate to) throws SQLException {
        DateRanges.check(from, to);
        List<Reservation> reservations = reservationRepository.findAll();
        long created = count(reservations, ReservationStatus.CREATED);
        long confirmed = count(reservations, ReservationStatus.CONFIRMED);
        long completed = count(reservations, ReservationStatus.COMPLETED);
        long cancelled = count(reservations, ReservationStatus.CANCELLED);
        LocalDate earliest = reservations.stream()
                .map(Reservation::getCreatedAt)
                .min(Comparator.naturalOrder())
                .orElse(null);
        LocalDate latest = reservations.stream()
                .map(Reservation::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
        Long period = null;
        if (from != null) {
            period = reservations.stream()
                    .filter(item -> !item.getCreatedAt().isBefore(from) && !item.getCreatedAt().isAfter(to))
                    .count();
        }
        BigDecimal share = reservations.isEmpty()
                ? new BigDecimal("0.0")
                : BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(reservations.size()), 1, RoundingMode.HALF_UP);
        return new StatisticsReport(
                clientRepository.findAll().size(),
                reservations.size(),
                created,
                confirmed,
                completed,
                cancelled,
                share,
                earliest,
                latest,
                period
        );
    }

    private long count(List<Reservation> reservations, ReservationStatus status) {
        return reservations.stream().filter(item -> item.getStatus() == status).count();
    }
}
